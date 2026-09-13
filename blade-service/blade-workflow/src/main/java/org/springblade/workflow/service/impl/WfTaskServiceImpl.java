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
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfApprovalLogMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.service.IWfTaskService;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.system.user.feign.IUserClient;
import org.springblade.workflow.entity.WfFormSnapshot;
import org.springblade.workflow.mapper.WfFormSnapshotMapper;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
import org.springblade.workflow.vo.WfTaskVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final WfApprovalLogMapper logMapper;
    private final IProcessService processService;
    private final IWfInstanceService instanceService;
    private final NodeActionExecutor nodeActionExecutor;
    private final IUserClient userClient;
    private final WfFormSnapshotMapper snapshotMapper;

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

        // 0. 节点信息 → 运行时消费：操作菜单校验 + 签字意见必填
        if (!system) {
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

        // 3. 推进引擎并同步后续任务
        Map<String, Object> vars = new HashMap<>(8);
        if (dto != null && dto.getVariables() != null) {
            vars.putAll(dto.getVariables());
        }
        // 节点信息 → 运行时消费：「指定流转」。开启后由处理人手动指定下一节点（模式1 可指定操作者）
        int appointMode = WfNodeSettingsUtil.appointFlowMode(node);
        if (!system && appointMode != 0) {
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
        } else {
            processService.completeTask(task.getEngineTaskId(), vars);
            instanceService.advance(inst.getId(), operator);
        }

        // 节点信息 → 运行时消费：节点后附加操作 + 子流程触发（异常策略由 NodeActionExecutor 吸收）
        nodeActionExecutor.execute(inst, node, NodeActionExecutor.PHASE_POST, operator);
        nodeActionExecutor.triggerSubflow(inst, node, NodeActionExecutor.TRIGGER_AFTER_SUBMIT, operator);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(Long taskId, RejectDTO dto) {
        WfTask task = requireTodoTask(taskId);
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        // 节点信息 → 运行时消费：未勾选「退回」则不允许退回
        requireOperate(loadNode(inst.getDefId(), task.getNodeKey()), MENU_REJECT);

        task.setStatus(WfTask.STATUS_DONE);
        task.setOperateTime(new Date());
        taskMapper.updateById(task);
        appendLog(inst.getId(), task.getId(), task.getNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_REJECT, dto == null ? null : dto.getOpinion());

        // 说明：退回至指定节点需在 BPMN 中建模退回线（wf_node_link.is_reject），
        // 属 P5 高级特性；当前内核阶段退回应终止实例并置为「不通过」。
        inst.setStatus(WfInstance.STATUS_REJECTED);
        inst.setEndTime(new Date());
        instanceMapper.updateById(inst);
        closeSiblings(inst.getId(), task.getNodeKey(), task.getId());
        // 节点信息 → 运行时消费：节点后附加操作（退回场景，仅执行勾选「退回时触发」的条目）
        nodeActionExecutor.execute(inst, loadNode(inst.getDefId(), task.getNodeKey()),
            NodeActionExecutor.PHASE_POST, SecureUtil.getUserId(), true);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean forward(Long taskId, ForwardDTO dto) {
        WfTask task = requireTodoTask(taskId);
        WfInstance inst = instanceMapper.selectById(task.getInstId());
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
        taskMapper.insert(forwarded);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addSign(Long taskId, AddSignDTO dto) {
        WfTask task = requireTodoTask(taskId);
        WfInstance inst = instanceMapper.selectById(task.getInstId());
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
        appendLog(inst.getId(), task.getId(), task.getNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, dto == null ? null : dto.getOpinion());
        return true;
    }

    @Override
    public Map<String, Long> count(Long assignee) {
        Map<String, Long> result = new HashMap<>(4);
        Long todo = taskMapper.selectCount(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getAssignee, assignee)
            .eq(WfTask::getStatus, WfTask.STATUS_TODO));
        Long done = taskMapper.selectCount(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getAssignee, assignee)
            .ne(WfTask::getStatus, WfTask.STATUS_TODO));
        result.put("todo", todo == null ? 0L : todo);
        result.put("done", done == null ? 0L : done);
        return result;
    }

    // ------------------------------------------------------------------ 私有方法

    private List<WfTaskVO> list(Long assignee, List<Integer> statuses) {
        List<WfTask> tasks = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getAssignee, assignee)
            .in(WfTask::getStatus, statuses)
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
            vo.setFormId(inst.getFormId());
            vo.setDataId(inst.getDataId());
            vo.setStarter(inst.getStarter());
            vo.setStartTime(inst.getStartTime());
            vo.setUrgency(inst.getUrgency());
            WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
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

    /** 意见处理：必填校验 + 缺省套用「签字意见设置」的默认模板 */
    private String resolveOpinion(WfProcessNode node, String opinion) {
        boolean blank = opinion == null || opinion.trim().isEmpty();
        if (blank) {
            if (WfNodeSettingsUtil.opinionRequired(node)) {
                throw new ServiceException("该节点必须填写签字意见");
            }
            String tpl = WfNodeSettingsUtil.opinionTemplate(node);
            return tpl == null ? "" : tpl;
        }
        return opinion;
    }

    private WfTask requireTodoTask(Long taskId) {
        WfTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        if (!Integer.valueOf(WfTask.STATUS_TODO).equals(task.getStatus())) {
            throw new ServiceException("任务非待办状态，不可处理");
        }
        return task;
    }

    private int resolveSignOrder(Long defId, String nodeKey) {
        WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .eq(WfProcessNode::getNodeKey, nodeKey)
            .last("LIMIT 1"));
        return (node == null || node.getSignOrder() == null) ? SIGN_ANY : node.getSignOrder();
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

    private static double toNumber(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0d;
        }
    }

}
