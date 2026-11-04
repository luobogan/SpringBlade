package org.springblade.workflow.service.policy;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.jackson.JsonUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 归档 / 撤回流程级策略（解析 {@code wf_process_definition.archive_manage / recall_manage} 两列 JSON）。
 *
 * <p><b>对标 E9</b>：{@code workflow_base.isoverrb}（归档可强制收回）、{@code workflow_base.isoveriv}
 * （归档可干预）、{@code workflow_base.isrejectremind}（退回是否提醒）等流程级开关。
 * 节点级退回配置（允许被撤回的节点 / 排除选择 / 节点前附加操作）在 BPMN 节点
 * {@code extJson.settings.reject.*} 与 {@code extJson.settings.preOperate}，不在本类范围内。</p>
 *
 * <p><b>容错契约（重要）</b>：解析失败（空串 / NULL / 非法 JSON / 字段类型异常）<b>一律降级</b>为
 * 「全部关闭 + 空集合」并记 warn，<b>绝不抛异常</b>——本类会被监控列表批量读取，
 * 一条脏配置不应打断整页渲染。</p>
 *
 * <p><b>单一事实源</b>：流程设计页写入这两列，流程监控按同一份配置决定动作显隐，
 * 后端 {@code WfInstanceServiceImpl} 再按同一份配置做强制校验。</p>
 *
 * @author blade-workflow
 */
@Slf4j
public final class WfArchiveRecallPolicy {

    // ───────────────── 归档管理 JSON 键 ─────────────────

    /** 是否允许「强制归档」 */
    public static final String A_FORCE_ARCHIVE_ENABLED = "forceArchiveEnabled";
    /** 可归档节点范围；{@code ["*"]} = 任意节点，否则为 nodeKey 白名单 */
    public static final String A_ARCHIVE_APPLICABLE_NODE_KEYS = "archiveApplicableNodeKeys";
    /** 归档后允许「强制收回」 */
    public static final String A_RECALL_FROM_ARCHIVE_ENABLED = "recallFromArchiveEnabled";
    /** 归档后允许「流程干预」（回退 / 流转到指定节点） */
    public static final String A_INTERVENE_FROM_ARCHIVE_ENABLED = "interveneFromArchiveEnabled";

    // ───────────────── 撤回管理 JSON 键 ─────────────────

    /** 是否允许「强制收回」 */
    public static final String R_RECALL_ENABLED = "recallEnabled";
    /** 是否允许「监控回退」 */
    public static final String R_REJECT_ENABLED = "rejectEnabled";
    /** 允许前端用户选择是否提醒（总闸，节点级 {@code settings.reject.remind} 为节点覆盖） */
    public static final String R_REMIND_SELECT = "remindSelect";
    /** 强制收回落点范围 */
    public static final String R_RECALL_TARGET_SCOPE = "recallTargetScope";
    /** 强制收回落点范围 = {@link #SCOPE_SPECIFIED} 时的指定清单（nodeKey） */
    public static final String R_RECALL_TARGET_NODE_KEYS = "recallTargetNodeKeys";

    // ───────────────── 枚举值 ─────────────────

    /** 节点通配符：{@code archiveApplicableNodeKeys} 含此值表示任意节点均可归档 */
    public static final String NODE_KEY_ANY = "*";

    /** 落点范围：仅创建节点 */
    public static final String SCOPE_CREATE = "create";
    /** 落点范围：已办节点 + 创建节点 */
    public static final String SCOPE_HISTORY = "history";
    /** 落点范围：任意可停节点 */
    public static final String SCOPE_ALL = "all";
    /** 落点范围：指定清单 */
    public static final String SCOPE_SPECIFIED = "specified";

    /** 落点范围缺省值：{@link #SCOPE_ALL}。
     *
     * <p>取 all 而非更窄的范围，是为了<b>不改变既有行为</b>：本配置项落地前，
     * 「强制收回」的后端落点校验只有「必须是引擎能停住的 UserTask（nodeType 0/1/2）」，
     * 范围由该硬校验兜底；前端候选列表的窄口径是 UI 行为，不应被后端默认收窄。</p>
     */
    public static final String SCOPE_DEFAULT = SCOPE_ALL;

