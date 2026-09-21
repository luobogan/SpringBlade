package org.springblade.workflow.utils;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.vo.OpinionDisplayVO;
import org.springblade.workflow.vo.PrintSetVO;

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

    /** 取某个设置项下的对象数组（缺省空列表），用于多目标等结构化明细 */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> listMaps(String extJson, String key, String field) {
        Object v = setting(extJson, key).get(field);
        if (v instanceof List) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object o : (List<Object>) v) {
                if (o instanceof Map) {
                    out.add((Map<String, Object>) o);
                }
            }
            return out;
        }
        return Collections.emptyList();
    }

    // ---------------- 表单内容（settings.formContent）读取 ----------------
    // 「设置表单内容」弹框把打印内容 / 意见显示等统一写进 settings.formContent，
    // 故这些读取必须从 formContent 取，而非顶层 settings（否则读不到）。

    /** 取 settings.formContent 整体 */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> formContent(WfProcessNode node) {
        Object v = settings(node.getExtJson()).get("formContent");
        return v instanceof Map ? (Map<String, Object>) v : Collections.emptyMap();
    }

    /** 取 formContent 下某设置项（如 printSet / opinionDisplay） */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> fcSetting(WfProcessNode node, String key) {
        Object v = formContent(node).get(key);
        return v instanceof Map ? (Map<String, Object>) v : Collections.emptyMap();
    }

    private static int fcInt(WfProcessNode node, String key, String field, int def) {
        Object v = fcSetting(node, key).get(field);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    private static boolean fcBool(WfProcessNode node, String key, String field) {
        Object v = fcSetting(node, key).get(field);
        return v instanceof Boolean && (Boolean) v;
    }

    @SuppressWarnings("unchecked")
    private static List<String> fcList(WfProcessNode node, String key, String field) {
        Object v = fcSetting(node, key).get(field);
        if (v instanceof List) {
            List<String> out = new ArrayList<>();
            for (Object o : (List<Object>) v) {
                if (o != null) out.add(String.valueOf(o));
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

    /**
     * 签字意见必填模式（对齐 ecology「意见必填」三态）：
     * never=从不要求（缺省，不改变既有行为）/ all=所有操作均必填 / byOperation=仅指定操作必填。
     * 兼容旧字段 {@code signOpinion.required=true} → 视为 all。
     */
    public static String signOpinionMustInput(WfProcessNode node) {
        String v = node == null ? null : str(node.getExtJson(), "signOpinion", "mustInput");
        if ("all".equals(v) || "byOperation".equals(v)) {
            return v;
        }
        if (node != null && bool(node.getExtJson(), "signOpinion", "required")) {
            return "all";
        }
        return "never";
    }

    /**
     * 仅 byOperation 模式生效：哪些操作需要必填意见。
     * 缺省为 ecology 默认语义：退回 + 征询回复。
     */
    public static List<String> signOpinionMustInputOperations(WfProcessNode node) {
        if (node == null) {
            return List.of("reject", "consultReply");
        }
        List<String> ops = list(node.getExtJson(), "signOpinion", "mustInputOperations");
        return (ops == null || ops.isEmpty()) ? List.of("reject", "consultReply") : ops;
    }

    /** 意见输入框是否不显示（hideInput） */
    public static boolean signOpinionHideInput(WfProcessNode node) {
        return node != null && bool(node.getExtJson(), "signOpinion", "hideInput");
    }

    /** 意见区域是否整体不显示（hideArea）：输入框与历史意见块一并隐藏 */
    public static boolean signOpinionHideArea(WfProcessNode node) {
        return node != null && bool(node.getExtJson(), "signOpinion", "hideArea");
    }

    /**
     * 意见显示范围模式（三态）：all=全部可见（缺省）/ none=全不可见 / list=仅 list 内节点可见。
     */
    public static String signOpinionViewMode(WfProcessNode node) {
        String m = node == null ? null : str(node.getExtJson(), "signOpinion", "viewNodeMode");
        return m == null || m.isBlank() ? "all" : m;
    }

    /** 意见显示范围：list 模式下的节点Key集合（缺省空列表） */
    public static List<String> signOpinionViewNodeKeys(WfProcessNode node) {
        if (node == null) {
            return List.of();
        }
        List<String> v = list(node.getExtJson(), "signOpinion", "viewNodeKeys");
        return v == null ? List.of() : v;
    }

    /** 同节点办理人互不可见彼此意见（notSeeEachOther） */
    public static boolean signOpinionNotSeeEachOther(WfProcessNode node) {
        return node != null && bool(node.getExtJson(), "signOpinion", "notSeeEachOther");
    }

    /** 意见反馈（feedback）：审批意见回写/反馈给上游节点 */
    public static boolean signOpinionFeedback(WfProcessNode node) {
        return node != null && bool(node.getExtJson(), "signOpinion", "feedback");
    }

    /** 意见为空时不反馈（nullNotFeedback，需配合 feedback） */
    public static boolean signOpinionNullNotFeedback(WfProcessNode node) {
        return node != null && bool(node.getExtJson(), "signOpinion", "nullNotFeedback");
    }

    /**
     * 签字意见是否必填（兼容旧口径，等价于 mustInput=all）。
     * 新增「byOperation」场景由 {@link #opinionRequiredByOp} 按操作类型判定。
     */
    public static boolean opinionRequired(WfProcessNode node) {
        return "all".equals(signOpinionMustInput(node));
    }

    /**
     * 按操作类型判定意见是否必填（供提交/退回/征询回复等入口使用）。
     *
     * @param op 操作 code（如 submit / reject / consultReply，对齐 MENUS_OPTIONS）
     */
    public static boolean opinionRequiredByOp(WfProcessNode node, String op) {
        String mode = signOpinionMustInput(node);
        if ("all".equals(mode)) {
            return true;
        }
        if ("byOperation".equals(mode) && op != null) {
            return signOpinionMustInputOperations(node).contains(op);
        }
        return false;
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

    /**
     * 节点前附加操作「退回时触发」脚本（可为 null）。
     *
     * <p>由前端从 items 派生：仅包含勾选「退回时触发」的启用条目。
     * 退回场景只执行这些命令；未勾选的条目仅正常提交时执行。</p>
     */
    public static String preOperateRejectScript(WfProcessNode node) {
        return node == null ? null : str(node.getExtJson(), "preOperate", "scriptOnReject");
    }

    /** 节点后附加操作「退回时触发」脚本（可为 null） */
    public static String postOperateRejectScript(WfProcessNode node) {
        return node == null ? null : str(node.getExtJson(), "postOperate", "scriptOnReject");
    }

    // ---------------- 退回（reject）配置：对齐 ecology isselectrejectnode / rejectableNodes / isrejectremind ----------------

    /**
     * 退回方式：1=直接退回（退到默认/上一节点，不弹窗选）；2=选择退回节点（弹窗选节点）。
     * 缺省 1。
     */
    public static int rejectType(WfProcessNode node) {
        return node == null ? 1 : intValue(node.getExtJson(), "reject", "type", 1);
    }

    /** 默认退回节点Key（rejectType=1 时优先用；为空则退上一节点） */
    public static String defaultRejectNodeKey(WfProcessNode node) {
        return node == null ? null : str(node.getExtJson(), "reject", "defaultNodeKey");
    }

    /** 可退回节点白名单（rejectType=2 时限制可选范围；为空=不限制） */
    public static List<String> rejectableNodeKeys(WfProcessNode node) {
        return node == null ? Collections.emptyList() : list(node.getExtJson(), "reject", "nodeKeys");
    }

    /** 退回时是否提醒已走过节点（对应 ecology isrejectremind） */
    public static boolean rejectRemind(WfProcessNode node) {
        return node != null && bool(node.getExtJson(), "reject", "remind");
    }

    /** 退回时是否允许变更退回节点（对应 ecology ischangrejectnode） */
    public static boolean rejectChangeNode(WfProcessNode node) {
        return node != null && bool(node.getExtJson(), "reject", "changeNode");
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

    // ---------------- 子流程高级设置（subflow 子对象）：对齐 ecology issubwfAllEnd / subProcessSummary / issubwfremind / subwffreeforword ----------------

    /** 全部归档才能提交（阻塞主流程归档）：1=是 0=否（缺省 0，不改变既有行为） */
    public static int subflowAllEndBeforeSubmit(WfProcessNode node) {
        return node == null ? 0 : (bool(node.getExtJson(), "subflow", "allEndBeforeSubmit") ? 1 : 0);
    }

    /** 子流程归档后汇总数据到主流程：1=是 0=否（缺省 0） */
    public static int subflowDataSummary(WfProcessNode node) {
        return node == null ? 0 : (bool(node.getExtJson(), "subflow", "dataSummary") ? 1 : 0);
    }

    /** 全部归档后自动流转主流程：1=是 0=否（缺省 0） */
    public static int subflowAutoForward(WfProcessNode node) {
        return node == null ? 0 : (bool(node.getExtJson(), "subflow", "autoForward") ? 1 : 0);
    }

    /** 全部归档后提醒：1=是 0=否（缺省 0） */
    public static int subflowRemindEnabled(WfProcessNode node) {
        return node == null ? 0 : (bool(node.getExtJson(), "subflow", "remindEnabled") ? 1 : 0);
    }

    /** 提醒方式：含 sys=流程提醒 ml=短信 sm=邮件（缺省 null） */
    public static String subflowRemindTypes(WfProcessNode node) {
        if (node == null) {
            return null;
        }
        List<String> v = list(node.getExtJson(), "subflow", "remindTypes");
        return (v == null || v.isEmpty()) ? null : String.join(",", v);
    }

    /** 提醒对象：节点操作者本人 1=是 0=否（缺省 0） */
    public static int subflowRemindBeforeOperator(WfProcessNode node) {
        return node == null ? 0 : (bool(node.getExtJson(), "subflow", "remindBeforeOperator") ? 1 : 0);
    }

    /** 提醒对象：指定人员（人力资源ID逗号分隔，缺省 null） */
    public static String subflowRemindPersons(WfProcessNode node) {
        return node == null ? null : str(node.getExtJson(), "subflow", "remindPersons");
    }

    /** 流程异常处理：continue / stop（缺省 continue，保证不改变既有行为） */
    public static String exceptionMode(WfProcessNode node) {
        String m = node == null ? null : str(node.getExtJson(), "exceptionHandle", "mode");
        return m == null || m.isBlank() ? "continue" : m;
    }

    // ---------------- 流程异常处理（流转兜底）：对齐 ecology useExceptionHandle / exceptionHandleWay / flowToAssignNode ----------------

    /** 兜底方式：未启用 / 不适用 */
    public static final int FALLBACK_NONE = 0;
    /** 兜底方式：自动流转至下一节点（ecology exceptionHandleWay=1） */
    public static final int FALLBACK_NEXT_NODE = 1;
    /** 兜底方式：提交至指定节点（ecology exceptionHandleWay=2） */
    public static final int FALLBACK_ASSIGN_NODE = 2;
    /** 兜底方式：由用户指定操作者（ecology exceptionHandleWay=3） */
    public static final int FALLBACK_USER_ASSIGN = 3;

    /**
     * 「流转异常兜底」的显式开关（{@code settings.exceptionHandle.enabled}）。
     *
     * <p>与 {@link #exceptionMode(WfProcessNode)}（附加操作 / 子流程的失败策略 continue|stop）
     * 共用同一个 {@code exceptionHandle} 对象，但**语义互不干扰**，可同时配置：
     * 前者管「下一节点解析不到操作者怎么办」，后者管「附加操作执行失败怎么办」。</p>
     */
    public static boolean exceptionFallbackEnabled(WfProcessNode node) {
        return node != null && bool(node.getExtJson(), "exceptionHandle", "enabled");
    }

    /**
     * 「流转异常兜底」是否适用于该节点。
     *
     * <p>排除（对齐 ecology「以下几种情况忽略异常处理设置」）：</p>
     * <ul>
     *   <li>创建(0) / 归档(3) / 网关(7)：不是「可办理」节点，不存在「解析操作者为空」；</li>
     *   <li>分叉合并类节点 {@code mergeType ∈ {3 按分支数合并, 4 指定分支合并, 5 比例合并}}：
     *       其激活由上游分支决定，不允许被兜底改写走向。</li>
     * </ul>
     */
    public static boolean exceptionFallbackApplicable(WfProcessNode node) {
        if (node == null) {
            return false;
        }
        Integer type = node.getNodeType();
        if (type == null || type == 0 || type == 3 || type == 7) {
            return false;
        }
        Integer merge = node.getMergeType();
        return merge == null || (merge != 3 && merge != 4 && merge != 5);
    }

    /**
     * 兜底方式（未启用 / 不适用 / 取值非法一律返回 {@link #FALLBACK_NONE}）。
     *
     * <p>开关判定：{@code enabled} 显式写为 {@code false} 时关闭；未写 {@code enabled}
     * 但已配置合法 {@code way} 时**视为启用**（便于历史数据与脚本直接配 way 生效）。</p>
     *
     * @return 0=不启用；1=自动流转至下一节点；2=提交至指定节点；3=由用户指定操作者
     */
    public static int exceptionFallbackWay(WfProcessNode node) {
        if (node == null || !exceptionFallbackApplicable(node)) {
            return FALLBACK_NONE;
        }
        int way = intValue(node.getExtJson(), "exceptionHandle", "way", FALLBACK_NONE);
        if (way < FALLBACK_NEXT_NODE || way > FALLBACK_USER_ASSIGN) {
            return FALLBACK_NONE;
        }
        Map<String, Object> m = setting(node.getExtJson(), "exceptionHandle");
        if (m.containsKey("enabled") && !bool(node.getExtJson(), "exceptionHandle", "enabled")) {
            return FALLBACK_NONE;
        }
        return way;
    }

    /** 兜底目标节点 Key（way=2「提交至指定节点」时使用；未配置返回 null） */
    public static String exceptionFallbackTargetNodeKey(WfProcessNode node) {
        return node == null ? null : str(node.getExtJson(), "exceptionHandle", "targetNodeKey");
    }

    // ---------------- 打印内容设置（printSet）：对齐 ecology printflowcomment / printviewtype / printremarkcolumn / printstnull / printshowtype ----------------

    /** 打印显示类型中「沿用显示模板」的魔法值（对齐 ecology printviewtype=oldvalue） */
    public static final String PRINT_VIEW_TYPE_OLD = "oldvalue";

    /** 打印流转意见：0 始终不打印 / 1 放入模板时不打印（默认）/ 2 始终打印（越界归一为默认 1） */
    public static int printFlowComment(WfProcessNode node) {
        if (node == null) {
            return 1;
        }
        int v = fcInt(node, "printSet", "flowComment", 1);
        return (v >= 0 && v <= 2) ? v : 1;
    }

    /** 打印意见显示方式：0 只显示最后一次（默认）/ 1 显示全部（越界归一为默认 0） */
    public static int printShowType(WfProcessNode node) {
        if (node == null) {
            return 0;
        }
        int v = fcInt(node, "printSet", "showType", 0);
        return (v == 0 || v == 1) ? v : 0;
    }

    /** 打印意见分栏列数：1（默认）/ 2 / 3（越界归一为默认 1） */
    public static int printRemarkColumn(WfProcessNode node) {
        if (node == null) {
            return 1;
        }
        int v = fcInt(node, "printSet", "remarkColumn", 1);
        return (v >= 1 && v <= 3) ? v : 1;
    }

    /** 打印时不显示空意见 */
    public static boolean printStNull(WfProcessNode node) {
        return fcBool(node, "printSet", "stNull");
    }

    /**
     * 打印显示类型。
     *
     * @return {@code ["oldvalue"]} 表示沿用显示模板；否则为意见类型键列表；未配置返回空列表（= 按显示模板口径）
     */
    public static List<String> printViewTypes(WfProcessNode node) {
        return fcList(node, "printSet", "viewTypes");
    }

    // ---------------- 签字意见显示设置（opinionDisplay）：屏显口径 ----------------

    /** 显示全部意见：0 仅显示最后一次（默认）/ 1 显示全部 */
    public static int opinionViewTypeAll(WfProcessNode node) {
        return fcInt(node, "opinionDisplay", "viewTypeAll", 0);
    }

    /** 显示方式：0 按时间倒序（默认）/ 1 按节点正序 */
    public static int opinionShowType(WfProcessNode node) {
        return fcInt(node, "opinionDisplay", "showType", 0);
    }

    /** 意见分栏列数：1（默认）/ 2 / 3 */
    public static int opinionRemarkColumn(WfProcessNode node) {
        int v = fcInt(node, "opinionDisplay", "remarkColumn", 1);
        return (v >= 1 && v <= 3) ? v : 1;
    }

    /** 不显示空意见 */
    public static boolean opinionStNull(WfProcessNode node) {
        return fcBool(node, "opinionDisplay", "stNull");
    }

    /** 意见类型显示白名单（键如 approve/reject/submit …）；空=全部显示 */
    public static List<String> opinionViewTypes(WfProcessNode node) {
        return fcList(node, "opinionDisplay", "viewTypes");
    }

    /** 组装为渲染包用的 {@link OpinionDisplayVO}（读不到给系统默认值，绝不抛异常） */
    public static OpinionDisplayVO opinionDisplay(WfProcessNode node) {
        OpinionDisplayVO vo = new OpinionDisplayVO();
        vo.setViewTypeAll(opinionViewTypeAll(node));
        vo.setShowType(opinionShowType(node));
        vo.setRemarkColumn(opinionRemarkColumn(node));
        vo.setStNull(opinionStNull(node) ? 1 : 0);
        vo.setViewTypes(opinionViewTypes(node));
        return vo;
    }

    /**
     * 组装为渲染包用的 {@link PrintSetVO}（读不到一律给系统默认值，绝不抛异常）。
     *
     * <p>供 {@code GET /form/render} 与 {@code GET /form/preview} 随渲染包下发，
     * 使打印/预览端能按节点配置渲染打印内容。</p>
     */
    public static PrintSetVO printSet(WfProcessNode node) {
        PrintSetVO vo = new PrintSetVO();
        vo.setFlowComment(printFlowComment(node));
        vo.setShowType(printShowType(node));
        vo.setRemarkColumn(printRemarkColumn(node));
        vo.setStNull(printStNull(node));
        vo.setViewTypes(printViewTypes(node));
        return vo;
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
     * 指定流转·多目标列表（对齐 E9 多目标节点：逗号列表 + 下标对齐操作者）。
     *
     * <p>每个目标：{@code nodeKey}（目标节点）、{@code operatorIds}（该节点的指定操作者，缺省按节点设置解析）、
     * {@code signType}（会签/或签等，缺省 0=节点默认）。</p>
     *
     * @return 多目标列表（未配置或为空返回空列表，绝不抛异常）
     */
    public static List<Map<String, Object>> appointFlowTargets(WfProcessNode node) {
        if (node == null) {
            return List.of();
        }
        List<Map<String, Object>> targets = listMaps(node.getExtJson(), "appointFlow", "targets");
        return targets == null ? List.of() : targets;
    }

    /**
     * 表单日志可见节点集合（三态，与签字意见显示范围 {@code signOpinion.viewNodeMode} 统一语义）。
     *
     * <p>新值 {@code {scope:'all'|'none'|'list', nodeKeys:[...]}}：</p>
     * <ul>
     *   <li>{@code all}（或节点未配置 scope 且无 nodeKeys）→ 全部可见，返回 {@code null}（不限制）；</li>
     *   <li>{@code none} → 全不可见，返回空集合（所有节点日志被过滤掉）；</li>
     *   <li>{@code list} → 仅 nodeKeys 内节点可见。</li>
     * </ul>
     *
     * <p>⚠️ 三态边界：<b>未配置 = 全部可见</b>，<b>显式 none = 全不可见</b>，二者不可混淆
     * （旧注释把「不选=全部可见」误写成反向，已在 v1.2 纠偏）。</p>
     *
     * <p>旧值 {@code {scope:'node'}} 降级兼容为「仅当前节点」；{@code {scope:'all'|'self'}} 降级为不限制。</p>
     *
     * @return null 表示不限制（全部可见）；空集合表示全不可见；否则为可见节点 Key 集合
     */
    public static List<String> formLogVisibleNodeKeys(WfProcessNode node) {
        if (node == null) {
            return null;
        }
        String scope = str(node.getExtJson(), "formLogScope", "scope");
        if ("none".equals(scope)) {
            return List.of(); // 全不可见
        }
        if ("all".equals(scope)) {
            return null; // 全部可见
        }
        List<String> keys = list(node.getExtJson(), "formLogScope", "nodeKeys");
        if (!keys.isEmpty()) {
            return keys; // 部分可见
        }
        // 旧值兼容
        if ("node".equals(scope)) {
            return Collections.singletonList(node.getNodeKey());
        }
        return null; // 未配置 → 全部可见（旧默认）
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
