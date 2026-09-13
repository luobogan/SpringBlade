package org.springblade.workflow.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.cloud.header.BladeHttpHeadersContextHolder;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.core.tool.utils.WebUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

/**
 * Feign 鉴权头透传配置。
 *
 * <p><b>为什么需要它</b>：框架自带的
 * {@code org.springblade.core.cloud.header.BladeFeignRequestHeaderInterceptor}
 * 只读取 {@link BladeHttpHeadersContextHolder} 这个 ThreadLocal，而该 ThreadLocal 在**同步请求**
 * 链路上没有任何地方写入（全项目仅异步 {@code BladeHttpHeadersCallable} 会 set），
 * 导致本服务发出的跨服务调用**不带 Blade-Auth**；下游带 {@code @PreAuth} 的接口会直接以
 * {@code SecureException: 请求未授权} 拒绝（例如 formmode 的
 * {@code DELETE /form-layout/node/{formId}}、{@code GET /form-layout/{formId}}）。</p>
 *
 * <p><b>做法</b>：注册一个 {@link RequestInterceptor}——优先用 ThreadLocal（异步链路已写入），
 * 为空则直接从当前 {@link HttpServletRequest} 透传鉴权头。框架的同名 Bean 标注了
 * {@code @ConditionalOnMissingBean}，因此本 Bean 会取代它。</p>
 */
@Slf4j
@Configuration
public class FeignHeaderRelayConfig {

    /**
     * 需要透传的请求头。
     * 注意：Servlet 的 getHeader 大小写不敏感，故同一语义的头只写一个名字，避免重复透传。
     */
    private static final String[] RELAY_HEADERS = {
        "Blade-Auth", "Authorization", "X-Real-IP", "x-forwarded-for"
    };

    @Bean
    public RequestInterceptor bladeAuthRelayInterceptor() {
        return template -> {
            // ① 异步链路：框架已把请求头写入 ThreadLocal
            HttpHeaders cached = BladeHttpHeadersContextHolder.get();
            if (cached != null && !cached.isEmpty()) {
                cached.forEach((key, values) -> values.forEach(v -> template.header(key, v)));
                return;
            }
            // ② 同步链路：直接从当前请求透传
            HttpServletRequest request = WebUtil.getRequest();
            if (request == null) {
                return;
            }
            for (String name : RELAY_HEADERS) {
                String value = request.getHeader(name);
                if (StringUtil.isNotBlank(value)) {
                    template.header(name, value);
                }
            }
        };
    }
}