    @Getter
    private final Archive archive;

    @Getter
    private final Recall recall;

    private WfArchiveRecallPolicy(Archive archive, Recall recall) {
        this.archive = archive;
        this.recall = recall;
    }

    /**
     * 解析两列 JSON，构造策略对象。
     *
     * @param archiveManage 归档管理 JSON（可空）
     * @param recallManage  撤回管理 JSON（可空）
     * @return 永不为 null；解析失败降级为「全部关闭」
     */
    public static WfArchiveRecallPolicy of(String archiveManage, String recallManage) {
        return new WfArchiveRecallPolicy(Archive.parse(archiveManage), Recall.parse(recallManage));
    }

    /** 全关闭的空策略（等价于未配置） */
    public static WfArchiveRecallPolicy empty() {
        return of(null, null);
    }

    // ───────────────── 便捷判定（转发到内部对象） ─────────────────

    /**
     * 该流程是否<b>显式配置</b>了归档管理策略。
     *
     * <p>用于区分两种语义（后端强校验时必须区分，否则会误伤存量流程）：</p>
     * <ul>
     *   <li><b>未配置</b>（本方法 false）→ 对<b>既有</b>能力（如强制归档）保持原有行为（放行），
     *       对<b>全新</b>能力（归档后强制收回 / 归档后干预）保持关闭（拒绝）；</li>
     *   <li><b>已配置</b>（本方法 true）→ 一律按配置裁决，显式 false 即拒绝。</li>
     * </ul>
     */
    public boolean isArchiveConfigured() {
        return archive.isConfigured();
    }

    /**
     * 该流程是否<b>显式配置</b>了撤回管理策略（语义同 {@link #isArchiveConfigured()}）。
     */
    public boolean isRecallConfigured() {
        return recall.isConfigured();
    }

    /** 是否允许「强制归档」 */
    public boolean isForceArchiveEnabled() {
        return archive.isForceArchiveEnabled();
    }

    /** 归档后是否允许「强制收回」 */
    public boolean isRecallFromArchiveEnabled() {
        return archive.isRecallFromArchiveEnabled();
    }

    /** 归档后是否允许「流程干预」 */
    public boolean isInterveneFromArchiveEnabled() {
        return archive.isInterveneFromArchiveEnabled();
    }

    /** 可归档节点范围（空集合 = 不限制之外的任何判定都需显式配置，见 {@link #isNodeArchiveApplicable}） */
    public Set<String> getArchiveApplicableNodeKeys() {
        return archive.getArchiveApplicableNodeKeys();
    }

    /**
     * 判定某节点是否在「可归档节点范围」内。
     *
     * <p>口径（前后端必须一致）：</p>
     * <ul>
     *   <li>范围含 {@link #NODE_KEY_ANY} → 任意节点均可归档；</li>
     *   <li>范围为 nodeKey 白名单 → 命中即允许；</li>
     *   <li>范围<b>为空</b> → 视为「未限制」（允许），避免存量行（NULL）因未配置而把
     *       既有「强制归档」能力一刀切掉；是否允许归档仍由 {@code forceArchiveEnabled} 决定。</li>
     * </ul>
     */
    public boolean isNodeArchiveApplicable(String nodeKey) {
        return archive.isNodeArchiveApplicable(nodeKey);
    }

    /** 是否允许「强制收回」 */
    public boolean isRecallEnabled() {
        return recall.isRecallEnabled();
    }

    /** 是否允许「监控回退」 */
    public boolean isRejectEnabled() {
        return recall.isRejectEnabled();
    }

    /** 提醒选择总闸（节点级 {@code settings.reject.remind} 为节点覆盖） */
    public boolean isRemindSelect() {
        return recall.isRemindSelect();
    }

    /** 强制收回落点范围（缺省 {@link #SCOPE_ALL}） */
    public String getRecallTargetScope() {
        return recall.getRecallTargetScope();
    }

    /**
     * 强制收回落点「指定清单」（{@code recallTargetScope=specified} 时生效；为空 = 未指定任何节点）。
     */
    public Set<String> getRecallTargetNodeKeys() {
        return recall.getRecallTargetNodeKeys();
    }

