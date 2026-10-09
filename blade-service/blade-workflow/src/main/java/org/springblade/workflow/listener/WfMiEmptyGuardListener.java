package org.springblade.workflow.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.delegate.event.FlowableMultiInstanceActivityEvent;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.service.helper.WfWriteHelper;
import org.springblade.workflow.util.BpmnExtensionUtil;
import org.springblade.workflow.utils.WfAuthUtil;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * MI 空集合守卫监听（2026-10-09 产品拍板：把「静默跳过」变成「当场可见的失败」）。
 *
 * <p><b>背景</b>：会签/或签/依次下沉引擎多实例后，进入 MI 节点前由 {@code injectMiCollectionVars}
 * 注入集合变量 {@code wfMiAssignees_<nodeKey>}。解析结果为空时（操作者未配置 / 人员离职 /
 * {@code opType=18} 依赖的 starter 或 manager_id 失效），引擎按 <b>0 实例</b>展开 =
 * 节点被「瞬间完成」并静默跳过 —— 流程无声少了审批级，是最隐蔽的故障
 * （dev 实测：Task_Department opType=18 空集合整级跳过，见
 * doc/md/Flowable8承接台账模块-去wf_表改造分析.md 的往返保真章节）。</p>
 *
 * <p><b>拦截点</b>：{@link FlowableEngineEventType#MULTI_INSTANCE_ACTIVITY_STARTED} ——
 * 引擎进入 MI 活动根执行时派发（{@code ContinueProcessOperation#executeActivityBehavior}），
 * <b>先于</b>集合求值与子实例创建（MI 子实例进入时派发的是普通 {@code ACTIVITY_STARTED}，
 * 不会重复触发本守卫）。在此抛异常即可回滚整次流转，节点保持原状。</p>
 *
 * <p><b>行为</b>：集合为空（或未注入，同样会 0 实例跳过）时 ——
 * <ul>
 *   <li>节点配置了「流程异常处理」兜底（{@code settings.exceptionHandle} way=1/2）：放行
 *       天然跳过（way=1「自动流转至下一节点」与跳过落点一致），并补审批日志留痕；
 *       way=2「提交至指定节点」对 MI 节点的重路由为已知限制（监听期不能安全移 token），同样放行+留痕；</li>
 *   <li>未配置兜底：抛 {@link org.flowable.common.engine.api.FlowableException} 阻断本次流转，
 *       错误消息点名节点，用户当场可见。</li>
 * </ul></p>
 *
 * <p><b>依赖策略</b>：守卫判定只依赖引擎数据（事件 + 执行变量 + 部署 BPMN），全部经
 * {@link CommandContextUtil} 取当前命令上下文，<b>不注入引擎 Service</b>（避免与
 * FlowableConfig 构造期循环依赖，同 WfBizCallbackListener 的装配约束）；
 * wf_* 侧（实例反查 / 审批日志留痕）尽力而为，失败只记 warn，不影响守卫判定。</p>
 *
 * <p><b>开关</b>：{@code blade.workflow.mi-empty-guard.enabled}，默认 true。
 * 关闭即完全回到「空集合静默跳过」的历史行为。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "blade.workflow.mi-empty-guard.enabled", havingValue = "true", matchIfMissing = true)
public class WfMiEmptyGuardListener implements FlowableEventListener {

    private static final String MI_VAR_PREFIX = "wfMiAssignees_";

    private final WfInstanceMapper instanceMapper;
    private final WfWriteHelper writeHelper;

    public WfMiEmptyGuardListener(@Autowired(required = false) WfInstanceMapper instanceMapper,
                                  @Autowired(required = false) WfWriteHelper writeHelper) {
        this.instanceMapper = instanceMapper;
        this.writeHelper = writeHelper;
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (!(event.getType() == FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED)
            || !(event instanceof FlowableMultiInstanceActivityEvent ee)) {
            return;
        }
        String activityId = ee.getActivityId();
        String executionId = ee.getExecutionId();
        if (activityId == null || activityId.isBlank() || executionId == null) {
            return;
        }
        ProcessEngineConfigurationImpl cfg = CommandContextUtil.getProcessEngineConfiguration();
        if (cfg == null) {
            return;
        }
        try {
            ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
            ExecutionEntity execution = executionEntityManager == null ? null : executionEntityManager.findById(executionId);
            if (execution == null) {
                return;
            }
            // 集合非空 = 正常逐人展开，放行
            Object value = execution.getVariable(MI_VAR_PREFIX + activityId);
            if (value instanceof Collection<?> c && !c.isEmpty()) {
                return;
            }
            // 空集合或未注入（未注入同样会 0 实例静默跳过）→ 守卫
            String nodeLabel = resolveNodeLabel(cfg, ee.getProcessDefinitionId(), activityId);
            if (hasExceptionFallback(cfg, ee.getProcessDefinitionId(), activityId)) {
                // 配了兜底：放行天然跳过（way=1 落点与跳过一致；way=2 的 MI 重路由为已知限制），留痕
                log.warn("[WfMiEmptyGuard] MI 节点未解析到操作者，按「流程异常处理」放行自动跳过: procInstId={}, node={}",
                    ee.getProcessInstanceId(), nodeLabel);
                appendGuardLog(ee.getProcessInstanceId(), activityId, nodeLabel,
                    "节点「" + nodeLabel + "」未解析到操作者（操作者可能已离职/未配置），按「流程异常处理」自动跳过该节点");
                return;
            }
            String msg = "流程流转被阻止：节点「" + nodeLabel + "」未解析到任何办理人"
                + "（操作者可能已离职/未配置，或多实例集合为空）→ 该节点会被整级跳过，已按「MI 空集合守卫」拦截。"
                + "请检查节点操作者设置，或为该节点配置「流程异常处理」兜底。";
            log.error("[WfMiEmptyGuard] 拦截 MI 空集合静默跳过: procInstId={}, node={}, cause={}",
                ee.getProcessInstanceId(), nodeLabel, (value == null ? "集合变量未注入" : "集合为空"));
            throw new org.flowable.common.engine.api.FlowableException(msg);
        } catch (org.flowable.common.engine.api.FlowableException fe) {
            throw fe;
        } catch (Exception e) {
            // 守卫自身的探测异常不应掩盖真实流转错误：记 warn 放行（与台账监听「数据问题不阻断」口径一致）
            log.warn("[WfMiEmptyGuard] 守卫探测失败（放行）: procInstId={}, activityId={}, {}",
                ee.getProcessInstanceId(), activityId, e.getMessage());
        }
    }

    @Override
    public boolean isFailOnException() {
        // 拦截异常必须向上传播回滚整次流转（C1 强一致，同台账监听阶段2语义）
        return true;
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        // 不在事务提交/回滚生命周期额外触发，仅随引擎 Command 同步派发（同台账监听）
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }

    /** 节点显示名：BPMN 元素 name(id)，取不到就退 activityId */
    private String resolveNodeLabel(ProcessEngineConfigurationImpl cfg, String processDefinitionId, String activityId) {
        try {
            FlowElement fe = findFlowElement(cfg.getRepositoryService().getBpmnModel(processDefinitionId), activityId);
            if (fe instanceof UserTask ut) {
                String name = ut.getName();
                return (name == null || name.isBlank()) ? activityId : name + "(" + activityId + ")";
            }
        } catch (Exception ignore) {
            // 模型取不到时退 activityId
        }
        return activityId;
    }

    /** 节点是否配置了「流程异常处理」兜底（读部署 BPMN wf:node extJson，与运行期读源同源） */
    private boolean hasExceptionFallback(ProcessEngineConfigurationImpl cfg, String processDefinitionId, String activityId) {
        try {
            FlowElement fe = findFlowElement(cfg.getRepositoryService().getBpmnModel(processDefinitionId), activityId);
            if (!(fe instanceof UserTask ut)) {
                return false;
            }
            BpmnExtensionUtil.WfNodeExt ext = BpmnExtensionUtil.readNode(ut);
            if (ext == null || ext.extJson == null || ext.extJson.isBlank()) {
                return false;
            }
            WfProcessNode probe = new WfProcessNode();
            if (ext.nodeType != null && !ext.nodeType.isBlank()) {
                probe.setNodeType(Integer.valueOf(ext.nodeType.trim()));
            }
            probe.setExtJson(ext.extJson);
            return WfNodeSettingsUtil.exceptionFallbackWay(probe) != WfNodeSettingsUtil.FALLBACK_NONE;
        } catch (Exception e) {
            log.debug("[WfMiEmptyGuard] 读取节点异常兜底配置失败（按未配置处理）: activityId={}, {}", activityId, e.getMessage());
            return false;
        }
    }

    /** 主流程 + 一级子流程内查找元素（MI 节点在子流程中的场景） */
    private FlowElement findFlowElement(BpmnModel model, String activityId) {
        if (model == null || model.getMainProcess() == null) {
            return null;
        }
        FlowElement fe = model.getMainProcess().getFlowElement(activityId);
        if (fe != null) {
            return fe;
        }
        List<SubProcess> subs = model.getMainProcess().findFlowElementsOfType(SubProcess.class);
        for (SubProcess sp : subs) {
            FlowElement inner = sp.getFlowElement(activityId);
            if (inner != null) {
                return inner;
            }
        }
        return null;
    }

    /** 留痕到审批日志（尽力而为：表/上下文不可用时只记 warn，不影响守卫判定） */
    private void appendGuardLog(String procInstId, String nodeKey, String nodeLabel, String opinion) {
        if (instanceMapper == null || writeHelper == null) {
            log.info("[WfMiEmptyGuard] （无 wf_* 依赖，跳过留痕）procInstId={}, node={}, {}", procInstId, nodeKey, opinion);
            return;
        }
        try {
            WfInstance inst = instanceMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WfInstance>()
                    .eq(WfInstance::getEngineInstId, procInstId)
                    .last("LIMIT 1"));
            if (inst == null) {
                return;
            }
            writeHelper.appendLog(inst.getId(), null, nodeKey, WfAuthUtil.systemId(),
                org.springblade.workflow.entity.WfApprovalLog.LOG_SUPERVISE, opinion);
        } catch (Exception e) {
            log.warn("[WfMiEmptyGuard] 留痕日志写入失败（不影响流转）: procInstId={}, node={}, {}",
                procInstId, nodeKey, e.getMessage());
        }
    }
}
