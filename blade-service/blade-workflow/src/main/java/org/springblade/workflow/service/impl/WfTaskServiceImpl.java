package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.action.NodeActionExecutor;
import org.springblade.workflow.dto.AddSignDTO;
import org.springblade.workflow.dto.ApproveDTO;
import org.springblade.workflow.dto.CirculateDTO;
import org.springblade.workflow.dto.ForwardDTO;
import org.springblade.workflow.dto.RejectDTO;
import org.springblade.workflow.dto.UrgeDTO;
import org.springblade.workflow.entity.WfApprovalLog;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfNodeOperator;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.enums.AdvanceSrc;
import org.springblade.workflow.exception.WfAccessDeniedException;
import org.springblade.workflow.mapper.WfApprovalLogMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfNodeOperatorMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.mapper.WfSubflowRequestMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.entity.WfSubflowRequest;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.service.IWfTaskService;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.system.user.feign.IUserClient;
import org.springblade.workflow.entity.WfFormSnapshot;
import org.springblade.workflow.mapper.WfFormSnapshotMapper;
import org.springblade.workflow.reject.WfRejectManager;
import org.springblade.workflow.utils.WfAuthUtil;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
import org.springblade.workflow.vo.RejectCandidateVO;
import org.springblade.workflow.vo.RejectCandidatesVO;
import org.springblade.workflow.vo.TaskVO;
import org.springblade.workflow.vo.WfTaskVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 流程任务语义服务实现
 *
 * <p>会签 / 或签 / 依次语义（对齐 ecology {@code sign_order}）在此落地：</p>
 * <ul>
 *   <li>{@code 0 或签}：任一人处理即推进引擎，同节点其余待办置办结</li>
 *   <li>{@code 1 会签}：全部处理人办完后才推进引擎</li>
 *   <li>{@code 2 依次}：按批次逐个激活，最后一人处理完才推进引擎</li>
 * </ul>
 * <p>退回 / 撤回 / 转发 / 加签 / 抄送 / 催办等国产 OA 语义同样在此实现，
 * BPMN 仅承载主干流转。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfTaskServiceImpl implements IWfTaskService {

    /** 或签 */
    private static final int SIGN_ANY = 0;
    /** 会签 */
    private static final int SIGN_ALL = 1;
    /** 依次 */
    private static final int SIGN_SEQUENCE = 2;

    // 节点信息「操作菜单」的操作码（与前端 wfDict.MENUS_OPTIONS 保持一致）
    private static final String MENU_SUBMIT = "submit";
    private static final String MENU_REJECT = "reject";
    private static final String MENU_FORWARD = "forward";

    /** 系统自动通过时的办理人占位（无人上下文，如超时任务） */
    private static final long AUTO_OPERATOR = 0L;
    /** 超时自动通过的默认意见 */
    private static final String AUTO_OPINION = "超时自动通过";

    private final WfTaskMapper taskMapper;
    private final WfInstanceMapper instanceMapper;
    private final WfProcessNodeMapper nodeMapper;
    private final WfNodeOperatorMapper operatorMapper;
    private final WfApprovalLogMapper logMapper;
    private final IProcessService processService;
    private final IWfInstanceService instanceService;
    private final NodeActionExecutor nodeActionExecutor;
    private final IUserClient userClient;
    private final WfFormSnapshotMapper snapshotMapper;
    private final WfRejectManager rejectManager;
    private final WfSubflowRequestMapper subflowRequestMapper;

    @Override
    public List<WfTaskVO> todo(Long assignee) {
        return list(assignee, List.of(WfTask.STATUS_TODO));
    }

    @Override
    public List<WfTaskVO> done(Long assignee) {
        return list(assignee, List.of(WfTask.STATUS_DONE, WfTask.STATUS_FINISHED,
            WfTask.STATUS_AUTO_SUBMIT, WfTask.STATUS_COADJUTANT,
            WfTask.STATUS_CIRCULATE, WfTask.STATUS_READ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long taskId, ApproveDTO dto) {
        return doApprove(taskId, dto, SecureUtil.getUserId(), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean autoApprove(Long taskId, String opinion) {
        ApproveDTO dto = new ApproveDTO();
        dto.setOpinion(opinion == null ? AUTO_OPINION : opinion);
        return doApprove(taskId, dto, AUTO_OPERATOR, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean autoApprove(Long taskId, String opinion, java.util.Map<String, Object> variables) {
        ApproveDTO dto = new ApproveDTO();
        dto.setOpinion(opinion == null ? AUTO_OPINION : opinion);
        dto.setVariables(variables);
        return doApprove(taskId, dto, AUTO_OPERATOR, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean autoApprove(Long taskId, String opinion, Long operator) {
        ApproveDTO dto = new ApproveDTO();
        dto.setOpinion(opinion == null ? AUTO_OPINION : opinion);
        // 流程测试：以节点「接收人」身份审批；未指定时回退系统。
        Long op = (operator == null) ? AUTO_OPERATOR : operator;
        return doApprove(taskId, dto, op, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean autoApprove(Long taskId, String opinion, java.util.Map<String, Object> variables, Long operator) {
        ApproveDTO dto = new ApproveDTO();
        dto.setOpinion(opinion == null ? AUTO_OPINION : opinion);
        dto.setVariables(variables);
        Long op = (operator == null) ? AUTO_OPERATOR : operator;
        return doApprove(taskId, dto, op, true);
    }

    /**
     * 同意处理主体。
     *
     * @param operator 办理人（系统自动通过时为 {@link #AUTO_OPERATOR}）
     * @param system   系统自动触发：跳过「操作菜单」校验（否则未开 submit 的节点无法自动通过）
     */
    private boolean doApprove(Long taskId, ApproveDTO dto, Long operator, boolean system) {
        WfTask task = requireTodoTask(taskId);
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        WfProcessNode node = loadNode(inst.getDefId(), task.getNodeKey());

        // 0. 记录级鉴权：只有该任务办理人本人（或流程管理员）能同意。
        //    系统自动提交 / 超时自动通过走 system=true，无人上下文，不受此限。
        if (!system) {
            WfAuthUtil.requireOperateTask(task, "同意");
            // 节点信息 → 运行时消费：操作菜单校验 + 签字意见必填
            requireOperate(node, MENU_SUBMIT);
        }
        String opinion = resolveOpinion(node, dto == null ? null : dto.getOpinion());

        // 节点信息 → 运行时消费：二次认证 + 字段校验（失败即拒绝，不推进）
        validateBeforeApprove(node, inst, operator, dto, system);

        // 1. 本任务置已办
        task.setStatus(WfTask.STATUS_DONE);
        task.setOperateTime(new Date());
        taskMapper.updateById(task);
        appendLog(inst.getId(), task.getId(), task.getNodeKey(), operator,
            WfApprovalLog.LOG_APPROVE, opinion);

        // 2. 按节点审批方式判定是否推进引擎
        int signOrder = resolveSignOrder(inst.getDefId(), task.getNodeKey());
        if (signOrder == SIGN_ALL) {
            // 会签：仍有同节点待办则不推进
            if (countPending(inst.getId(), task.getNodeKey()) > 0) {
                log.info("[blade-workflow] 会签节点仍有待办，暂不推进. instId={}, nodeKey={}",
                    inst.getId(), task.getNodeKey());
                return true;
            }
        } else if (signOrder == SIGN_SEQUENCE) {
            // 依次：激活下一批次，未到最后一人则不推进
            WfTask next = nextPending(inst.getId(), task.getNodeKey());
            if (next != null) {
                next.setReceiveTime(new Date());
                taskMapper.updateById(next);
                log.info("[blade-workflow] 依次审批激活下一处理人. instId={}, nextTaskId={}",
                    inst.getId(), next.getId());
                return true;
            }
        } else {
            // 或签：办结同节点其余待办
            closeSiblings(inst.getId(), task.getNodeKey(), task.getId());
        }

        // 节点信息 → 运行时消费：子流程「全部归档才能提交」—— 本次提交前若本节点仍有未归档子流程，拒绝提交
        if (node != null && WfNodeSettingsUtil.subflowAllEndBeforeSubmit(node) == 1) {
            long pending = subflowRequestMapper.selectCount(Wrappers.<WfSubflowRequest>lambdaQuery()
                .eq(WfSubflowRequest::getMainInstId, inst.getId())
                .eq(WfSubflowRequest::getMainNodeKey, node.getNodeKey())
                .eq(WfSubflowRequest::getStatus, 0));
            if (pending > 0) {
                throw new ServiceException("该节点存在未归档的子流程（" + pending + " 条），全部归档后才能提交");
            }
        }

        // 2.5 提交变量（提前准备：正常提交与「退回发起人重新提交」两条路径都要用）
        Map<String, Object> vars = new HashMap<>(8);
        if (dto != null && dto.getVariables() != null) {
            vars.putAll(dto.getVariables());
        }

        // 2.6 「退回发起人」后的重新提交：发起人的待办是合成任务（无引擎任务），
        //     其提交 = 让流程重新从创建节点入流（重新生成第一个审批节点的待办），
        //     而不是 completeTask —— 引擎此刻正停在第一个审批节点上，complete 会跳过它直接往后走。
        if (isCreatorResubmitTask(task, node)) {
            resubmitByCreator(inst, node, operator, vars);
            // 节点信息 → 运行时消费：节点后附加操作 + 子流程触发（与正常提交同口径）
            // 测试态：跳过附加操作/子流程副作用（对齐 ecology istest，避免污染真实业务数据）
            if (inst.getIsTest() == null || inst.getIsTest() != 1) {
                nodeActionExecutor.execute(inst, node, NodeActionExecutor.PHASE_POST, operator);
                nodeActionExecutor.triggerSubflow(inst, node, NodeActionExecutor.TRIGGER_AFTER_SUBMIT, operator);
            }
            return true;
        }

        // 3. 推进引擎并同步后续任务
        // 节点信息 → 运行时消费：「指定流转」。开启后由处理人手动指定下一节点（模式1 可指定操作者）；
        // 模式3（多目标）：以节点上配置的 targets 为准，提交时并行扇出到多个目标节点（仅提交时生效，退回不适用）。
        int appointMode = WfNodeSettingsUtil.appointFlowMode(node);
        if (!system && appointMode != 0) {
            if (appointMode == 3) {
                // 多目标：取节点配置的 targets（{nodeKey, operatorIds[], signType}），并行跳转
                List<Map<String, Object>> targets = WfNodeSettingsUtil.appointFlowTargets(node);
                if (targets == null || targets.isEmpty()) {
                    throw new ServiceException("当前节点启用了「指定流转（多目标）」，但未配置目标节点");
                }
                List<String> toKeys = new ArrayList<>();
                Map<String, Long> overrideAssignees = new LinkedHashMap<>();
                Set<String> seen = new LinkedHashSet<>();
                for (Map<String, Object> tg : targets) {
                    String tk2 = tg == null ? null : String.valueOf(tg.get("nodeKey"));
                    if (tk2 == null || tk2.isBlank()) {
                        continue;
                    }
                    if (tk2.equals(task.getNodeKey())) {
                        throw new ServiceException("指定流转的目标节点不能是当前节点：" + tk2);
                    }
                    if (loadNode(inst.getDefId(), tk2) == null) {
                        throw new ServiceException("指定流转的目标节点不存在：" + tk2);
                    }
                    if (!seen.add(tk2)) {
                        continue; // 去重
                    }
                    toKeys.add(tk2);
                    Object ops = tg.get("operatorIds");
                    if (ops instanceof List && !((List<?>) ops).isEmpty()) {
                        Object first = ((List<?>) ops).get(0);
                        Long oid = parseId(first);
                        if (oid != null && oid > 0) {
                            overrideAssignees.put(tk2, oid);
                        }
                    }
                }
                if (toKeys.isEmpty()) {
                    throw new ServiceException("当前节点启用了「指定流转（多目标）」，但未配置有效目标节点");
                }
                processService.moveActivityToActivities(inst.getEngineInstId(), task.getNodeKey(), toKeys, vars);
                instanceService.advance(inst.getId(), operator, overrideAssignees, AdvanceSrc.SUBMIT);
            } else {
                String nextNodeKey = dto == null ? null : dto.getNextNodeKey();
                if (nextNodeKey == null || nextNodeKey.isBlank()) {
                    throw new ServiceException("当前节点启用了「指定流转」，请选择下一节点");
                }
                if (nextNodeKey.equals(task.getNodeKey())) {
                    throw new ServiceException("指定流转的目标节点不能是当前节点");
                }
                if (loadNode(inst.getDefId(), nextNodeKey) == null) {
                    throw new ServiceException("指定流转的目标节点不存在：" + nextNodeKey);
                }
                // 模式1：用户指定操作者；模式2：忽略用户操作者，按目标节点设置解析
                Long overrideAssignee = (appointMode == 1 && dto != null) ? dto.getNextAssignee() : null;
                processService.moveActivity(inst.getEngineInstId(), task.getNodeKey(), nextNodeKey, vars);
                instanceService.advance(inst.getId(), operator, nextNodeKey, overrideAssignee);
            }
        } else {
            processService.completeTask(task.getEngineTaskId(), vars);
            instanceService.advance(inst.getId(), operator);
        }

        // 节点信息 → 运行时消费：节点后附加操作 + 子流程触发（异常策略由 NodeActionExecutor 吸收）
        // 测试态：跳过附加操作/子流程副作用（对齐 ecology istest，避免污染真实业务数据）
        boolean testInst = inst.getIsTest() != null && inst.getIsTest() == 1;
        if (!testInst) {
            nodeActionExecutor.execute(inst, node, NodeActionExecutor.PHASE_POST, operator);
            nodeActionExecutor.triggerSubflow(inst, node, NodeActionExecutor.TRIGGER_AFTER_SUBMIT, operator);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(Long taskId, RejectDTO dto) {
        WfTask task = requireTodoTask(taskId);
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        // 记录级鉴权：只有该任务办理人本人（或流程管理员）能退回
        WfAuthUtil.requireOperateTask(task, "退回");
        WfProcessNode node = loadNode(inst.getDefId(), task.getNodeKey());
        // 节点信息 → 运行时消费：未勾选「退回」则不允许退回
        requireOperate(node, MENU_REJECT);
        // 主开关：节点表 allow_reject=0 时关闭退回（与 operateMenu 双重保险）
        if (node != null && node.getAllowReject() != null && node.getAllowReject() == 0) {
            throw new ServiceException("当前节点未开放退回");
        }

        // 计算可退回节点集合（反向回溯 + 白名单；已剔除引擎停不住的类型，见 WfRejectManager）
        List<WfProcessNode> candidates = rejectManager.computeRejectableNodes(
            inst.getDefId(), task.getNodeKey(), node);
        if (candidates.isEmpty()) {
            // 上游没有可停留的节点（如当前就是第一个审批节点、上游只有开始节点的情况本不该走到这里）。
            // 明确报错，避免调用方拿到 null 目标后又「移了个寂寞」。
            throw new ServiceException("当前节点没有可退回的节点：上游不存在可退的审批/提交节点");
        }
        int rejectType = WfNodeSettingsUtil.rejectType(node);

        // 解析目标节点
        String targetNodeKey;
        if (dto != null && dto.getTargetNodeKey() != null && !dto.getTargetNodeKey().isBlank()) {
            targetNodeKey = dto.getTargetNodeKey();
        } else {
            // 选择退回（rejectType=2）必须显式选节点；直接退回（1）退默认/上一节点
            if (rejectType == 2) {
                throw new ServiceException("该节点需选择退回节点");
            }
            targetNodeKey = rejectManager.resolveDefaultRejectNode(
                inst.getDefId(), task.getNodeKey(), node, candidates);
        }
        // 目标合法性：必须在可退回集合内（对齐 ecology DoRejectRequestCmd 的目标校验）
        if (!rejectManager.isRejectable(candidates, targetNodeKey)) {
            throw new ServiceException("退回目标节点不合法：" + targetNodeKey);
        }

        task.setStatus(WfTask.STATUS_DONE);
        task.setOperateTime(new Date());
        taskMapper.updateById(task);
        // 留痕记「节点接收人」而非当前登录人：代跑/测试态下当前登录人是管理员，记管理员会让
        // 流程信息看起来是「管理员退回了这张单」，与代跑留痕口径不一致（见方案 C14/C16）
        appendLog(inst.getId(), task.getId(), task.getNodeKey(), task.getAssignee(),
            WfApprovalLog.LOG_REJECT, dto == null ? null : dto.getOpinion());
        closeSiblings(inst.getId(), task.getNodeKey(), task.getId());

        // 引擎回退：把当前节点 token 移动到目标节点（保持实例运行，不终止）
        Map<String, Object> vars = new HashMap<>(4);
        WfProcessNode targetNode = loadNode(inst.getDefId(), targetNodeKey);
        if (targetNode != null && targetNode.getNodeType() != null && targetNode.getNodeType() == 0) {
            // 退回创建节点 = 退回发起人：创建节点在引擎里是 startEvent（非等待态），token 停不住，
            // 走专用路径（不能让引擎停在创建节点上，也不能给创建节点生成引擎任务）
            rejectToStarter(inst, task, targetNode, vars);
        } else {
            processService.moveActivity(inst.getEngineInstId(), task.getNodeKey(), targetNodeKey, vars);
            // 同步 wf_task / 当前节点：advance 读引擎当前活动任务，为目标节点生成待办
            // 来源标记 REJECT：退回链路的「流程异常处理」兜底不生效（对齐 ecology「退回忽略异常处理设置」）
            instanceService.advance(inst.getId(), task.getAssignee(), null, null, AdvanceSrc.REJECT);
        }

        // 节点信息 → 运行时消费：节点后附加操作（退回场景，仅执行勾选「退回时触发」的条目）
        // 测试态：跳过附加操作 —— 对齐 doApprove 的 PHASE_POST 跳过（:309-313），
        // 否则在测试实例上点退回会真的执行「写业务数据 / 调外部接口」类动作（方案 V14/C16）
        if (!isTestInst(inst)) {
            nodeActionExecutor.execute(inst, node, NodeActionExecutor.PHASE_POST, task.getAssignee(), true);
        }
        return true;
    }

    @Override
    public RejectCandidatesVO rejectNodes(Long taskId) {
        WfTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        // 记录级鉴权：只有该任务办理人本人（或流程管理员）能查询
        WfAuthUtil.requireOperateTask(task, "查询退回节点");
        WfProcessNode node = loadNode(inst.getDefId(), task.getNodeKey());
        if (node != null && node.getAllowReject() != null && node.getAllowReject() == 0) {
            throw new ServiceException("当前节点未开放退回");
        }
        List<WfProcessNode> candidates = rejectManager.computeRejectableNodes(
            inst.getDefId(), task.getNodeKey(), node);
        int rejectType = WfNodeSettingsUtil.rejectType(node);
        String defaultNodeKey = rejectManager.resolveDefaultRejectNode(
            inst.getDefId(), task.getNodeKey(), node, candidates);

        RejectCandidatesVO vo = new RejectCandidatesVO();
        vo.setType(rejectType);
        vo.setDefaultNodeKey(defaultNodeKey);
        vo.setRemind(WfNodeSettingsUtil.rejectRemind(node));
        vo.setChangeNode(WfNodeSettingsUtil.rejectChangeNode(node));
        List<RejectCandidateVO> nodes = new ArrayList<>();
        for (WfProcessNode n : candidates) {
            RejectCandidateVO c = new RejectCandidateVO();
            c.setNodeKey(n.getNodeKey());
            c.setNodeName(n.getNodeName());
            c.setNodeType(n.getNodeType());
            nodes.add(c);
        }
        vo.setNodes(nodes);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean forward(Long taskId, ForwardDTO dto) {
        WfTask task = requireTodoTask(taskId);
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        // 记录级鉴权：只有该任务办理人本人（或流程管理员）能把自己的待办转出去
        WfAuthUtil.requireOperateTask(task, "转办");
        // 测试态：转办会给「目标人」新建一条待办（且存在标记断层 V10），必须后端拒绝
        assertNotTestTask(task, "转办");
        if (dto == null || dto.getAssignee() == null) {
            throw new ServiceException("转办目标人不能为空");
        }
        // 节点信息 → 运行时消费：未勾选「转办」则不允许转办
        requireOperate(loadNode(inst.getDefId(), task.getNodeKey()), MENU_FORWARD);

        task.setStatus(WfTask.STATUS_DONE);
        task.setOperateTime(new Date());
        taskMapper.updateById(task);
        appendLog(inst.getId(), task.getId(), task.getNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_FORWARD, dto.getOpinion());

        // 为目标人生成同节点待办（沿用同一引擎任务）
        WfTask forwarded = new WfTask();
        forwarded.setInstId(inst.getId());
        forwarded.setEngineTaskId(task.getEngineTaskId());
        forwarded.setNodeKey(task.getNodeKey());
        forwarded.setAssignee(dto.getAssignee());
        forwarded.setOriginalUser(task.getAssignee());
        forwarded.setStatus(WfTask.STATUS_TODO);
        forwarded.setReceiveTime(new Date());
        // 标记继承：任务 is_test 必须随实例 —— 否则将来放开「测试域转办」时，
        // 新建的这条任务会是 is_test=0，任何 is_test 过滤都拦不住它（V10 / C2 标记断层）
        forwarded.setIsTest(inst.getIsTest() == null ? 0 : inst.getIsTest());
        taskMapper.insert(forwarded);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addSign(Long taskId, AddSignDTO dto) {
        WfTask task = requireTodoTask(taskId);
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        // 记录级鉴权：只有该任务办理人本人（或流程管理员）能加签
        WfAuthUtil.requireOperateTask(task, "加签");
        // 测试态：加签会给「加签人」新建一条待办（且存在标记断层 V10），必须后端拒绝
        assertNotTestTask(task, "加签");
        if (dto == null || dto.getAssignee() == null) {
            throw new ServiceException("加签人不能为空");
        }

        WfTask added = new WfTask();
        added.setInstId(inst.getId());
        added.setEngineTaskId(task.getEngineTaskId());
        added.setNodeKey(task.getNodeKey());
        added.setAssignee(dto.getAssignee());
        added.setSignOrder(dto.getAddSignType());
        added.setStatus(WfTask.STATUS_TODO);
        added.setReceiveTime(new Date());
        // 标记继承：同 forward（V10 / C2）
        added.setIsTest(inst.getIsTest() == null ? 0 : inst.getIsTest());
        taskMapper.insert(added);

        appendLog(inst.getId(), task.getId(), task.getNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_COMMENT,
            ("加签：" + (dto.getOpinion() == null ? "" : dto.getOpinion())));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean circulate(Long taskId, CirculateDTO dto) {
        WfTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        // 记录级鉴权：只有该任务办理人本人（或流程管理员）能发起抄送
        WfAuthUtil.requireOperateTask(task, "抄送");
        // 测试态：抄送会给真人建「已办」条目（status=8，V9），必须后端拒绝
        assertNotTestTask(task, "抄送");
        if (dto == null || dto.getAssignees() == null || dto.getAssignees().isEmpty()) {
            throw new ServiceException("抄送人不能为空");
        }
        for (Long assignee : dto.getAssignees()) {
            WfTask cc = new WfTask();
            cc.setInstId(inst.getId());
            cc.setEngineTaskId(task.getEngineTaskId());
            cc.setNodeKey(task.getNodeKey());
            cc.setAssignee(assignee);
            cc.setStatus(WfTask.STATUS_CIRCULATE);
            cc.setReceiveTime(new Date());
            // 标记继承：同 forward（V10 / C2）—— 抄送任务会进入被抄送人的「已办」（V9）
            cc.setIsTest(inst.getIsTest() == null ? 0 : inst.getIsTest());
            taskMapper.insert(cc);
        }
        appendLog(inst.getId(), task.getId(), task.getNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_CIRCULATE, dto.getOpinion());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean urge(Long taskId, UrgeDTO dto) {
        WfTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        // 记录级鉴权：催办限「发起人 / 该节点办理人 / 流程管理员」——
        // 发起人催审批人、同节点办理人之间互相催办都是常见诉求，无关人员无权干预该单
        if (!WfAuthUtil.isSelfOrAdmin(inst.getStarter()) && !WfAuthUtil.canOperateTask(task)) {
            throw new WfAccessDeniedException("无权催办：只有流程发起人、当前节点办理人或流程管理员可以催办");
        }
        // 测试态：催办会在「当前登录人」名下写留痕且无实际意义，后端拒绝
        assertNotTestTask(task, "催办");
        appendLog(inst.getId(), task.getId(), task.getNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, dto == null ? null : dto.getOpinion());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markViewed(Long taskId) {
        if (taskId == null) {
            return false;
        }
        WfTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        // 记录级鉴权：只有该任务办理人本人（或流程管理员）打开办理页才会写「已查看」
        WfAuthUtil.requireOperateTask(task, "标记查看");
        // 只记首次：已看过不改时间（流程图「已查看」只关心是否打开过）
        if (task.getViewTime() != null) {
            return true;
        }
        WfTask patch = new WfTask();
        patch.setId(taskId);
        patch.setViewTime(new Date());
        return taskMapper.updateById(patch) > 0;
    }

    @Override
    public Map<String, Long> count(Long assignee) {
        Map<String, Long> result = new HashMap<>(4);
        // 记录级鉴权：非流程管理员只能看自己的角标数（顶栏待办红点）
        Long target = WfAuthUtil.resolveSelfIfNotAdmin(assignee);
        // 测试态任务不计入角标（方案 §6.4 C1 / V2）：否则顶栏红点会把测试单算进去，
        // 把用户引去「办理」一条根本不该出现在生产面的单子
        Long todo = taskMapper.selectCount(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getAssignee, target)
            .eq(WfTask::getStatus, WfTask.STATUS_TODO)
            .eq(WfTask::getIsTest, 0));
        Long done = taskMapper.selectCount(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getAssignee, target)
            .ne(WfTask::getStatus, WfTask.STATUS_TODO)
            .eq(WfTask::getIsTest, 0));
        result.put("todo", todo == null ? 0L : todo);
        result.put("done", done == null ? 0L : done);
        return result;
    }

    // ------------------------------------------------------------------ 私有方法

    private List<WfTaskVO> list(Long assignee, List<Integer> statuses) {
        // 记录级鉴权：非流程管理员一律只能查自己的待办/已办，忽略传入的 assignee
        Long target = WfAuthUtil.resolveSelfIfNotAdmin(assignee);
        // 测试态任务不进生产「待办 / 已办」列表（方案 §6.4 C1 / V1、V3、C14）：
        // 含已办(2)/办结(3)/自动提交(4)/协办(7)/传阅(8)/已读(9) —— 整组排除，
        // 其中「已办」最易漏：代跑会在真人名下留一条已办，平时无人细看，漏了就长期存在。
        // 注：wf_task.is_test 为 tinyint NOT NULL DEFAULT 0，不存在 NULL 漏网。
        List<WfTask> tasks = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getAssignee, target)
            .in(WfTask::getStatus, statuses)
            .eq(WfTask::getIsTest, 0)
            .orderByDesc(WfTask::getCreateTime));
        List<WfTaskVO> result = new ArrayList<>(tasks.size());
        for (WfTask t : tasks) {
            result.add(toVO(t));
        }
        return result;
    }

    private WfTaskVO toVO(WfTask t) {
        WfTaskVO vo = new WfTaskVO();
        vo.setId(t.getId());
        vo.setInstId(t.getInstId());
        vo.setNodeKey(t.getNodeKey());
        vo.setAssignee(t.getAssignee());
        vo.setStatus(t.getStatus());
        vo.setReceiveTime(t.getReceiveTime());
        vo.setOperateTime(t.getOperateTime());
        vo.setDueTime(t.getDueTime());

        WfInstance inst = instanceMapper.selectById(t.getInstId());
        if (inst != null) {
            vo.setTitle(inst.getTitle());
            vo.setDefId(inst.getDefId());
            vo.setFormId(inst.getFormId());
            vo.setDataId(inst.getDataId());
            vo.setStarter(inst.getStarter());
            vo.setStartTime(inst.getStartTime());
            vo.setUrgency(inst.getUrgency());
            vo.setInstStatus(inst.getStatus());
            // ⚠️ 必须带 defId：bpmn-js 的 id（Activity_xxx 等）在不同流程间会重复，
            //    只按 nodeKey 查会命中别的流程的节点，待办/已办列表节点名串味。
            WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(WfProcessNode::getDefId, inst.getDefId())
                .eq(WfProcessNode::getNodeKey, t.getNodeKey())
                .last("LIMIT 1"));
            if (node != null) {
                vo.setNodeName(node.getNodeName());
            }
        }
        return vo;
    }

    // ------------------------------------------------------------------ 节点信息运行时消费

    /** 加载节点（拿不到返回 null，按「未配置」处理，不阻断流转） */
    private WfProcessNode loadNode(Long defId, String nodeKey) {
        if (defId == null || nodeKey == null) {
            return null;
        }
        return nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .eq(WfProcessNode::getNodeKey, nodeKey)
            .last("LIMIT 1"));
    }

    /**
     * 校验该节点的「操作菜单」是否允许某操作。
     * 未配置菜单（null）表示不限制，保持既有行为。
     */
    private void requireOperate(WfProcessNode node, String menu) {
        if (!WfNodeSettingsUtil.allowOperate(node, menu)) {
            throw new ServiceException("当前节点未开放该操作：" + menu);
        }
    }

    /**
     * 富文本意见的「空」判定：编辑器里的空内容是 {@code <p><br></p>} 这类标签，
     * 直接 trim 会判成非空，使「意见必填」形同虚设（前端同口径，见 isRichTextEmpty）。
     */
    private static boolean isBlankOpinion(String opinion) {
        if (opinion == null) {
            return true;
        }
        return opinion
            .replaceAll("(?i)<br\\s*/?>", "")
            .replace("&nbsp;", "")
            .replaceAll("<[^>]*>", "")
            .trim()
            .isEmpty();
    }

    /** 意见处理：必填校验 + 缺省套用「签字意见设置」的默认模板 */
    private String resolveOpinion(WfProcessNode node, String opinion) {
        boolean blank = isBlankOpinion(opinion);
        if (blank) {
            if (WfNodeSettingsUtil.opinionRequired(node)) {
                throw new ServiceException("该节点必须填写签字意见");
            }
            String tpl = WfNodeSettingsUtil.opinionTemplate(node);
            return tpl == null ? "" : tpl;
        }
        return opinion;
    }

    /**
     * 退回发起人（退回「创建节点」）。
     *
     * <p><b>为什么必须单独处理</b>：创建节点在 BPMN 里是 {@code startEvent}（对齐泛微「创建节点」＝
     * 发起人填单环节），<b>不是等待态</b> —— 直接把 token 移过去它不会停住，会立刻沿出口继续流出。
     * 实测（instId=2102013238909661186）：退到开始节点后经 sequenceFlow 又流回原审批节点，
     * 表现为「退回了但节点没变」。所以本路径刻意不让引擎停在创建节点上：</p>
     * <ol>
     *   <li>引擎侧：token 移到创建节点 → 顺其出口自然落到<b>第一个审批节点</b>并停住。
     *       这样引擎始终有活动任务，不会被 {@code advance} 判成「无活动任务 = 流程结束」而误归档；</li>
     *   <li>语义侧：当前节点记为创建节点，并给<b>发起人</b>生成一条合成待办
     *       （{@code engine_task_id} 留空 —— 与草稿待办同一套「合成任务」机制）；</li>
     *   <li>发起人改完表单提交时，{@link #doApprove} 走「退回后重新提交」分支：把 token 移回创建节点，
     *       引擎重新入流 → 第一个审批节点重新拿到待办，即「流程从第一个审批节点重新走」。</li>
     * </ol>
     *
     * <p>⚠️ 本路径<b>不能</b>调用 {@code instanceService.advance}：那会把当前节点改写成引擎落点
     * （第一个审批节点）并给它的操作者生成待办，等于没退回。</p>
     */
    private void rejectToStarter(WfInstance inst, WfTask task, WfProcessNode creatorNode,
                                 Map<String, Object> vars) {
        // ① 引擎：移 token 到创建节点，让它自然流出并停在第一个审批节点
        processService.moveActivity(inst.getEngineInstId(), task.getNodeKey(), creatorNode.getNodeKey(), vars);

        // 并行分支下的「退回发起人」：moveActivity 只移动本分支 token，其余分支 token 仍在。
        // 这种情形该不该作废其他分支属业务决策，此处仅告警不改动，避免误杀。
        List<TaskVO> active = processService.currentTasks(inst.getEngineInstId());
        if (active.size() > 1) {
            log.warn("[blade-workflow] 退回发起人时引擎仍有多条活动分支（{} 条），仅本分支回到发起人. instId={}",
                active.size(), inst.getId());
        }

        // ② 清掉可能残留的待办：此刻本实例已无合法待办（原节点任务已置已办/办结，发起人即将成为唯一处理人）。
        //    只删 TODO，保留已办/办结记录（「已办」列表要能看到这次退回的操作痕迹）。
        taskMapper.delete(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, inst.getId())
            .eq(WfTask::getStatus, WfTask.STATUS_TODO));

        // ③ 语义：当前节点回到创建节点 + 给发起人一条合成待办（engine_task_id 留空 = 非引擎任务）
        WfTask creatorTask = new WfTask();
        creatorTask.setInstId(inst.getId());
        creatorTask.setNodeKey(creatorNode.getNodeKey());
        creatorTask.setAssignee(inst.getStarter());
        creatorTask.setStatus(WfTask.STATUS_TODO);
        creatorTask.setReceiveTime(new Date());
        taskMapper.insert(creatorTask);

        WfInstance patch = new WfInstance();
        patch.setId(inst.getId());
        patch.setCurrentNodeKey(creatorNode.getNodeKey());
        instanceMapper.updateById(patch);

        log.info("[blade-workflow] 退回发起人. instId={}, from={}, creatorNode={}, starter={}",
            inst.getId(), task.getNodeKey(), creatorNode.getNodeKey(), inst.getStarter());
    }

    /**
     * 是否为「退回发起人」后发起人的合成待办。
     *
     * <p>判据：节点是创建节点（{@code node_type=0}）且没有引擎任务
     * （{@code engine_task_id} 为空 —— 合成任务标记，与草稿待办同一套机制）。</p>
     */
    private boolean isCreatorResubmitTask(WfTask task, WfProcessNode node) {
        if (node == null || node.getNodeType() == null || node.getNodeType() != 0) {
            return false;
        }
        String engineTaskId = task.getEngineTaskId();
        return engineTaskId == null || engineTaskId.isEmpty();
    }

    /**
     * 「退回发起人」后的重新提交：让流程重新从创建节点入流。
     *
     * <p>引擎此刻停在第一个审批节点上（{@link #rejectToStarter} 让 token 从创建节点自然流出后的落点），
     * 这里把 token 移回创建节点，引擎沿出口重新入流 → 生成第一个审批节点的新待办，
     * 再由 {@code advance} 同步到 wf_task / 当前节点（即「流程从第一个审批节点重新走」）。</p>
     */
    private void resubmitByCreator(WfInstance inst, WfProcessNode creatorNode, Long operator,
                                   Map<String, Object> vars) {
        List<TaskVO> parked = processService.currentTasks(inst.getEngineInstId());
        String parkedKey = parked.isEmpty() ? null : parked.get(0).getTaskDefinitionKey();
        if (parkedKey == null) {
            throw new ServiceException("流程引擎当前没有活动节点，无法重新提交（请确认该流程未被终止/归档）");
        }
        // 与退回同一机制：移回创建节点 → 引擎顺出口重新入流，停回第一个审批节点
        if (!creatorNode.getNodeKey().equals(parkedKey)) {
            processService.moveActivity(inst.getEngineInstId(), parkedKey, creatorNode.getNodeKey(), vars);
        }
        instanceService.advance(inst.getId(), operator);
        log.info("[blade-workflow] 退回发起人后重新提交. instId={}, creatorNode={}, operator={}",
            inst.getId(), creatorNode.getNodeKey(), operator);
    }

    /**
     * 实例是否为测试态（{@code wf_instance.is_test=1}）。
     *
     * <p>测试态实例与正式实例<b>同库同表</b>，仅靠 {@code is_test} 区分；任何会「外溢到生产」
     * 的写操作（给真人建任务、写生产可见留痕、触发外部接口）都必须先过这一层。</p>
     */
    private boolean isTestInst(WfInstance inst) {
        return inst != null && inst.getIsTest() != null && inst.getIsTest() == 1;
    }

    /** 任务所属实例是否为测试态（见 {@link #isTestInst}） */
    private boolean isTestTask(WfTask task) {
        if (task == null || task.getInstId() == null) {
            return false;
        }
        return isTestInst(instanceMapper.selectById(task.getInstId()));
    }

    /**
     * 测试态守卫：拒绝在测试实例上执行「会外溢到生产」的动作（方案 §6.4 C16）。
     *
     * <p><b>为什么必须在后端兜底</b>：前端已在测试面板禁用这些按钮（C6），但
     * 「前端禁用 ≠ 后端拒绝」——直接调接口照样能触发。而这几类动作的共同点是
     * <b>会向生产用户产生东西</b>：转办/加签给真人新建待办、传阅给真人建已办条目、
     * 催办在真人名下写留痕；且新建的任务还存在「标记断层」（V10/C2）。</p>
     *
     * <p>注：退回（{@link #reject}）不走本守卫——退回是流程测试需要验证的路径，
     * 改为「允许执行 + 跳过副作用 + 留痕用接收人」。</p>
     */
    private void assertNotTestTask(WfTask task, String action) {
        if (isTestTask(task)) {
            throw new ServiceException("测试流程不支持「" + action
                + "」：该操作会给真实用户产生任务或留痕，请在正式流程中使用");
        }
    }

    private WfTask requireTodoTask(Long taskId) {
        WfTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在（可能该流程已被删除），请刷新页面后重试");
        }
        if (!Integer.valueOf(WfTask.STATUS_TODO).equals(task.getStatus())) {
            throw new ServiceException(staleTaskReason(task));
        }
        return task;
    }

    /**
     * 任务已办结时的拒绝原因（「界面状态与实例状态不一致」规则在办理入口的口径）。
     *
     * <p>浏览器里已打开的办理页若未刷新，流程可能已被退回、被他人流转、被撤回或归档，
     * 此时本节点任务已办结。这里把笼统的「任务非待办状态」翻译成用户能据以行动的原因：
     * 说清流程现在停在哪，并提示刷新页面，避免用户看到「不可处理」后反复重试。</p>
     */
    private String staleTaskReason(WfTask task) {
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        if (inst == null) {
            return "本页已过期：该流程已不存在（可能已被删除），请刷新页面后重试";
        }
        Integer st = inst.getStatus();
        if (st != null && st == WfInstance.STATUS_APPROVED) {
            return "本页已过期：该流程已归档结束，无法再办理，请刷新页面查看最新结果";
        }
        if (st != null && st == WfInstance.STATUS_CANCELED) {
            return "本页已过期：该流程已被撤回/撤销，无法再办理，请刷新页面查看最新结果";
        }
        if (st != null && st == WfInstance.STATUS_REJECTED) {
            return "本页已过期：该流程已不通过结束，无法再办理，请刷新页面查看最新结果";
        }
        String cur = inst.getCurrentNodeKey();
        if (cur == null || cur.isEmpty()) {
            return "本页已过期：该任务已办结，流程已结束，请刷新页面查看最新结果";
        }
        return "本页已过期：该任务已办结，流程当前节点已变更为「" + nodeNameOf(inst.getDefId(), cur)
            + "」（可能已被退回或被他人流转），请刷新页面后重新办理";
    }

    /** 节点Key → 节点名称（查不到或异常时回退节点Key，本方法只用于提示语，绝不抛错） */
    private String nodeNameOf(Long defId, String nodeKey) {
        if (defId == null || nodeKey == null || nodeKey.isEmpty()) {
            return nodeKey;
        }
        try {
            WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(WfProcessNode::getDefId, defId)
                .eq(WfProcessNode::getNodeKey, nodeKey)
                .last("LIMIT 1"));
            return (node != null && node.getNodeName() != null && !node.getNodeName().isEmpty())
                ? node.getNodeName() : nodeKey;
        } catch (Exception e) {
            log.warn("[blade-workflow] 取节点名称失败，回退节点Key. defId={}, nodeKey={}", defId, nodeKey, e);
            return nodeKey;
        }
    }

    /**
     * 解析节点的签批方式（{@link #SIGN_ANY 或签} / {@link #SIGN_ALL 会签} / {@link #SIGN_SEQUENCE 依次}）。
     *
     * <p>⚠️ 必须同时看「操作组级会签属性」（{@code wf_node_operator.sign_order}）：
     * 「节点信息 → 操作者 → 添加操作组」里选的会签/依次**只写 wf_node_operator**，
     * 而节点级「审批方式」（{@code wf_process_node.sign_order}）往往仍是默认 0（或签）。
     * 若只读节点级，就会出现「配了会签，却一人通过即推进引擎/归档」。</p>
     *
     * <p>合并规则（两层取更严格者）：任一层为会签 → 会签；否则任一层为依次 → 依次；否则或签。</p>
     */
    private int resolveSignOrder(Long defId, String nodeKey) {
        WfProcessNode node = loadNode(defId, nodeKey);
        int nodeLevel = (node == null || node.getSignOrder() == null) ? SIGN_ANY : node.getSignOrder();
        boolean all = nodeLevel == SIGN_ALL;
        boolean sequence = nodeLevel == SIGN_SEQUENCE;
        if (node != null && node.getId() != null) {
            List<WfNodeOperator> ops = operatorMapper.selectList(Wrappers.<WfNodeOperator>lambdaQuery()
                .eq(WfNodeOperator::getNodeId, node.getId()));
            if (ops != null) {
                for (WfNodeOperator op : ops) {
                    Integer so = op.getSignOrder();
                    if (so == null) {
                        continue;
                    }
                    if (so == SIGN_ALL) {
                        all = true;
                    } else if (so == SIGN_SEQUENCE) {
                        sequence = true;
                    }
                }
            }
        }
        if (all) {
            return SIGN_ALL;
        }
        return sequence ? SIGN_SEQUENCE : SIGN_ANY;
    }

    private long countPending(Long instId, String nodeKey) {
        Long count = taskMapper.selectCount(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .eq(WfTask::getNodeKey, nodeKey)
            .eq(WfTask::getStatus, WfTask.STATUS_TODO));
        return count == null ? 0L : count;
    }

    private WfTask nextPending(Long instId, String nodeKey) {
        return taskMapper.selectOne(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .eq(WfTask::getNodeKey, nodeKey)
            .eq(WfTask::getStatus, WfTask.STATUS_TODO)
            .orderByAsc(WfTask::getId)
            .last("LIMIT 1"));
    }

    private void closeSiblings(Long instId, String nodeKey, Long excludeTaskId) {
        List<WfTask> siblings = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .eq(WfTask::getNodeKey, nodeKey)
            .eq(WfTask::getStatus, WfTask.STATUS_TODO)
            .ne(WfTask::getId, excludeTaskId));
        for (WfTask s : siblings) {
            s.setStatus(WfTask.STATUS_FINISHED);
            s.setOperateTime(new Date());
            taskMapper.updateById(s);
        }
    }

    private void appendLog(Long instId, Long taskId, String nodeKey, Long operator,
                           String logType, String opinion) {
        WfApprovalLog log = new WfApprovalLog();
        log.setInstId(instId);
        log.setTaskId(taskId);
        log.setNodeKey(nodeKey == null ? "" : nodeKey);
        log.setOperator(operator);
        log.setLogType(logType);
        log.setOpinion(opinion == null ? "" : opinion);
        log.setOperateTime(new Date());
        logMapper.insert(log);
    }

    // ------------------------------------------------------------------ 二次认证 + 字段校验

    /**
     * 节点信息 → 运行时消费：同意前校验「二次认证」与「节点字段校验」。失败即抛异常拒绝。
     */
    private void validateBeforeApprove(WfProcessNode node, WfInstance inst, Long operator, ApproveDTO dto, boolean system) {
        // 二次认证：需重新校验密码（系统自动通过跳过）
        if (!system && WfNodeSettingsUtil.secondAuth(node)) {
            String password = dto == null ? null : dto.getPassword();
            if (password == null || password.isEmpty()) {
                throw new ServiceException("该节点需要二次认证，请重新输入密码");
            }
            R<Boolean> r;
            try {
                r = userClient.verifyPassword(operator, password);
            } catch (Exception e) {
                throw new ServiceException("二次认证服务不可用，请稍后重试");
            }
            if (r == null || !r.isSuccess() || !Boolean.TRUE.equals(r.getData())) {
                throw new ServiceException("二次认证失败：密码错误");
            }
        }
        // 字段校验：按规则校验最新表单快照
        List<String> rules = WfNodeSettingsUtil.fieldCheckRules(node);
        if (!rules.isEmpty()) {
            Map<String, Object> data = loadLatestFormData(inst.getId());
            for (String rule : rules) {
                checkFieldRule(data, rule);
            }
        }
    }

    /** 取实例最新表单快照数据（无则空 map） */
    private Map<String, Object> loadLatestFormData(Long instId) {
        WfFormSnapshot snap = snapshotMapper.selectOne(Wrappers.<WfFormSnapshot>lambdaQuery()
            .eq(WfFormSnapshot::getInstId, instId)
            .orderByDesc(WfFormSnapshot::getId)
            .last("LIMIT 1"));
        if (snap == null || snap.getDataJson() == null || snap.getDataJson().isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> data = JsonUtil.parse(snap.getDataJson(), Map.class);
            return data == null ? Map.of() : data;
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** 校验单条规则：字段名:required / regex= / min= / max= */
    private void checkFieldRule(Map<String, Object> data, String rule) {
        int idx = rule.indexOf(':');
        if (idx <= 0) {
            return;
        }
        String field = rule.substring(0, idx).trim();
        String expr = rule.substring(idx + 1).trim();
        Object val = data.get(field);
        String sval = val == null ? null : String.valueOf(val);
        boolean blank = sval == null || sval.isEmpty();
        if ("required".equals(expr)) {
            if (blank) {
                throw new ServiceException("字段校验失败：" + field + " 不能为空");
            }
        } else if (expr.startsWith("regex=")) {
            String pattern = expr.substring("regex=".length());
            if (!blank && !sval.matches(pattern)) {
                throw new ServiceException("字段校验失败：" + field + " 格式不正确");
            }
        } else if (expr.startsWith("min=")) {
            if (!blank && toNumber(sval) < parseDouble(expr.substring("min=".length()))) {
                throw new ServiceException("字段校验失败：" + field + " 小于最小值");
            }
        } else if (expr.startsWith("max=")) {
            if (!blank && toNumber(sval) > parseDouble(expr.substring("max=".length()))) {
                throw new ServiceException("字段校验失败：" + field + " 大于最大值");
            }
        }
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0d;
        }
    }

    /** 把对象（字符串/数字）解析为 Long ID；非法返回 null */
    private static Long parseId(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Long) {
            return (Long) o;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static double toNumber(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0d;
        }
    }

}