    /**
     * 按落点范围裁决某候选节点是否可作为「强制收回」的落点。
     *
     * <p>与 {@link #getRecallTargetScope()} 配套使用，把范围语义收在一处，
     * 避免前后端各写一套判断导致口径漂移。</p>
     *
     * @param scope       落点范围（{@code create/history/all/specified}，空则取缺省 all）
     * @param isCreateNode 是否为「创建」节点
     * @param isHandledNode 是否为「本实例已办理过」的节点
     * @param inSpecified  是否命中指定清单（{@code scope=specified} 时有效）
     * @return 是否允许作为落点
     */
    public static boolean scopeAllows(String scope, boolean isCreateNode, boolean isHandledNode,
                                      boolean inSpecified) {
        String s = (scope == null || scope.isBlank()) ? SCOPE_DEFAULT : scope.trim();
        switch (s) {
            case SCOPE_CREATE:
                return isCreateNode;
            case SCOPE_HISTORY:
                return isCreateNode || isHandledNode;
            case SCOPE_SPECIFIED:
                return inSpecified;
            case SCOPE_ALL:
            default:
                return true;
        }
    }

    // ───────────────── 内部值对象 ─────────────────

    /**
     * 归档管理策略（对应 {@code archive_manage} 列）。
     *
     * <p><b>「未配置」与「显式关闭」必须区分</b>：{@link #isConfigured()} 为 false 表示
     * 该流程<b>没有</b>配置归档策略 —— 对<b>既有</b>能力（如强制归档）应保持原有行为（放行），
     * 而对<b>全新</b>能力（如归档后强制收回）应保持关闭（拒绝）。</p>
     */
    @Getter
    public static final class Archive {

        private final boolean configured;
        private final boolean forceArchiveEnabled;
        private final Set<String> archiveApplicableNodeKeys;
        private final boolean recallFromArchiveEnabled;
        private final boolean interveneFromArchiveEnabled;

        private Archive(boolean configured, boolean forceArchiveEnabled, Set<String> archiveApplicableNodeKeys,
                        boolean recallFromArchiveEnabled, boolean interveneFromArchiveEnabled) {
            this.configured = configured;
            this.forceArchiveEnabled = forceArchiveEnabled;
            this.archiveApplicableNodeKeys = archiveApplicableNodeKeys;
            this.recallFromArchiveEnabled = recallFromArchiveEnabled;
            this.interveneFromArchiveEnabled = interveneFromArchiveEnabled;
        }

        static Archive parse(String json) {
            Map<String, Object> map = parseMap(json, "archive_manage");
            boolean configured = !map.isEmpty();
            // ⚠️ 默认值口径（前端必须同口径）：
            //   · 「允许强制归档」是**既有**能力 → 缺省 true（未配置/未写该键都保持原有行为），
            //     只有显式 false 才关闭；否则存量流程会被一刀切掉强制归档。
            //   · 「归档后可收回 / 可干预」是**全新**能力 → 缺省 false（必须显式开启才可用）。
            return new Archive(
                configured,
                boolOf(map, A_FORCE_ARCHIVE_ENABLED, true),
                nodeKeysOf(map, A_ARCHIVE_APPLICABLE_NODE_KEYS),
                boolOf(map, A_RECALL_FROM_ARCHIVE_ENABLED, false),
                boolOf(map, A_INTERVENE_FROM_ARCHIVE_ENABLED, false)
            );
        }

        /**
         * 判定某节点是否在可归档范围内（口径见 {@link WfArchiveRecallPolicy#isNodeArchiveApplicable}）。
         */
        public boolean isNodeArchiveApplicable(String nodeKey) {
            if (archiveApplicableNodeKeys.isEmpty()) {
                // 未配置范围 = 不限制（是否允许归档由 forceArchiveEnabled 决定）
                return true;
            }
            if (archiveApplicableNodeKeys.contains(NODE_KEY_ANY)) {
                return true;
            }
            return nodeKey != null && !nodeKey.isBlank() && archiveApplicableNodeKeys.contains(nodeKey.trim());
        }
    }

    /**
     * 撤回管理策略（对应 {@code recall_manage} 列）。
     *
     * <p>「未配置」与「显式关闭」的区分语义同 {@link Archive#isConfigured()}。</p>
     */
    @Getter
    public static final class Recall {

