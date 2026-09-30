package org.springblade.workflow.resolver;

import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.BusinessRuleTask;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.ManualTask;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.ReceiveTask;
import org.flowable.bpmn.model.ScriptTask;
import org.flowable.bpmn.model.SendTask;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.ThrowEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springblade.workflow.entity.WfNodeLink;
import org.springblade.workflow.entity.WfNodeTimeout;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfProcessNode;
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

    /**
     * 读单个节点（定义源 BPMN）→ {@link WfProcessNode}（去 wf_ 表：定义期节点配置改从 BPMN 读）。
     *
     * <p>节点名取 BPMN {@code UserTask} 的 name，其余维度取 {@code wf:node} 扩展。
     * BPMN 里没有对应 UserTask 返回 null（调用方按「未配置」处理或回退 wf_* 表）。</p>
     */
    public WfProcessNode node(Long defId, String nodeKey) {
        UserTask task = findUserTask(defId, nodeKey);
        if (task == null) {
            return null;
        }
        return toProcessNode(defId, nodeKey, task.getName(), BpmnExtensionUtil.readNode(task));
    }

    /**
     * 读出口（连线）列表（定义源 BPMN）：真实 sequenceFlow 上的 {@code wf:link}
     * ＋ 流程级 {@code wf:foldedLink}（网关折叠合成连线）合并返回。
     *
     * <p>注意：折叠连线（A→网关→B 折叠为 A→B）在 BPMN 中<b>没有</b>对应的 A→B sequenceFlow，
     * 只能由流程级 {@code wf:foldedLink} 承载；只遍历 sequenceFlow 会漏掉它们，
     * 导致「可退回节点 / 下一节点」计算与设计器连线不一致。</p>
     *
     * <p>解析走 {@link #resolveOwnProcDefId}（def 自己的部署）：草稿/未部署返回空列表，由调用方回退 wf_* 表。</p>
     */
    public List<WfNodeLink> links(Long defId) {
        String procDefId = resolveOwnProcDefId(defId);
        if (procDefId == null) {
            return List.of();
        }
        BpmnModel model = modelCache.computeIfAbsent(procDefId, this::loadModel);
        return toNodeLinks(defId, model);
    }

    /**
     * 读全部节点（定义源 BPMN）：每个 {@code UserTask}（含 nodeType=0 的「创建/申请人」开始节点，
     * 回填/保存时均以 nodeKey=UserTask id 写 {@code wf:node} 扩展）→ {@link WfProcessNode}，
     * 按 {@code sortOrder} 升序（空值排末尾），与 {@code wf_process_node} 表读语义一致。
     *
     * <p>解析走 {@link #resolveOwnProcDefId}（def 自己的部署）：草稿/未部署返回空列表，由调用方回退 wf_* 表。</p>
     */
    public List<WfProcessNode> nodes(Long defId) {
        String procDefId = resolveOwnProcDefId(defId);
        if (procDefId == null) {
            return List.of();
        }
        BpmnModel model = modelCache.computeIfAbsent(procDefId, this::loadModel);
        return toNodes(defId, model);
    }

    /**
     * BPMN 模型 → 全部节点 {@link WfProcessNode} 列表（按 sortOrder 升序、空值排末尾）。
     * 纯函数（仅依赖 BpmnModel），便于单测回归；未写 {@code wf:node} 扩展的 UserTask
     * 仅回填 defId/nodeKey/nodeName（与 {@link #toProcessNode} 语义一致）。
     *
     * <p><b>非 UserTask 元素合成（2026-09-30 实测补缺）</b>：回填/保存只把 {@code wf:node} 扩展写在
     * UserTask 上，但 {@code wf_process_node} 还含开始(0)/结束(3)/网关(7)/等待(5)/自动处理(6) 等行——
     * 发起页按 nodeType=0 定位开始节点，退回剔除也依赖类型，缺失会造成「BPMN 读源比表读少行」。
     * 类型映射对齐 DB 实测约定（dev 库 428 行分布）：StartEvent→0、EndEvent→3、Gateway→7、
     * ReceiveTask/中间抛出/捕获事件→5、Service/Script/Send/BusinessRule/CallActivity→6、ManualTask→1。
     * 合成节点无 sortOrder（BPMN 扩展未承载）排末尾，且无 signOrder/allowReject 等扩展维度（按 null=默认处理）。</p>
     */
    public static List<WfProcessNode> toNodes(Long defId, BpmnModel model) {
        List<WfProcessNode> result = new ArrayList<>();
        if (model == null || model.getMainProcess() == null) {
            return result;
        }
        Process process = model.getMainProcess();
        // ① UserTask：wf:node 扩展承载全部业务维度
        for (UserTask t : process.findFlowElementsOfType(UserTask.class, true)) {
            result.add(toProcessNode(defId, t.getId(), t.getName(), BpmnExtensionUtil.readNode(t)));
        }
        // ② 非 UserTask 元素合成（含子流程，BoundaryEvent 挂在 activity 上、不在此遍历范围）
        for (StartEvent e : process.findFlowElementsOfType(StartEvent.class, true)) {
            result.add(syntheticNode(defId, e, 0));
        }
        for (EndEvent e : process.findFlowElementsOfType(EndEvent.class, true)) {
            result.add(syntheticNode(defId, e, 3));
        }
        for (Gateway e : process.findFlowElementsOfType(Gateway.class, true)) {
            result.add(syntheticNode(defId, e, 7));
        }
        for (FlowElement e : process.findFlowElementsOfType(FlowElement.class, true)) {
            Integer type = syntheticNodeType(e);
            if (type != null) {
                result.add(syntheticNode(defId, e, type));
            }
        }
        result.sort(Comparator.comparingInt((WfProcessNode n) ->
            n.getSortOrder() == null ? Integer.MAX_VALUE : n.getSortOrder()));
        return result;
    }

    /** 非 UserTask 元素 → {@code wf_process_node} 的 nodeType（见 {@link #toNodes} 类型映射表）；null=不合成 */
    private static Integer syntheticNodeType(FlowElement fe) {
        if (fe instanceof ReceiveTask || fe instanceof IntermediateCatchEvent
            || fe instanceof ThrowEvent) {
            return 5;
        }
        if (fe instanceof ServiceTask || fe instanceof ScriptTask || fe instanceof SendTask
            || fe instanceof BusinessRuleTask || fe instanceof CallActivity) {
            return 6;
        }
        if (fe instanceof ManualTask) {
            return 1;
        }
        return null;
    }

    private static WfProcessNode syntheticNode(Long defId, FlowElement e, int nodeType) {
        WfProcessNode n = new WfProcessNode();
        n.setDefId(defId);
        n.setNodeKey(e.getId());
        n.setNodeName(e.getName());
        n.setNodeType(nodeType);
        return n;
    }

    /**
     * BPMN 模型 → {@link WfNodeLink} 列表（真实连线 + 折叠连线），按 sortOrder 升序。
     * 纯函数（仅依赖 BpmnModel），便于单测回归。
     */
    public static List<WfNodeLink> toNodeLinks(Long defId, BpmnModel model) {
        List<WfNodeLink> result = new ArrayList<>();
        if (model == null || model.getMainProcess() == null) {
            return result;
        }
        Process process = model.getMainProcess();
        // ① 真实 sequenceFlow
        for (FlowElement fe : process.getFlowElements()) {
            if (!(fe instanceof SequenceFlow flow)) {
                continue;
            }
            WfNodeLink l = new WfNodeLink();
            l.setDefId(defId);
            l.setFromNodeKey(flow.getSourceRef());
            l.setToNodeKey(flow.getTargetRef());
            l.setConditionExpr(flow.getConditionExpression());
            BpmnExtensionUtil.WfLinkExt ext = BpmnExtensionUtil.readLink(flow);
            if (ext != null) {
                l.setIsReject(toInt(ext.isReject));
                l.setIsMustPass(toInt(ext.isMustPass));
                l.setConditionCn(ext.conditionCn);
                l.setSortOrder(toInt(ext.sortOrder));
                l.setViaGateway(toInt(ext.viaGateway));
                l.setViaGatewayKey(ext.viaGatewayKey);
                l.setExtraOperations(ext.extraOperations);
            }
            result.add(l);
        }
        // ② 流程级折叠连线（无对应 sequenceFlow）
        for (BpmnExtensionUtil.WfFoldedLinkExt f : BpmnExtensionUtil.readFoldedLinks(process)) {
            WfNodeLink l = new WfNodeLink();
            l.setDefId(defId);
            l.setFromNodeKey(f.from);
            l.setToNodeKey(f.to);
            l.setViaGateway(1);
            l.setViaGatewayKey(f.viaGatewayKey);
            l.setIsReject(toInt(f.isReject));
            l.setIsMustPass(toInt(f.isMustPass));
            l.setConditionCn(f.conditionCn);
            l.setSortOrder(toInt(f.sortOrder));
            l.setExtraOperations(f.extraOperations);
            result.add(l);
        }
        result.sort(Comparator.comparingInt(l -> l.getSortOrder() == null ? Integer.MAX_VALUE : l.getSortOrder()));
        return result;
    }

    /**
     * P3-5：BPMN {@code wf:node} 扩展 → {@link WfProcessNode}（与 {@code wf_process_node} 表读语义一致）。
     *
     * <p>纯函数、无 Spring 依赖，供 {@code definitionFromBpmn} 开关使用并便于单测回归。
     * {@code ext} 为 null（该 UserTask 未写 {@code wf:node} 扩展）时只回填 defId/nodeKey/nodeName，
     * 其余维度保持 null，由调用方按「未配置」处理。</p>
     */
    public static WfProcessNode toProcessNode(Long defId, String nodeKey, String nodeName,
                                              BpmnExtensionUtil.WfNodeExt ext) {
        WfProcessNode n = new WfProcessNode();
        n.setDefId(defId);
        n.setNodeKey(nodeKey);
        n.setNodeName(nodeName);
        if (ext != null) {
            n.setNodeType(toInt(ext.nodeType));
            n.setSignOrder(toInt(ext.signOrder));
            n.setMergeType(toInt(ext.mergeType));
            n.setPassNum(toInt(ext.passNum));
            n.setAllowReject(toInt(ext.allowReject));
            n.setAllowForward(toInt(ext.allowForward));
            n.setAutoApprove(toInt(ext.autoApprove));
            n.setSortOrder(toInt(ext.sortOrder));
            n.setTestStatus(toInt(ext.testStatus));
            n.setExtJson(ext.extJson);
        }
        return n;
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

    /**
     * 按 def <b>自身的部署</b>精确解析 procDefId（deploy 时回写的 {@code wf_process_definition.proc_def_id}）。
     *
     * <p>与 {@link #resolveProcDefId}（按 procKey 取引擎最新版本）的区别：定义期读源（/nodes /links）
     * 必须锚定 def 自己那版——草稿/未激活版本没有部署（procDefId 为空）时返回 null，
     * 让调用方回退 {@code wf_process_node}/{@code wf_node_link}，而不是错拿同 procKey 已部署版本的数据。</p>
     */
    private String resolveOwnProcDefId(Long defId) {
        try {
            WfProcessDefinition def = defMapper.selectById(defId);
            if (def == null || def.getProcDefId() == null || def.getProcDefId().isBlank()) {
                return null;
            }
            return def.getProcDefId();
        } catch (Exception e) {
            log.warn("[blade-workflow] 按定义自身部署解析 procDefId 失败. defId={}, {}", defId, e.getMessage());
            return null;
        }
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
