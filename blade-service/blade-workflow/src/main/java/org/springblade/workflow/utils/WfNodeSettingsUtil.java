package org.springblade.workflow.utils;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.workflow.entity.WfProcessNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 节点扩展属性（{@code wf_process_node.ext_json}）的读取工具。
 *
 * <p>前端把「操作菜单 / 签字意见 / 标题显示 / 子流程 / 表单日志范围 / 流程异常处理 /
 * 指定流转 / 超时设置」等统一写进 {@code ext_json.settings}，后端此前从不解析（只存不用）。
 * 本工具类把 settings 解析成结构化读取入口，供流转运行时消费。</p>
 *
 * <p>ext_json 结构（与前端 buildExtJson 保持契约）：</p>
 * <pre>{@code
 * {
 *   "timeoutHours": 0,   // 兼容字段：settings.timeout.hours
 *   "remind": 0,         // 兼容字段：settings.timeout.remind ? 1 : 0
 *   "sign": 0,
 *   "settings": {
 *     "operateMenu":     { "menus": ["submit","reject"] },
 *     "preOperate":      { "script": "..." },
 *     "postOperate":     { "script": "..." },
 *     "signOpinion":     { "required": true, "template": "同意" },
 *     "titleDisplay":    { "template": "{创建人}的{表单名称}" },
 *     "subflow":         { "flowKey": "...", "trigger": "afterSubmit" },
 *     "formLogScope":    { "nodeKeys": ["UserTask_1"] },   // 旧值 { "scope": "all" } 仍兼容
 *     "exceptionHandle": { "mode": "continue" },
 *     "appointFlow":     { "mode": 1, "target": "UserTask_2" },
 *     "timeout":         { "hours": 24, "remind": true, "autoApprove": false }
 *   }
 * }
 * }</pre>
 *
 * <p>设计原则：<b>读不到就返回「未配置/不限制」的默认值，绝不抛异常阻塞流转主链路。</b></p>
 */
@Slf4j
public final class WfNodeSettingsUtil {

    private WfNodeSettingsUtil() {
    }

    // ---------------- 基础读取 ----------------

