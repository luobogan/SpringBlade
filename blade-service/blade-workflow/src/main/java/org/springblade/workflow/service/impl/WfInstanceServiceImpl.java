package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.workflow.action.NodeActionExecutor;
import org.springblade.workflow.dto.StartProcessDTO;
import org.springblade.workflow.entity.WfApprovalLog;
import org.springblade.workflow.entity.WfFormSnapshot;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfApprovalLogMapper;
import org.springblade.workflow.mapper.WfFormSnapshotMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.resolver.WfOperatorResolver;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.vo.ApprovalLogVO;
import org.springblade.workflow.vo.InstanceVO;
import org.springblade.workflow.vo.TaskVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程实例语义服务实现
 *
 * <p>wf_instance / wf_form_snapshot / wf_approval_log 的写入均在此完成；
 * 引擎侧一律通过 {@link IProcessService} 调用，保证语义层与引擎解耦。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfInstanceServiceImpl implements IWfInstanceService {

    private final WfInstanceMapper instanceMapper;
    private final WfTaskMapper taskMapper;
    private final WfApprovalLogMapper logMapper;
    private final WfFormSnapshotMapper snapshotMapper;
    private final WfProcessDefinitionMapper defMapper;
    private final WfProcessNodeMapper nodeMapper;
    private final IProcessService processService;
    private final WfOperatorResolver operatorResolver;
    private final NodeActionExecutor nodeActionExecutor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long start(StartProcessDTO dto) {
        if (dto == null) {
            throw new ServiceException("发起流程参数不能为空");
        }
        WfProcessDefinition def = resolveDefinition(dto);
        String bizKey = buildBizKey(dto.getFormId(), dto.getDataId());

        Map<String, Object> vars = new HashMap<>(16);
        if (dto.getFieldValues() != null) {
            vars.putAll(dto.getFieldValues());
        }
        if (dto.getVariables() != null) {
            vars.putAll(dto.getVariables());
        }

        Long starter = (dto.getStarter() != null) ? dto.getStarter() : SecureUtil.getUserId();
        String engineInstId = processService.startInstance(def.getProcKey(), bizKey, vars);

        // 首节点先算出来：实例标题模板取自首节点的「标题显示设置」
        String firstNodeKey = resolveFirstNodeKey(def.getId());

        WfInstance inst = new WfInstance();
        inst.setEngineInstId(engineInstId);
        inst.setDefId(def.getId());
        inst.setFormId(dto.getFormId());
        inst.setDataId(dto.getDataId());
        inst.setTitle(resolveTitle(dto.getTitle(), def.getName(), def.getId(), firstNodeKey, starter));
        inst.setBizKey(bizKey);
        inst.setStarter(starter);
        inst.setParentId(dto.getParentId());
        inst.setStartTime(new Date());
        inst.setUrgency(dto.getUrgency() == null ? 0 : dto.getUrgency());
        // 测试态标记：测试产生的实例打 is_test=1，并记下临时部署ID，便于一键清理且不污染正常数据
        boolean test = Boolean.TRUE.equals(dto.getTestFlag());
        inst.setIsTest(test ? 1 : 0);
        inst.setTestDeploymentId(dto.getTestDeploymentId());
        inst.setStatus(WfInstance.STATUS_RUNNING);
        instanceMapper.insert(inst);

        // 表单数据快照（决策 3：数据与布局解耦）
        WfFormSnapshot snap = new WfFormSnapshot();
        snap.setInstId(inst.getId());
        snap.setNodeKey(firstNodeKey);
        snap.setDataJson(JsonUtil.toJson(dto.getFieldValues() == null ? Map.of() : dto.getFieldValues()));
        snapshotMapper.insert(snap);

        appendLog(inst.getId(), null, firstNodeKey, starter, WfApprovalLog.LOG_SUBMIT, "");

        advance(inst.getId());
        log.info("[blade-workflow] 发起流程成功. instId={}, defId={}, bizKey={}", inst.getId(), def.getId(), bizKey);
        return inst.getId();
    }

    @Override
    public InstanceVO detail(Long id) {
        WfInstance inst = instanceMapper.selectById(id);
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        return toInstanceVO(inst);
    }

    @Override
    public InstanceVO getByBiz(Long formId, Long dataId) {
        WfInstance inst = instanceMapper.selectOne(Wrappers.<WfInstance>lambdaQuery()
            .eq(WfInstance::getBizKey, buildBizKey(formId, dataId))
            .last("LIMIT 1"));
        return inst == null ? null : toInstanceVO(inst);
    }

    @Override
    public int countByForm(Long formId) {
        if (formId == null) {
            return 0;
        }
        Long count = instanceMapper.selectCount(Wrappers.<WfInstance>lambdaQuery()
            .eq(WfInstance::getFormId, formId));
        return count == null ? 0 : count.intValue();
    }

    @Override
    public List<ApprovalLogVO> logs(Long instId) {
        List<WfApprovalLog> logs = logMapper.selectList(Wrappers.<WfApprovalLog>lambdaQuery()
            .eq(WfApprovalLog::getInstId, instId)
            .orderByAsc(WfApprovalLog::getOperateTime));

        // 节点信息 → 运行时消费：按「当前节点」的「表单日志查看范围」过滤可见节点的日志。
        // null = 不限制（保持既有行为）。
        WfInstance inst = instanceMapper.selectById(instId);
        WfProcessNode curNode = (inst == null || inst.getCurrentNodeKey() == null) ? null
            : nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(WfProcessNode::getDefId, inst.getDefId())
                .eq(WfProcessNode::getNodeKey, inst.getCurrentNodeKey())
                .last("LIMIT 1"));
        List<String> visibleNodeKeys = WfNodeSettingsUtil.formLogVisibleNodeKeys(curNode);

        List<ApprovalLogVO> result = new ArrayList<>(logs.size());
        for (WfApprovalLog l : logs) {
            if (visibleNodeKeys != null && !visibleNodeKeys.contains(l.getNodeKey())) {
                continue;
            }
            ApprovalLogVO vo = new ApprovalLogVO();
            vo.setId(l.getId());
            vo.setNodeKey(l.getNodeKey());
            vo.setNodeName(resolveNodeName(l.getNodeKey()));
            vo.setOperator(l.getOperator());
            vo.setLogType(l.getLogType());
            vo.setOpinion(l.getOpinion());
            vo.setOperateTime(l.getOperateTime());
            result.add(vo);
        }
        return result;
    }

    @Override
    public String snapshot(Long instId, String nodeKey) {
        WfFormSnapshot snap = snapshotMapper.selectOne(Wrappers.<WfFormSnapshot>lambdaQuery()
            .eq(WfFormSnapshot::getInstId, instId)
            .eq(WfFormSnapshot::getNodeKey, nodeKey)
            .orderByDesc(WfFormSnapshot::getCreateTime)
            .last("LIMIT 1"));
        return snap == null ? null : snap.getDataJson();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdraw(Long instId, String opinion) {
        return terminate(instId, WfInstance.STATUS_CANCELED, opinion, "撤回");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean stop(Long instId) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        inst.setStatus(WfInstance.STATUS_SUSPENDED);
        instanceMapper.updateById(inst);
        appendLog(instId, null, inst.getCurrentNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, "暂停流程");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resume(Long instId) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        inst.setStatus(WfInstance.STATUS_RUNNING);
        instanceMapper.updateById(inst);
        appendLog(instId, null, inst.getCurrentNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, "恢复流程");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long instId, String opinion) {
        return terminate(instId, WfInstance.STATUS_CANCELED, opinion, "撤销");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advance(Long instId) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            return;
        }
        advance(instId, inst.getStarter());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advance(Long instId, Long currentOperator) {
        advance(instId, currentOperator, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advance(Long instId, Long currentOperator, String overrideNodeKey, Long overrideAssignee) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            return;
        }
        List<TaskVO> engineTasks = processService.currentTasks(inst.getEngineInstId());

        // 引擎无活动任务 → 流程结束
        if (engineTasks.isEmpty()) {
            String lastNodeKey = inst.getCurrentNodeKey();
            inst.setStatus(WfInstance.STATUS_APPROVED);
            inst.setEndTime(new Date());
            inst.setCurrentNodeKey("");
            instanceMapper.updateById(inst);
            // 节点信息 → 运行时消费：归档后子流程触发（settings.subflow.trigger=afterArchive）
            // 测试态：跳过附加操作/子流程副作用，避免污染真实业务数据（对齐 ecology istest）
            if (inst.getIsTest() == null || inst.getIsTest() != 1) {
                if (lastNodeKey != null && !lastNodeKey.isEmpty()) {
                    nodeActionExecutor.triggerSubflow(inst, loadNode(inst.getDefId(), lastNodeKey),
                        NodeActionExecutor.TRIGGER_AFTER_ARCHIVE, inst.getStarter());
                }
            }
            return;
        }

        String nodeKey = engineTasks.get(0).getTaskDefinitionKey();
        boolean nodeChanged = !nodeKey.equals(inst.getCurrentNodeKey());
        if (nodeChanged) {
            inst.setCurrentNodeKey(nodeKey);
        }
        instanceMapper.updateById(inst);
        if (nodeChanged && (inst.getIsTest() == null || inst.getIsTest() != 1)) {
            // 新节点激活 → 执行「节点前附加操作」（受「流程异常处理」策略保护）
            // 测试态：跳过附加操作副作用
            nodeActionExecutor.execute(inst, loadNode(inst.getDefId(), nodeKey),
                NodeActionExecutor.PHASE_PRE, inst.getStarter());
        }

        for (TaskVO t : engineTasks) {
            String tk = t.getTaskDefinitionKey();
            // ① 优先按「节点操作者」展开：一个节点可生成多条待办（或签/会签/依次由 WfTaskServiceImpl 推进门禁控制）
            // ⓪ 「指定流转」：用户手工指定的下一节点，其操作者以用户选择为准（模式1）
            if (overrideNodeKey != null && overrideNodeKey.equals(tk) && overrideAssignee != null) {
                if (!existsTask(instId, t.getTaskId(), overrideAssignee)) {
                    insertTask(inst, t, overrideAssignee, tk);
                }
                continue;
            }
            // ① 优先按「节点操作者」展开：一个节点可生成多条待办（或签/会签/依次由 WfTaskServiceImpl 推进门禁控制）
            List<Long> assignees = operatorResolver.resolve(inst.getDefId(), tk, instId, inst.getStarter(), currentOperator);
            if (assignees.isEmpty()) {
                // ② 未配置操作者或类型无法解析 → 回退原有行为（沿用引擎 assignee 单条待办）
                if (existsTask(instId, t.getTaskId(), null)) {
                    continue;
                }
                insertTask(inst, t, parseAssignee(t.getAssignee()), tk);
                continue;
            }
            for (Long uid : assignees) {
                // 幂等键改为 (engineTaskId, assignee)，否则同节点第二人会被 engineTaskId 去重掉
                if (existsTask(instId, t.getTaskId(), uid)) {
                    continue;
                }
                insertTask(inst, t, uid, tk);
            }
        }
    }

    /** 待办是否已存在（assignee 为 null 时只按引擎任务判重） */
    private boolean existsTask(Long instId, String engineTaskId, Long assignee) {
        LambdaQueryWrapper<WfTask> q = Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .eq(WfTask::getEngineTaskId, engineTaskId);
        if (assignee != null) {
            q.eq(WfTask::getAssignee, assignee);
        }
        Long count = taskMapper.selectCount(q);
        return count != null && count > 0;
    }

    /** 生成一条待办：顺带按节点「超时设置」写入 dueTime（供超时任务扫描） */
    private void insertTask(WfInstance inst, TaskVO t, Long assignee, String nodeKey) {
        WfTask task = new WfTask();
        task.setInstId(inst.getId());
        task.setEngineTaskId(t.getTaskId());
        task.setNodeKey(nodeKey);
        task.setAssignee(assignee);
        task.setIsTest(inst.getIsTest() == null ? 0 : inst.getIsTest());
        task.setStatus(WfTask.STATUS_TODO);
        task.setReceiveTime(new Date());
        Date due = resolveDueTime(inst.getDefId(), nodeKey, task.getReceiveTime());
        if (due != null) {
            task.setDueTime(due);
        }
        taskMapper.insert(task);
    }

    /**
     * 实例标题：调用方指定则优先；否则套用首节点「标题显示设置」的模板；模板缺失则回退流程名。
     *
     * <p>支持占位符：{@code {流程名称}} / {@code {创建人}} / {@code {发起人}} / {@code {日期}}。
     * ⚠️ 本模块无用户中心 Feign，{@code {创建人}} 只能落到用户ID（待接入用户中心后可换成姓名）。</p>
     */
    private String resolveTitle(String given, String defName, Long defId, String firstNodeKey, Long starter) {
        if (given != null && !given.isEmpty()) {
            return given;
        }
        WfProcessNode node = (defId == null || firstNodeKey == null) ? null
            : nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(WfProcessNode::getDefId, defId)
                .eq(WfProcessNode::getNodeKey, firstNodeKey)
                .last("LIMIT 1"));
        String tpl = WfNodeSettingsUtil.titleTemplate(node);
        if (tpl == null || tpl.isBlank()) {
            return defName;
        }
        String starterText = starter == null ? "" : String.valueOf(starter);
        String title = tpl
            .replace("{流程名称}", defName == null ? "" : defName)
            .replace("{创建人}", starterText)
            .replace("{发起人}", starterText)
            .replace("{日期}", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        return title.isBlank() ? defName : title;
    }

    /** 按节点 settings.timeout.hours 计算截止时间；未配置返回 null */
    private Date resolveDueTime(Long defId, String nodeKey, Date from) {
        int hours = WfNodeSettingsUtil.timeoutHours(loadNode(defId, nodeKey));
        if (hours <= 0) {
            return null;
        }
        return new Date(from.getTime() + hours * 60L * 60L * 1000L);
    }

    /** 按 defId + nodeKey 取节点（取不到返回 null） */
    private WfProcessNode loadNode(Long defId, String nodeKey) {
        if (defId == null || nodeKey == null) {
            return null;
        }
        return nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .eq(WfProcessNode::getNodeKey, nodeKey)
            .last("LIMIT 1"));
    }

    // ------------------------------------------------------------------ 私有方法

    private boolean terminate(Long instId, int status, String opinion, String action) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        inst.setStatus(status);
        inst.setEndTime(new Date());
        instanceMapper.updateById(inst);

        // 关闭所有未完成任务
        List<WfTask> tasks = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .eq(WfTask::getStatus, WfTask.STATUS_TODO));
        for (WfTask t : tasks) {
            t.setStatus(WfTask.STATUS_FINISHED);
            t.setOperateTime(new Date());
            taskMapper.updateById(t);
        }
        appendLog(instId, null, inst.getCurrentNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, action + "：" + (opinion == null ? "" : opinion));
        return true;
    }

    private WfProcessDefinition resolveDefinition(StartProcessDTO dto) {
        if (dto.getDefId() != null) {
            WfProcessDefinition def = defMapper.selectById(dto.getDefId());
            if (def != null) {
                return def;
            }
        }
        if (dto.getProcKey() == null || dto.getProcKey().isEmpty()) {
            throw new ServiceException("流程定义Key(procKey)与定义ID(defId)不能同时为空");
        }
        WfProcessDefinition def = defMapper.selectOne(Wrappers.<WfProcessDefinition>lambdaQuery()
            .eq(WfProcessDefinition::getProcKey, dto.getProcKey())
            .eq(WfProcessDefinition::getStatus, 1)
            .orderByDesc(WfProcessDefinition::getVersion)
            .last("LIMIT 1"));
        if (def == null) {
            throw new ServiceException("未找到已发布的流程定义: " + dto.getProcKey());
        }
        return def;
    }

    private String resolveFirstNodeKey(Long defId) {
        WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .orderByAsc(WfProcessNode::getSortOrder)
            .last("LIMIT 1"));
        return node == null ? "" : node.getNodeKey();
    }

    private String resolveNodeName(String nodeKey) {
        if (nodeKey == null || nodeKey.isEmpty()) {
            return "";
        }
        WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getNodeKey, nodeKey)
            .last("LIMIT 1"));
        return node == null ? nodeKey : node.getNodeName();
    }

    private InstanceVO toInstanceVO(WfInstance inst) {
        InstanceVO vo = new InstanceVO();
        vo.setId(inst.getId());
        vo.setDefId(inst.getDefId());
        vo.setFormId(inst.getFormId());
        vo.setDataId(inst.getDataId());
        vo.setTitle(inst.getTitle());
        vo.setBizKey(inst.getBizKey());
        vo.setStatus(inst.getStatus());
        vo.setCurrentNodeKey(inst.getCurrentNodeKey());
        vo.setCurrentNodeName(resolveNodeName(inst.getCurrentNodeKey()));
        vo.setStarter(inst.getStarter());
        vo.setStartTime(inst.getStartTime());
        vo.setEndTime(inst.getEndTime());
        vo.setUrgency(inst.getUrgency());
        WfProcessDefinition def = defMapper.selectById(inst.getDefId());
        if (def != null) {
            vo.setDefName(def.getName());
        }
        return vo;
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

    private static Long parseAssignee(String assignee) {
        if (assignee == null || assignee.isEmpty()) {
            return 0L;
        }
        try {
            return Long.valueOf(assignee);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String buildBizKey(Long formId, Long dataId) {
        return formId + ":" + dataId;
    }

}
