package org.springblade.workflow.action;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.workflow.config.WfActionProperties;
import org.springblade.workflow.entity.WfFormSnapshot;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.mapper.WfFormSnapshotMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * 节点「前/后附加操作」脚本的实际执行器。
 *
 * <p>按 {@code script} 前缀分派：</p>
 * <ul>
 *   <li>{@code http://...} / {@code https://...} → 发起 GET 请求（需 {@code blade.workflow.action.http-enabled=true}，且受 URL 白名单限制）</li>
 *   <li>{@code sql:...} / {@code dml:...} → 执行 DML（需 {@code blade.workflow.action.sql-enabled=true}）</li>
 *   <li>{@code field:name=value;name2=value2} / {@code set:...} → 写入当前实例最新表单快照</li>
 *   <li>其它 → 仅记录日志（保持扩展点语义）</li>
 * </ul>
 *
 * <p>异常一律向上抛，由 {@link NodeActionExecutor} 按「流程异常处理」策略（continue/stop）处理。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfActionExecutor {

    private static final String PREFIX_HTTP = "http://";
    private static final String PREFIX_HTTPS = "https://";
    private static final String PREFIX_SQL = "sql:";
    private static final String PREFIX_DML = "dml:";
    private static final String PREFIX_FIELD = "field:";
    private static final String PREFIX_SET = "set:";

    private final WfActionProperties properties;
    private final DataSource dataSource;
    private final WfFormSnapshotMapper snapshotMapper;

    /** 执行一个附加操作脚本（前缀分派） */
    public void execute(WfInstance inst, WfProcessNode node, String phase, String script, Long operator) {
        if (script == null || script.isBlank()) {
            return;
        }
        String s = script.trim();
        String lower = s.toLowerCase();
        if (lower.startsWith(PREFIX_HTTP) || lower.startsWith(PREFIX_HTTPS)) {
            doHttp(inst, node, phase, s);
        } else if (lower.startsWith(PREFIX_SQL) || lower.startsWith(PREFIX_DML)) {
            doSql(inst, node, phase, s);
        } else if (lower.startsWith(PREFIX_FIELD) || lower.startsWith(PREFIX_SET)) {
            doField(inst, s);
        } else {
            log.info("[blade-workflow] 附加操作未识别前缀，仅记录. instId={}, nodeKey={}, phase={}, script={}",
                inst == null ? null : inst.getId(), node == null ? null : node.getNodeKey(), phase, s);
        }
    }

    /** HTTP 调用（GET） */
    private void doHttp(WfInstance inst, WfProcessNode node, String phase, String url) {
        if (!properties.isHttpEnabled()) {
            log.warn("[blade-workflow] 附加操作 HTTP 未开启(blade.workflow.action.http-enabled=false)，跳过. url={}", url);
            return;
        }
        if (!allowedUrl(url)) {
            throw new ServiceException("附加操作 URL 不在白名单：" + url);
        }
        try {
            Duration timeout = Duration.ofMillis(Math.max(500, properties.getHttpTimeoutMs()));
            HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(timeout).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[blade-workflow] 附加操作 HTTP 完成. instId={}, nodeKey={}, phase={}, status={}, url={}",
                inst == null ? null : inst.getId(), node == null ? null : node.getNodeKey(),
                phase, response.statusCode(), url);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("附加操作 HTTP 被中断：" + e.getMessage());
        } catch (Exception e) {
            throw new ServiceException("附加操作 HTTP 调用失败：" + e.getMessage());
        }
    }

    /** DML 执行 */
    private void doSql(WfInstance inst, WfProcessNode node, String phase, String script) {
        if (!properties.isSqlEnabled()) {
            log.warn("[blade-workflow] 附加操作 SQL 未开启(blade.workflow.action.sql-enabled=false)，跳过. script={}", script);
            return;
        }
        int idx = script.indexOf(':');
        String sql = (idx < 0 ? "" : script.substring(idx + 1)).trim();
        if (sql.isEmpty()) {
            return;
        }
        int affected = new JdbcTemplate(dataSource).update(sql);
        log.info("[blade-workflow] 附加操作 SQL 完成. instId={}, nodeKey={}, phase={}, affected={}",
            inst == null ? null : inst.getId(), node == null ? null : node.getNodeKey(), phase, affected);
    }

    /** 写表单快照（field:name=value;name2=value2） */
    private void doField(WfInstance inst, String script) {
        if (inst == null || inst.getId() == null) {
            return;
        }
        WfFormSnapshot snap = snapshotMapper.selectOne(Wrappers.<WfFormSnapshot>lambdaQuery()
            .eq(WfFormSnapshot::getInstId, inst.getId())
            .orderByDesc(WfFormSnapshot::getId)
            .last("LIMIT 1"));
        if (snap == null) {
            log.warn("[blade-workflow] 附加操作字段赋值：实例无表单快照，跳过. instId={}", inst.getId());
            return;
        }
        int idx = script.indexOf(':');
        String body = (idx < 0 ? "" : script.substring(idx + 1));
        Map<String, Object> data = parseData(snap.getDataJson());
        for (String pair : body.split(";")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String name = pair.substring(0, eq).trim();
            String value = pair.substring(eq + 1).trim();
            if (!name.isEmpty()) {
                data.put(name, value);
            }
        }
        snap.setDataJson(JsonUtil.toJson(data));
        snapshotMapper.updateById(snap);
        log.info("[blade-workflow] 附加操作字段赋值完成. instId={}, fields={}", inst.getId(), data.keySet());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseData(String json) {
        if (json == null || json.isBlank()) {
            return new java.util.HashMap<>(8);
        }
        try {
            Map<String, Object> data = JsonUtil.parse(json, Map.class);
            return data == null ? new java.util.HashMap<>(8) : data;
        } catch (Exception e) {
            return new java.util.HashMap<>(8);
        }
    }

    private boolean allowedUrl(String url) {
        java.util.List<String> whitelist = properties.getUrlWhitelist();
        if (whitelist == null || whitelist.isEmpty()) {
            return true;
        }
        for (String prefix : whitelist) {
            if (prefix != null && !prefix.isBlank() && url.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
    }

}
