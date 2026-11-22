package org.springblade.workflow.action;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.util.BpmnExtensionUtil;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * E1 ServiceTask 自动执行的统一 JavaDelegate（2026-11-18）。
 *
 * <p>部署期由 {@code WfDefinitionServiceImpl#injectServiceTaskDelegate} 把「挂了 customAction 的
 * ServiceTask」实现注入为 {@code ${wfServiceTaskDelegate}}；运行时本 delegate 做三件事：</p>
 * <ol>
 *   <li>从 {@link DelegateExecution#getCurrentFlowElement()} 反读 {@code wf:node/customAction@actionKey}；</li>
 *   <li>由 {@code engineInstId} 反查业务实例 {@link WfInstance}（gate 的 is_test 判据依赖它）；</li>
 *   <li>调用 {@link WfActionExecutor#executeCustomAction} —— 内部拼 {@code action:&lt;key&gt;} 走
 *       {@code execute()} 完整分派，**保留 action-gate 守卫**，绝不直接调 doCustomAction。</li>
 * </ol>
 *
 * <p><b>安全护栏</b>：总开关 {@code blade.workflow.compat.service-task-exec} 默认 false，零行为变化；
 * 缺 actionKey / 找不到实例 / 未注册动作均跳过或由执行器抛错，日志写清 instId/nodeKey/actionKey。</p>
 *
 * <p><b>与 E7 分工</b>：本 delegate 只执行自定义动作、<b>不做 advance/建待办</b>；
 * ServiceTask 完成后的台账同步由 E7 桥（{@code WfEngineAutoTodoBridgeListener}）负责，二者互补不重复。</p>
 *
 * <p><b>异常策略</b>：动作执行失败由执行器抛出 ServiceException → 当前事务回滚、token 停在 ServiceTask，
 * 与人工节点附加动作语义一致（动作没成功不能让流程继续）。本项目 AsyncExecutor 关闭、无 deadletter。</p>
 */
@Slf4j
@Component("wfServiceTaskDelegate")
@RequiredArgsConstructor
public class WfServiceTaskDelegate implements JavaDelegate {

    /** 自动节点阶段标识（写入 WfActionRequest.phase / 日志） */
    private static final String PHASE = "serviceTask";
    /** 节点类型 6 = 自动处理 */
    private static final int NODE_TYPE_AUTO = 6;

    private final WfFlowtableCompatProperties compat;
    private final WfInstanceMapper instanceMapper;
    private final WfActionExecutor actionExecutor;

    @Override
    public void execute(DelegateExecution execution) {
        String procInstId = execution.getProcessInstanceId();
        if (!compat.isServiceTaskExec()) {
            log.debug("[e1-service-task] 开关关闭，跳过. procInstId={}", procInstId);
            return;
        }
        FlowElement element = execution.getCurrentFlowElement();
        if (!(element instanceof ServiceTask)) {
            log.warn("[e1-service-task] 非 ServiceTask 触发，跳过. procInstId={}, element={}",
                procInstId, element == null ? null : element.getId());
            return;
        }
        BpmnExtensionUtil.WfNodeExt nodeExt = BpmnExtensionUtil.readNode(element);
        if (nodeExt == null || nodeExt.customActions == null) {
            log.warn("[e1-service-task] ServiceTask 无 wf:node/customAction，跳过. procInstId={}, nodeKey={}",
                procInstId, element.getId());
            return;
        }
        BpmnExtensionUtil.WfCustomActionExt action = nodeExt.customActions.stream()
            .filter(c -> c != null && c.actionKey != null && !c.actionKey.isBlank())
            .findFirst().orElse(null);
        if (action == null) {
            log.warn("[e1-service-task] customAction 无有效 actionKey，跳过. procInstId={}, nodeKey={}",
                procInstId, element.getId());
            return;
        }

        WfInstance inst = instanceMapper.selectOne(
            Wrappers.<WfInstance>lambdaQuery()
                .eq(WfInstance::getEngineInstId, procInstId).last("LIMIT 1"));
        if (inst == null) {
            log.warn("[e1-service-task] 找不到业务实例，跳过. procInstId={}, nodeKey={}, actionKey={}",
                procInstId, element.getId(), action.actionKey);
            return;
        }
        // 灰度判定（运行期双保险）：覆盖「工件在开关打开前已按旧配置部署、之后才打开开关」的场景——
        // 那批工件的 ServiceTask 里已带 delegateExpression，若只靠部署期过滤会照常触发动作。
        if (!inGray(inst)) {
            log.debug("[e1-service-task] 灰度名单外，跳过. instId={}, defId={}", inst.getId(), inst.getDefId());
            return;
        }

        WfProcessNode node = new WfProcessNode();
        node.setDefId(inst.getDefId());
        node.setNodeKey(element.getId());
        node.setNodeName(element.getName());
        node.setNodeType(NODE_TYPE_AUTO);

        log.info("[e1-service-task] 执行自定义动作. instId={}, procInstId={}, nodeKey={}, actionKey={}",
            inst.getId(), procInstId, element.getId(), action.actionKey);
        actionExecutor.executeCustomAction(inst, node, PHASE, action.actionKey, null,
            engineVariables(execution));
    }

    /** 灰度：白名单空=全部生效；非空=仅名单内 defId。 */
    private boolean inGray(WfInstance inst) {
        List<Long> gray = compat.getServiceTaskExecGrayDefIds();
        return gray == null || gray.isEmpty()
            || (inst.getDefId() != null && gray.contains(inst.getDefId()));
    }

    /** 引擎变量快照：动作可读流程变量（如发起时写入的 approver 等）。 */
    private Map<String, Object> engineVariables(DelegateExecution execution) {
        try {
            Map<String, Object> vars = execution.getVariables();
            return vars == null ? Collections.emptyMap() : vars;
        } catch (Exception e) {
            log.warn("[e1-service-task] 读取引擎变量失败，按空处理. err={}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
