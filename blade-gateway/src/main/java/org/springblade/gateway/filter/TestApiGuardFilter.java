package org.springblade.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流程测试接口（{@code /blade-workflow/test/**}）的网关侧访问守卫。
 *
 * <p>背景（方案 §5.1「网络防护」/ §8 P3）：测试域接口能发起测试实例、驱动流程、清理测试数据，
 * 属于「管理面能力」。它虽然要求工作流角色（{@code HAS_ROLE_WORKFLOW}），但仍应能被限制在
 * <b>内网/办公网段</b>内访问，并对高频调用做限流，避免被当作批量驱动引擎的入口。</p>
 *
 * <p><b>默认不生效</b>：两项都不配置（空/0）时直接放行 —— 保证本过滤器上线后
 * <b>不改变任何现有行为</b>，由运维按需开启。</p>
 *
 * <pre>
 * blade:
 *   workflow:
 *     test-ip-whitelist: "192.168.1.*,127.0.0.1"   # 空 = 不限制来源IP；支持 * 通配
 *     test-rate-limit-per-minute: 60               # 0 = 不限流；按来源IP统计
 * </pre>
 *
 * <p>执行顺序取 {@code -1100}：<b>早于</b> {@link RequestFilter}（-1000），因此在路径被改写前
 * 就能看到带服务前缀的原始路径（{@code /blade-workflow/test/step}），判定更准确。</p>
 */
@Component
public class TestApiGuardFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TestApiGuardFilter.class);

    /** 测试接口的路径特征（含网关前缀，避免把其它服务的 /xxx/test/ 误伤） */
    private static final String TEST_API_MARK = "/blade-workflow/test/";

    /** IP 段白名单；空 = 不限制 */
    @Value("${blade.workflow.test-ip-whitelist:}")
    private String ipWhitelist;

    /** 每分钟最大请求数（按来源IP）；0 = 不限流 */
    @Value("${blade.workflow.test-rate-limit-per-minute:0}")
    private int rateLimitPerMinute;

    /** 限流计数：ip -> [当前分钟序号, 该分钟计数] */
    private final Map<String, int[]> counters = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        if (path == null || !path.contains(TEST_API_MARK)) {
            return chain.filter(exchange);
        }
        String ip = clientIp(exchange);

        // ① IP 段白名单：只允许内网/办公网访问测试接口
        if (StringUtils.hasText(ipWhitelist) && !matchAny(ip, ipWhitelist)) {
            log.warn("[blade-gateway] 拒绝测试接口访问（来源不在白名单）. ip={}, path={}", ip, path);
            return reject(exchange, HttpStatus.FORBIDDEN,
                "流程测试接口仅允许内网访问，请从办公网访问或联系管理员");
        }

        // ② 频次限制：测试接口会真实驱动引擎，限流避免被当作批量入口
        if (rateLimitPerMinute > 0 && !tryAcquire(ip)) {
            log.warn("[blade-gateway] 测试接口访问过于频繁被限流. ip={}, limit={}/min, path={}",
                ip, rateLimitPerMinute, path);
            return reject(exchange, HttpStatus.TOO_MANY_REQUESTS,
                "流程测试接口访问过于频繁，请稍后再试");
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 早于 RequestFilter(-1000)：此时路径尚未被改写，可见 /blade-workflow/... 前缀
        return -1100;
    }

    /** 取真实来源IP：优先 X-Forwarded-For 首个地址（经 nginx/前置代理时），否则取连接地址 */
    private String clientIp(ServerWebExchange exchange) {
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        InetSocketAddress addr = exchange.getRequest().getRemoteAddress();
        return (addr == null || addr.getAddress() == null) ? "" : addr.getAddress().getHostAddress();
    }

    /** 白名单匹配：逗号分隔，支持 {@code *} 通配（如 {@code 192.168.1.*}） */
    private boolean matchAny(String ip, String patterns) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        for (String p : patterns.split(",")) {
            String pattern = p.trim();
            if (pattern.isEmpty()) {
                continue;
            }
            if (pattern.indexOf('*') < 0) {
                if (pattern.equals(ip)) {
                    return true;
                }
            } else if (ip.startsWith(pattern.substring(0, pattern.indexOf('*')))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 滑动窗口（分钟粒度）计数。
     *
     * <p>用「当前分钟序号 + 计数」两元组，跨分钟自动重置；不引入 Redis，
     * 因为限流目的是「挡住异常高频」而非精确配额，网关多实例下按实例计数已足够。</p>
     */
    private boolean tryAcquire(String ip) {
        long minute = System.currentTimeMillis() / 60_000L;
        if (counters.size() > 10_000) {
            // 兜底防内存膨胀：条目过多时清掉上一分钟的记录
            counters.entrySet().removeIf(e -> e.getValue()[0] != (int) minute);
        }
        int[] slot = counters.compute(ip, (k, v) -> {
            if (v == null || v[0] != (int) minute) {
                return new int[]{(int) minute, 1};
            }
            v[1]++;
            return v;
        });
        return slot[1] <= rateLimitPerMinute;
    }

    /** 直接写回错误响应（网关层不经过业务服务的统一响应包装，这里手动构造 R 结构） */
    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String msg) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":" + status.value() + ",\"success\":false,\"msg\":\"" + msg + "\",\"data\":null}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
            .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
