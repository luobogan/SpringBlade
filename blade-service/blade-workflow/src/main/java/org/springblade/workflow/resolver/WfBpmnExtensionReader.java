package org.springblade.workflow.resolver;

import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springblade.workflow.entity.WfNodeTimeout;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.util.BpmnExtensionUtil;
import org.springblade.workflow.vo.DetailFilterVO;
import org.springblade.workflow.vo.DetailPermVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「定义期语义」BPMN {@code wf:} 扩展的读取收口（P3-5）。
 *
 * <p>迁移目标：把运行期对 {@code wf_node_operator} / {@code wf_field_perm} / {@code wf_process_node.ext_json}
 * 的读取，统一改为从 BPMN {@code extensionElements}（随 {@code ACT_GE_BYTEARRAY} 持久化）读取，
 * 让流程引擎成为定义语义的唯一事实源。</p>
 *
 * <p><b>键解析</b>：调用方传入的 {@code defId} 是 blade {@code wf_process_definition.id}（非 Flowable 的
 * {@code PROC_DEF_ID_}）。本类先按 {@code defId} 取 {@code procKey}，再查 Flowable 最新版本
 * {@code procDefId}，最后 {@code repositoryService.getBpmnModel(procDefId)}。</p>
 *
 * <p><b>缓存</b>：以 Flowable {@code procDefId} 为键缓存 {@link BpmnModel}。重部署会产生新的
 * {@code procDefId}，旧键自然失效（不会被 stale 命中），无需主动淘汰；{@code defId → procDefId} 不缓存，
 * 每次实时解析以保证拿到「最新部署」。</p>
 *
 * <p><b>节点匹配</b>：{@code nodeKey} 即 BPMN 中 {@code UserTask} 的元素 id（回填/保存时 {@code nodeKey}
 * 取元素 id，与 {@link BpmnExtensionUtil#readNode} 一致）。</p>
 */
@Slf4j
@Component
public class WfBpmnExtensionReader {

    private final WfProcessDefinitionMapper defMapper;
    private final RepositoryService repositoryService;

    /** procDefId → BpmnModel 缓存（重部署后 procDefId 变化 → 旧键失效） */
    private final Map<String, BpmnModel> modelCache = new ConcurrentHashMap<>(64);

    public WfBpmnExtensionReader(WfProcessDefinitionMapper defMapper, RepositoryService repositoryService) {
        this.defMapper = defMapper;
        this.repositoryService = repositoryService;
    }

    /** 读节点扩展（operator/fieldPerm/detailPerm/timeout/operation 等）；找不到返回 null */
    public BpmnExtensionUtil.WfNodeExt readNode(Long defId, String nodeKey) {
        UserTask task = findUserTask(defId, nodeKey);
        return task == null ? null : BpmnExtensionUtil.readNode(task);
    }

    /** 节点操作者列表（空列表表示无配置） */
    public List<BpmnExtensionUtil.WfOperatorExt> operators(Long defId, String nodeKey) {
        BpmnExtensionUtil.WfNodeExt node = readNode(defId, nodeKey);
        return node == null ? List.of() : node.operators;
    }

    /** 节点字段权限列表（空列表表示无配置；含 main 作用域与 dt 作用域的字段级权限） */
    public List<BpmnExtensionUtil.WfFieldPermExt> fieldPerms(Long defId, String nodeKey) {
        BpmnExtensionUtil.WfNodeExt node = readNode(defId, nodeKey);
        return node == null ? List.of() : node.fieldPerms;
    }

    /** 节点明细表内字段级权限列表（dt 作用域；空列表表示无配置） */
    public List<BpmnExtensionUtil.WfDetailPermExt> detailPerms(Long defId, String nodeKey) {
        BpmnExtensionUtil.WfNodeExt node = readNode(defId, nodeKey);
        return node == null ? List.of() : node.detailPerms;
    }

    /** 节点明细表整表权限列表（空列表表示无配置；字段与 {@code wf_node_detail_perm} 表对齐） */
    public List<BpmnExtensionUtil.WfDetailTablePermExt> detailTablePerms(Long defId, String nodeKey) {
        BpmnExtensionUtil.WfNodeExt node = readNode(defId, nodeKey);
        return node == null ? List.of() : node.detailTablePerms;
    }

    /** 节点明细表字段筛选规则列表（空列表表示无配置；字段与 {@code wf_node_detail_filter} 表对齐） */
    public List<BpmnExtensionUtil.WfDetailFilterExt> detailFilters(Long defId, String nodeKey) {
        BpmnExtensionUtil.WfNodeExt node = readNode(defId, nodeKey);
        return node == null ? List.of() : node.detailFilters;
    }

    /** 节点超时规则列表（空列表表示无配置；字段与 {@code wf_node_timeout} 表对齐） */
    public List<BpmnExtensionUtil.WfTimeoutExt> timeouts(Long defId, String nodeKey) {
        BpmnExtensionUtil.WfNodeExt node = readNode(defId, nodeKey);
        return node == null ? List.of() : node.timeouts;
    }

    /**
     * P3-5：BPMN {@code wf:timeout} 扩展 → {@link WfNodeTimeout} 实体列表（仅已启用，按 seq 升序）。
     *
     * <p>与 {@code wf_node_timeout} 表读（{@code enabled=1} + {@code seq} 升序）语义一致，
     * 供 {@code timeoutFromBpmn} 开关切换使用。纯函数、无 Spring 依赖，便于单元测试回归。</p>
     *
     * @param defId   业务定义 id（仅回填到实体，不参与 BPMN 解析）
     * @param nodeKey 节点 key（仅回填到实体）
     * @param exts    BPMN 解析出的超时扩展列表（可为 null）
     */
    public static List<WfNodeTimeout> toNodeTimeouts(Long defId, String nodeKey, List<BpmnExtensionUtil.WfTimeoutExt> exts) {
        List<WfNodeTimeout> result = new ArrayList<>();
        if (exts == null) {
            return result;
        }
        for (BpmnExtensionUtil.WfTimeoutExt t : exts) {
            if (t.enabled != null && !"1".equals(t.enabled.trim()) && !"true".equalsIgnoreCase(t.enabled.trim())) {
                continue; // 仅保留启用规则，与原表 enabled=1 过滤一致
            }
            WfNodeTimeout r = new WfNodeTimeout();
            r.setDefId(defId);
            r.setNodeKey(nodeKey);
            r.setSeq(toInt(t.seq));
            r.setEnabled(1);
            r.setStartType(toInt(t.startType));
            r.setStartField(t.startField);
            r.setEndType(toInt(t.endType));
            r.setEndFixedTime(t.endFixedTime);
            r.setEndField(t.endField);
            r.setDurationMin(toInt(t.durationMin));
            r.setActionWay(t.actionWay);
            r.setOpinion(t.opinion);
            r.setOperatorIds(t.operatorIds);
            r.setRemindBeforeOperator(toInt(t.remindBeforeOperator));
            r.setRemindTypes(t.remindTypes);
            r.setRemindPersons(t.remindPersons);
            // targetNodeKey 未进 BPMN（扩展位，运行期未使用），保持 null
            result.add(r);
        }
        result.sort(Comparator.comparingInt(r -> r.getSeq() == null ? Integer.MAX_VALUE : r.getSeq()));
        return result;
    }

    private static Integer toInt(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * P3-5：BPMN {@code wf:detailTablePerm} 扩展 → {@link DetailPermVO}（按 dtIndex 升序）。
     * 与 {@code wf_node_detail_perm} 表读语义一致，供 {@code detailPermFromBpmn} 开关使用。纯函数，便于单测回归。
     */
    public static List<DetailPermVO> toDetailPermVOs(List<BpmnExtensionUtil.WfDetailTablePermExt> exts) {
        List<DetailPermVO> result = new ArrayList<>();
        if (exts == null) {
            return result;
        }
        for (BpmnExtensionUtil.WfDetailTablePermExt d : exts) {
            DetailPermVO vo = new DetailPermVO();
            vo.setDtIndex(toInt(d.dtIndex));
            vo.setCanAdd(toInt(d.canAdd));
            vo.setCanEdit(toInt(d.canEdit));
            vo.setCanDelete(toInt(d.canDelete));
            vo.setHideEmpty(toInt(d.hideEmpty));
            vo.setDefaultRows(toInt(d.defaultRows));
            vo.setRequired(toInt(d.required));
            vo.setPrintSerial(toInt(d.printSerial));
            vo.setAllowScroll(toInt(d.allowScroll));
            vo.setOpenPaging(toInt(d.openPaging));
            result.add(vo);
        }
        result.sort(Comparator.comparingInt(v -> v.getDtIndex() == null ? Integer.MAX_VALUE : v.getDtIndex()));
        return result;
    }

    /**
     * P3-5：BPMN {@code wf:detailFilter} 扩展 → {@link DetailFilterVO}（按 modeType 过滤 + dtIndex/fieldName 升序）。
     * 与 {@code wf_node_detail_filter} 表读（modeType 过滤 + dtIndex/id 升序）语义一致，供 {@code detailFilterFromBpmn} 开关使用。纯函数。
     */
    public static List<DetailFilterVO> toDetailFilterVOs(Integer modeType, List<BpmnExtensionUtil.WfDetailFilterExt> exts) {
        List<DetailFilterVO> result = new ArrayList<>();
        if (exts == null) {
            return result;
        }
        for (BpmnExtensionUtil.WfDetailFilterExt d : exts) {
            if (modeType != null && !modeType.equals(toInt(d.modeType))) {
                continue;
            }
            DetailFilterVO vo = new DetailFilterVO();
            vo.setDtIndex(toInt(d.dtIndex));
            vo.setFieldName(d.fieldName);
            vo.setCompareType(toInt(d.compareType));
            vo.setCompareValue(d.compareValue);
            vo.setIsRequired(toInt(d.isRequired));
            result.add(vo);
        }
        result.sort(Comparator.comparingInt((DetailFilterVO v) -> v.getDtIndex() == null ? Integer.MAX_VALUE : v.getDtIndex())
            .thenComparing(v -> v.getFieldName() == null ? "" : v.getFieldName()));
        return result;
    }

    private UserTask findUserTask(Long defId, String nodeKey) {
        String procDefId = resolveProcDefId(defId);
        if (procDefId == null) {
            return null;
        }
        BpmnModel model = modelCache.computeIfAbsent(procDefId, this::loadModel);
        if (model == null || model.getMainProcess() == null) {
            return null;
        }
        Collection<UserTask> tasks = model.getMainProcess().findFlowElementsOfType(UserTask.class, true);
        for (UserTask t : tasks) {
            if (nodeKey.equals(t.getId())) {
                return t;
            }
        }
        return null;
    }

    private String resolveProcDefId(Long defId) {
        try {
            WfProcessDefinition def = defMapper.selectById(defId);
            if (def == null || def.getProcKey() == null || def.getProcKey().isBlank()) {
                return null;
            }
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(def.getProcKey())
                .latestVersion()
                .singleResult();
            return pd == null ? null : pd.getId();
        } catch (Exception e) {
            log.warn("[blade-workflow] 解析流程定义 procDefId 失败. defId={}, {}", defId, e.getMessage());
            return null;
        }
    }

    private BpmnModel loadModel(String procDefId) {
        try {
            return repositoryService.getBpmnModel(procDefId);
        } catch (Exception e) {
            log.warn("[blade-workflow] 读取 BPMN 模型失败（降级返回 null）. procDefId={}, {}", procDefId, e.getMessage());
            return null;
        }
    }
}