        private final boolean configured;
        private final boolean recallEnabled;
        private final boolean rejectEnabled;
        private final boolean remindSelect;
        private final String recallTargetScope;
        private final Set<String> recallTargetNodeKeys;

        private Recall(boolean configured, boolean recallEnabled, boolean rejectEnabled, boolean remindSelect,
                       String recallTargetScope, Set<String> recallTargetNodeKeys) {
            this.configured = configured;
            this.recallEnabled = recallEnabled;
            this.rejectEnabled = rejectEnabled;
            this.remindSelect = remindSelect;
            this.recallTargetScope = recallTargetScope;
            this.recallTargetNodeKeys = recallTargetNodeKeys;
        }

        static Recall parse(String json) {
            Map<String, Object> map = parseMap(json, "recall_manage");
            boolean configured = !map.isEmpty();
            String scope = strOf(map, R_RECALL_TARGET_SCOPE);
            // ⚠️ 默认值口径（与 Archive 侧一致、前端同口径）：
            //   · 「允许强制收回 / 允许监控回退」是**既有**能力 → 缺省 true（保持原有行为），
            //     只有显式 false 才关闭；
            //   · 「提醒选择总闸」缺省 false。
            return new Recall(
                configured,
                boolOf(map, R_RECALL_ENABLED, true),
                boolOf(map, R_REJECT_ENABLED, true),
                boolOf(map, R_REMIND_SELECT, false),
                (scope == null || scope.isBlank()) ? SCOPE_DEFAULT : scope.trim(),
                nodeKeysOf(map, R_RECALL_TARGET_NODE_KEYS)
            );
        }
    }

    // ───────────────── 解析工具 ─────────────────

    /**
     * 解析 JSON 为 Map；失败返回空 Map（不抛异常）。
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseMap(String json, String column) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> map = JsonUtil.parse(json, Map.class);
            return (map == null) ? Collections.emptyMap() : map;
        } catch (Exception e) {
            log.warn("[WfArchiveRecallPolicy] 策略 JSON 解析失败，降级为「全部关闭」. column={}, err={}",
                column, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 取布尔值：兼容 {@code true/false} 与 {@code "true"/"false"} 两种写法。
     */
    private static boolean boolOf(Map<String, Object> map, String key, boolean defaultValue) {
        Object v = map.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        if (v instanceof String) {
            String s = ((String) v).trim();
            if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
                return true;
            }
            if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
                return false;
            }
        }
        if (v instanceof Number) {
            return ((Number) v).intValue() != 0;
        }
        log.warn("[WfArchiveRecallPolicy] 策略字段类型异常，按「关闭」处理. key={}, type={}",
            key, v.getClass().getSimpleName());
        return defaultValue;
    }

    /**
     * 取字符串值（trim 后非空才返回）。
     */
    private static String strOf(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) {
            return null;
        }
        String s = (v instanceof String) ? ((String) v).trim() : String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * 取节点清单：兼容数组 {@code ["a","b"]} 与逗号分隔串 {@code "a,b"} 两种写法。
     */
    private static Set<String> nodeKeysOf(Map<String, Object> map, String key) {
        Object v = map.get(key);
        Set<String> out = new LinkedHashSet<>();
        if (v == null) {
            return Collections.unmodifiableSet(out);
        }
        List<String> raw = new ArrayList<>();
        if (v instanceof List) {
            for (Object o : (List<?>) v) {
                if (o != null) {
                    String s = String.valueOf(o).trim();
                    if (!s.isEmpty()) {
                        raw.add(s);
                    }
                }
            }
        } else if (v instanceof String) {
            for (String s : ((String) v).split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) {
                    raw.add(t);
                }
            }
        } else if (v instanceof Object[]) {
            for (Object o : (Object[]) v) {
                if (o != null) {
                    String s = String.valueOf(o).trim();
                    if (!s.isEmpty()) {
                        raw.add(s);
                    }
                }
            }
        } else {
            log.warn("[WfArchiveRecallPolicy] 节点清单字段类型异常，按「不限制」处理. key={}, type={}",
                key, v.getClass().getSimpleName());
        }
        out.addAll(raw);
        return Collections.unmodifiableSet(out);
    }
}
