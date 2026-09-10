package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.jackson.JsonUtil;
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
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.vo.ApprovalLogVO;
import org.springblade.workflow.vo.InstanceVO;
import org.springblade.workflow.vo.TaskVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        WfInstance inst = new WfInstance();
        inst.setEngineInstId(engineInstId);
        inst.setDefId(def.getId());
        inst.setFormId(dto.getFormId());
        inst.setDataId(dto.getDataId());
        inst.setTitle((dto.getTitle() == null || dto.getTitle().isEmpty()) ? def.getName() : dto.getTitle());
        inst.setBizKey(bizKey);
        inst.setStarter(starter);
        inst.setStartTime(new Date());
        inst.setUrgency(dto.getUrgency() == null ? 0 : dto.getUrgency());
        inst.setStatus(WfInstance.STATUS_RUNNING);
        instanceMapper.insert(inst);

        String firstNodeKey = resolveFirstNodeKey(def.getId());

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
    public List<ApprovalLogVO> logs(Long instId) {
        List<WfApprovalLog> logs = logMapper.selectList(Wrappers.<WfApprovalLog>lambdaQuery()
            .eq(WfApprovalLog::getInstId, instId)
            .orderByAsc(WfApprovalLog::getOperateTime));
        List<ApprovalLogVO> result = new ArrayList<>(logs.size());
        for (WfApprovalLog l : logs) {
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
        List<TaskVO> engineTasks = processService.currentTasks(inst.getEngineInstId());

        // 引擎无活动任务 → 流程结束
        if (engineTasks.isEmpty()) {
            inst.setStatus(WfInstance.STATUS_APPROVED);
            inst.setEndTime(new Date());
            inst.setCurrentNodeKey("");
            instanceMapper.updateById(inst);
            return;
        }

        String nodeKey = engineTasks.get(0).getTaskDefinitionKey();
        if (!nodeKey.equals(inst.getCurrentNodeKey())) {
            inst.setCurrentNodeKey(nodeKey);
        }
        instanceMapper.updateById(inst);

        for (TaskVO t : engineTasks) {
            Long count = taskMapper.selectCount(Wrappers.<WfTask>lambdaQuery()
                .eq(WfTask::getInstId, instId)
                .eq(WfTask::getEngineTaskId, t.getTaskId()));
            if (count != null && count > 0) {
                continue;
            }
            WfTask task = new WfTask();
            task.setInstId(instId);
            task.setEngineTaskId(t.getTaskId());
            task.setNodeKey(t.getTaskDefinitionKey());
            task.setAssignee(parseAssignee(t.getAssignee()));
            task.setStatus(WfTask.STATUS_TODO);
            task.setReceiveTime(new Date());
            taskMapper.insert(task);
        }
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