    /** 解析 extJson → 顶层对象；坏数据返回空 map */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseExt(String extJson) {
        if (extJson == null || extJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> map = JsonUtil.parse(extJson, Map.class);
            return map == null ? Collections.emptyMap() : map;
        } catch (Exception e) {
            log.warn("[blade-workflow] ext_json 解析失败，按未配置处理: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /** 取 settings 整体 */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> settings(String extJson) {
        Object s = parseExt(extJson).get("settings");
        return s instanceof Map ? (Map<String, Object>) s : Collections.emptyMap();
    }

    /** 取某个设置项（如 operateMenu） */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> setting(String extJson, String key) {
        Object v = settings(extJson).get(key);
        return v instanceof Map ? (Map<String, Object>) v : Collections.emptyMap();
    }

    /** 取某个设置项下的字符串字段 */
    public static String str(String extJson, String key, String field) {
        Object v = setting(extJson, key).get(field);
        return v == null ? null : String.valueOf(v);
    }

    /** 取某个设置项下的整型字段（缺省 def） */
    public static int intValue(String extJson, String key, String field, int def) {
        Object v = setting(extJson, key).get(field);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v instanceof CharSequence && ((CharSequence) v).length() > 0) {
            try {
                return Integer.parseInt(v.toString());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    /** 取某个设置项下的布尔字段（缺省 false） */
    public static boolean bool(String extJson, String key, String field) {
        Object v = setting(extJson, key).get(field);
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue() != 0;
        }
        return "true".equalsIgnoreCase(String.valueOf(v));
    }

    /** 取某个设置项下的字符串数组（缺省空列表） */
    @SuppressWarnings("unchecked")
    public static List<String> list(String extJson, String key, String field) {
        Object v = setting(extJson, key).get(field);
        if (v instanceof List) {
            List<String> out = new ArrayList<>();
            for (Object o : (List<Object>) v) {
                if (o != null) {
                    out.add(String.valueOf(o));
                }
            }
            return out;
        }
        return Collections.emptyList();
    }

    // ---------------- 语义化读取（供运行时消费） ----------------

    /**
     * 操作菜单：该节点允许的操作集合（submit/reject/forward/sign/opinion/attach/print/urge）。
     *
     * @return 未配置时返回 {@code null}，表示「不限制（按系统默认全部可用）」；配置过则返回具体集合（可能为空=全部禁用）
     */
    public static List<String> operateMenus(WfProcessNode node) {
        if (node == null) {
            return null;
        }
        Map<String, Object> m = setting(node.getExtJson(), "operateMenu");
        if (!m.containsKey("menus")) {
            return null;
        }
        return list(node.getExtJson(), "operateMenu", "menus");
    }

    /** 是否允许某个操作（未配置菜单时不限制） */
    public static boolean allowOperate(WfProcessNode node, String menu) {
        List<String> menus = operateMenus(node);
        return menus == null || menus.contains(menu);
    }

    /** 签字意见是否必填 */
    public static boolean opinionRequired(WfProcessNode node) {
        return node != null && bool(node.getExtJson(), "signOpinion", "required");
    }

    /** 默认意见模板（可为 null） */
    public static String opinionTemplate(WfProcessNode node) {
        return node == null ? null : str(node.getExtJson(), "signOpinion", "template");
    }

    /** 标题模板（可为 null） */
    public static String titleTemplate(WfProcessNode node) {
        return node == null ? null : str(node.getExtJson(), "titleDisplay", "template");
    }

    /** 节点前附加操作脚本（可为 null） */
    public static String preOperateScript(WfProcessNode node) {
        return node == null ? null : str(node.getExtJson(), "preOperate", "script");
    }

    /** 节点后附加操作脚本（可为 null） */
    public static String postOperateScript(WfProcessNode node) {
        return node == null ? null : str(node.getExtJson(), "postOperate", "script");
    }

    /** 子流程 Key（可为 null） */
    public static String subflowKey(WfProcessNode node) {
        return node == null ? null : str(node.getExtJson(), "subflow", "flowKey");
    }

    /** 子流程触发时机：afterSubmit / afterArchive（缺省 afterSubmit） */
    public static String subflowTrigger(WfProcessNode node) {
        String t = node == null ? null : str(node.getExtJson(), "subflow", "trigger");
        return t == null || t.isBlank() ? "afterSubmit" : t;
    }

    /** 流程异常处理：continue / stop（缺省 continue，保证不改变既有行为） */
    public static String exceptionMode(WfProcessNode node) {
        String m = node == null ? null : str(node.getExtJson(), "exceptionHandle", "mode");
        return m == null || m.isBlank() ? "continue" : m;
    }

    /** 指定流转模式：1=指定节点和操作者，2=仅指定节点；0 表示未启用 */
    public static int appointFlowMode(WfProcessNode node) {
        return node == null ? 0 : intValue(node.getExtJson(), "appointFlow", "mode", 0);
    }

    /** 指定流转的目标节点 Key（可为 null） */
    public static String appointFlowTarget(WfProcessNode node) {
        return node == null ? null : str(node.getExtJson(), "appointFlow", "target");
    }

    /**
     * 表单日志可见节点集合。
     *
     * <p>新值 {@code {nodeKeys:[...]}} 对齐 ecology viewnodeids；
     * 旧值 {@code {scope:'all'|'node'|'self'}} 做降级兼容：
     * node → 仅当前节点，all/self/其它 → 不限制（返回 null）。</p>
     *
     * @return null 表示不限制；否则为可见节点 Key 集合
     */
    public static List<String> formLogVisibleNodeKeys(WfProcessNode node) {
        if (node == null) {
            return null;
        }
        List<String> keys = list(node.getExtJson(), "formLogScope", "nodeKeys");
        if (!keys.isEmpty()) {
            return keys;
        }
        String scope = str(node.getExtJson(), "formLogScope", "scope");
        if ("node".equals(scope)) {
            return Collections.singletonList(node.getNodeKey());
        }
        return null;
    }

    /** 超时小时数（0 = 未配置） */
    public static int timeoutHours(WfProcessNode node) {
        if (node == null) {
            return 0;
        }
        int hours = intValue(node.getExtJson(), "timeout", "hours", 0);
        // 兼容顶层字段（历史数据可能只写了 timeoutHours）
        return hours > 0 ? hours : parseInt(parseExt(node.getExtJson()).get("timeoutHours"), 0);
    }

    /** 到期提醒（settings.timeout.remind 或顶层 remind） */
    public static boolean timeoutRemind(WfProcessNode node) {
        if (node == null) {
            return false;
        }
        return bool(node.getExtJson(), "timeout", "remind")
            || parseInt(parseExt(node.getExtJson()).get("remind"), 0) == 1;
    }

    /** 超时自动通过 */
    public static boolean timeoutAutoApprove(WfProcessNode node) {
        return node != null && bool(node.getExtJson(), "timeout", "autoApprove");
    }

    /** 启用签章（顶层 sign） */
    public static boolean signEnabled(WfProcessNode node) {
        return node != null && parseInt(parseExt(node.getExtJson()).get("sign"), 0) == 1;
    }

    /**
     * 二次认证：处理该节点前是否要求重新校验密码（对齐 E9 {@code hasSecondAuth}）。
     *
     * @return true=需要二次认证
     */
    public static boolean secondAuth(WfProcessNode node) {
        return node != null && bool(node.getExtJson(), "secondAuth", "required");
    }

    /**
     * 节点字段校验规则（{@code settings.fieldCheck.script} 的多行文本，逐行一条）。
     *
     * <p>规则格式：{@code 字段名:required} / {@code 字段名:regex=正则} /
     * {@code 字段名:min=数字} / {@code 字段名:max=数字}；空行与 {@code #} 开头行忽略。</p>
     *
     * @return 规则列表（未配置时为空列表）
     */
    public static List<String> fieldCheckRules(WfProcessNode node) {
        if (node == null) {
            return Collections.emptyList();
        }
        String script = str(node.getExtJson(), "fieldCheck", "script");
        if (script == null || script.isBlank()) {
            return Collections.emptyList();
        }
        List<String> rules = new ArrayList<>();
        for (String line : script.split("\\r?\\n")) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("#")) {
                rules.add(t);
            }
        }
        return rules;
    }

    private static int parseInt(Object v, int def) {
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v instanceof CharSequence && ((CharSequence) v).length() > 0) {
            try {
                return Integer.parseInt(v.toString());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }
}
