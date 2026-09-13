package org.springblade.workflow.action;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.SpringUtil;
import org.springblade.workflow.config.WfActionProperties;
import org.springblade.workflow.entity.WfCustomAction;
import org.springblade.workflow.entity.WfFormSnapshot;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.mapper.WfFormSnapshotMapper;
import org.springblade.workflow.service.IWfCustomActionService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点「前/后附加操作」脚本的实际执行器。
 *
 * <p>按 {@code script} 前缀分派：</p>
 * <ul>
 *   <li>{@code http://...} / {@code https://...} → 发起 GET 请求（需 {@code blade.workflow.action.http-enabled=true}，且受 URL 白名单限制）</li>
 *   <li>{@code sql:...} / {@code dml:...} → 执行 DML（需 {@code blade.workflow.action.sql-enabled=true}）</li>
 *   <li>{@code field:name=value;name2=value2} / {@code set:...} → 写入当前实例最新表单快照</li>
 *   <li>{@code action:<接口动作标识>} 或 {@code action:<标识>|{"参数名":"参数值"}} → 调用**已注册的自定义接口动作**（Java 类）</li>
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
    /** 自定义接口动作（对齐 E9「注册自定义接口」）：action:&lt;接口动作标识&gt;[|{参数}] */
    private static final String PREFIX_ACTION = "action:";

    private final WfActionProperties properties;
    private final DataSource dataSource;
    private final WfFormSnapshotMapper snapshotMapper;
    /** 自定义接口动作注册表（action: 命令按标识取注册记录） */
    private final IWfCustomActionService customActionService;

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
        } else if (lower.startsWith(PREFIX_ACTION)) {
            doCustomAction(inst, node, phase, s, operator);
        } else {
            log.info("[blade-workflow] 附加操作未识别前缀，仅记录. instId={}, nodeKey={}, phase={}, script={}",
                inst == null ? null : inst.getId(), node == null ? null : node.getNodeKey(), phase, s);
        }
    }

    /**
     * 自定义接口动作（对齐 ecology E9「注册自定义接口」）。
     *
     * <p>命令形如 {@code action:<接口动作标识>} 或 {@code action:<标识>|{"参数名":"参数值"}}：
     * 按标识取注册记录 → 反射实例化 {@code 接口动作类文件} → 调用
     * {@link IWfCustomAction#execute(WfActionRequest)}。</p>
     */
    private void doCustomAction(WfInstance inst, WfProcessNode node, String phase, String script, Long operator) {
        String body = script.substring(PREFIX_ACTION.length()).trim();
        String actionKey = body;
        String overrideJson = null;
        int bar = body.indexOf('|');
        if (bar >= 0) {
            actionKey = body.substring(0, bar).trim();
            overrideJson = body.substring(bar + 1).trim();
        }
        if (actionKey.isEmpty()) {
            throw new ServiceException("自定义接口动作标识为空");
        }
        WfCustomAction def = customActionService.getByKey(actionKey);
        if (def == null) {
            throw new ServiceException("未注册的自定义接口动作：" + actionKey);
        }

        IWfCustomAction bean;
        try {
            Class<?> clazz = Class.forName(def.getClassName());
            // 优先取 Spring 容器里的实例：动作类标 @Component 后即可 @Autowired 注入依赖；
            // 容器里没有（如未加注解）则按无参构造新建——对齐 E9「类文件 + 无参构造」的约定。
            Object obj = null;
            try {
                obj = SpringUtil.getBean(clazz);
            } catch (Exception ignore) {
                obj = null;
            }
            if (obj == null) {
                obj = clazz.getDeclaredConstructor().newInstance();
            }
            if (!(obj instanceof IWfCustomAction)) {
                throw new ServiceException("接口动作类必须实现 IWfCustomAction：" + def.getClassName());
            }
            bean = (IWfCustomAction) obj;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("自定义接口动作类实例化失败：" + def.getClassName() + " - " + e.getMessage());
        }

        Map<String, Object> formData = latestFormData(inst);
        WfActionRequest req = new WfActionRequest();
        req.setInstId(inst == null ? null : inst.getId());
        req.setDefId(node == null ? null : node.getDefId());
        req.setNodeKey(node == null ? null : node.getNodeKey());
        req.setPhase(phase);
        req.setOperator(operator);
        req.setActionKey(def.getActionKey());
        req.setActionName(def.getActionName());
        req.setFormData(formData);
        req.setParams(mergeParams(def.getParamsJson(), overrideJson, formData));

        try {
            String result = bean.execute(req);
            log.info("[blade-workflow] 自定义接口动作执行完成. instId={}, nodeKey={}, phase={}, actionKey={}, result={}",
                req.getInstId(), req.getNodeKey(), phase, actionKey, result);
        } catch (Exception e) {
            throw new ServiceException("自定义接口动作执行失败：" + actionKey + " - " + e.getMessage());
        }
    }

    /**
     * 合并参数：注册记录的「参数设置」为默认值，节点上的覆盖值优先。
     *
     * <p>「是否数据源=是」的参数，其参数值按**表单字段名**在运行时从表单快照取值。</p>
     */
    private Map<String, Object> mergeParams(String defParamsJson, String overrideJson, Map<String, Object> formData) {
        Map<String, Object> out = new LinkedHashMap<>();

        // 节点上的覆盖值：{"参数名":"参数值"}
        Map<String, Object> override = new LinkedHashMap<>();
        if (overrideJson != null && !overrideJson.isBlank()) {
            try {
                Map<String, Object> m = JsonUtil.parse(overrideJson, Map.class);
                if (m != null) {
                    override.putAll(m);
                }
            } catch (Exception e) {
                log.warn("[blade-workflow] 自定义接口动作覆盖参数解析失败，忽略. json={}", overrideJson, e);
            }
        }

        // 注册记录里的参数定义：[{name,value,isDataSource}]
        List<Map<String, Object>> defs = new ArrayList<>();
        if (defParamsJson != null && !defParamsJson.isBlank()) {
            try {
                List<Map<String, Object>> list = JsonUtil.parse(defParamsJson, List.class);
                if (list != null) {
                    defs.addAll(list);
                }
            } catch (Exception e) {
                log.warn("[blade-workflow] 自定义接口动作参数定义解析失败，按空处理. json={}", defParamsJson, e);
            }
        }

        for (Map<String, Object> d : defs) {
            if (d == null || d.get("name") == null || String.valueOf(d.get("name")).isBlank()) {
                continue;
            }
            String name = String.valueOf(d.get("name"));
            Object raw = d.get("isDataSource");
            boolean dataSource = Boolean.TRUE.equals(raw) || "true".equalsIgnoreCase(String.valueOf(raw));
            Object value = override.containsKey(name) ? override.get(name) : d.get("value");
            if (dataSource) {
                String field = value == null ? "" : String.valueOf(value);
                value = field.isBlank() ? null : formData.get(field);
            }
            out.put(name, value);
        }
        // 注册记录未声明、但节点上单独配了的参数一并透传
        override.forEach(out::putIfAbsent);
        return out;
    }

    /** 当前实例最新表单快照的数据（无快照返回空 map） */
    private Map<String, Object> latestFormData(WfInstance inst) {
        if (inst == null || inst.getId() == null) {
            return new LinkedHashMap<>();
        }
        WfFormSnapshot snap = snapshotMapper.selectOne(Wrappers.<WfFormSnapshot>lambdaQuery()
            .eq(WfFormSnapshot::getInstId, inst.getId())
            .orderByDesc(WfFormSnapshot::getId)
            .last("LIMIT 1"));
        if (snap == null || snap.getDataJson() == null) {
            return new LinkedHashMap<>();
        }
        return parseData(snap.getDataJson());
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
        List<String> whitelist = properties.getUrlWhitelist();
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
