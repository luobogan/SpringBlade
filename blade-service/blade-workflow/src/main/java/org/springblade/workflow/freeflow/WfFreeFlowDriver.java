package org.springblade.workflow.freeflow;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.workflow.dto.ApproveDTO;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.entity.WfFreeNode;
import org.springblade.workflow.mapper.WfFreeNodeMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.IWfInstanceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自由流程推进器。
 *
 * <p>采用「静态节点池绑定 + 实例级步骤编排 + 影子节点装饰」方案：不启用真免模型引擎，
 * 仍由 Flowable 持有运行态（避免 {@code instance-read-source=act} 下实例隐形），
 * 但每一步的显示名 / 办理人 / 会签方式 / 角色标记来自 {@code wf_free_node}。</p>
 *
 * <p>推进复用既有能力：中间步 {@code moveActivity} 池内迁移 + {@code advance} 生成待办；
 * 末步 {@code completeTask} 走 EndEvent，由既有 {@code advanceInternal} 的空任务分支归档，
 * 保证归档日志、终态双写、子流程回调等既有语义不丢失。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfFreeFlowDriver implements IWfFreeFlowDriver {

    private final WfFreeFlowManager manager;
    private final WfFreeNodeResolver resolver;
    private final WfFreeNodeMapper freeNodeMapper;
    private final WfProcessDefinitionMapper defMapper;
    private final IProcessService processService;
    private final IWfInstanceService instanceService;
    private final WfTaskMapper taskMapper;

    /** 特性开关：默认关闭，isFree=0 的流程零行为变化，可一键回退 */
    @Value("${blade.workflow.free-flow.enabled:false}")
    private boolean enabled;

    @Override
    public boolean isEnabled(WfInstance inst) {
        if (!enabled || inst == null || inst.getDefId() == null) {
            return false;
        }
        // 测试态（/formmode/test）走定义态自动推进链路，不进入自由流程分支，
        // 否则测试实例没有编排步骤会直接抛错，且静态节点覆盖率口径会失真。
        if (inst.getIsTest() != null && inst.getIsTest() == 1) {
            return false;
        }
        WfProcessDefinition def = defMapper.selectById(inst.getDefId());
        return def != null && def.getIsFree() != null && def.getIsFree() == 1;
    }

    @Override
    public WfFreeNodeContext context(WfInstance inst) {
        return isEnabled(inst)
            ? WfFreeNodeContext.load(freeNodeMapper, inst.getId())
            : WfFreeNodeContext.empty();
    }

    @Override
    public FreeAdvanceResult submit(WfInstance inst, WfTask task, ApproveDTO dto, Long operator,
                                    Map<String, Object> vars, WfFreeNodeContext context) {
        if (context == null || context.isEmpty()) {
            throw new ServiceException("当前流程未设置自由流程步骤，请先在「流转设定」中编排步骤");
        }
        WfFreeNode current = context.byNodeKey(task.getNodeKey());
        if (current == null) {
            throw new ServiceException("当前节点未关联自由流程步骤，请重新设置流转");
        }
        WfFreeNode next = context.nextOf(current);
        List<Long> assignees = next == null ? Collections.emptyList() : resolveAssignees(dto, next);
        if (next != null && assignees.isEmpty()) {
            throw new ServiceException("下一步「" + next.getNodeName() + "」未指定办理人");
        }

        current.setNodeStatus(1);
        freeNodeMapper.updateById(current);

        Map<String, Object> flowVars = vars == null ? new LinkedHashMap<>() : vars;
        if (next == null) {
            // 末步：completeTask 走 EndEvent → currentTasks 为空 → 既有 advanceInternal 归档
            processService.completeTask(task.getEngineTaskId(), flowVars);
            instanceService.advance(inst.getId(), operator);
            log.info("[free-flow] 末步提交，流程归档. instId={}, nodeKey={}", inst.getId(), task.getNodeKey());
            return FreeAdvanceResult.finished();
        }

        // 多人集合变量：MI 节点按集合展开；非 MI 节点由 appendExtraAssignees 生成并列待办
        flowVars.put("wfMiAssignees_" + next.getNodeKey(), new ArrayList<>(assignees));
        processService.moveActivity(inst.getEngineInstId(), task.getNodeKey(), next.getNodeKey(), flowVars);
        instanceService.advance(inst.getId(), operator, next.getNodeKey(), assignees.get(0));
        appendExtraAssignees(inst, next, assignees);
        // 首人待办由 advance 生成、ext_json 为空；统一补写，避免「首人静态名、他人自定义名」
        stampTaskExtJson(inst, next);
        log.info("[free-flow] 步骤推进. instId={}, from={}, to={}, assignees={}",
            inst.getId(), task.getNodeKey(), next.getNodeKey(), assignees.size());
        return FreeAdvanceResult.advanced(next.getNodeKey(), assignees.size());
    }

    @Override
    public void onReject(Long instId, String targetNodeKey) {
        // 退回后把目标步骤及之后的步骤重新置为「未流转」，
        // 否则重新提交仍按 flowOrder 一路前进，退回语义静默失效。
        manager.resetFrom(instId, targetNodeKey);
    }

    /** 办理人优先级：本次提交自选 > 步骤编排预设 */
    private List<Long> resolveAssignees(ApproveDTO dto, WfFreeNode next) {
        if (dto != null && dto.getNextAssigneeIds() != null && !dto.getNextAssigneeIds().isEmpty()) {
            return new ArrayList<>(dto.getNextAssigneeIds());
        }
        if (dto != null && dto.getNextAssignee() != null) {
            List<Long> one = new ArrayList<>(1);
            one.add(dto.getNextAssignee());
            return one;
        }
        return new ArrayList<>(WfFreeFlowManager.parseIds(next.getOperatorIds()));
    }

    /**
     * 非引擎多实例模式下，为首人以外的办理人补并列待办。
     * 与 addSign/forward/circulate 同范式：复用同一 engineTaskId、只造 wf_task 行。
     */
    private void appendExtraAssignees(WfInstance inst, WfFreeNode next, List<Long> assignees) {
        if (assignees.size() <= 1) {
            return;
        }
        WfTask sample = taskMapper.selectOne(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, inst.getId())
            .eq(WfTask::getNodeKey, next.getNodeKey())
            .eq(WfTask::getStatus, WfTask.STATUS_TODO)
            .last("LIMIT 1"));
        if (sample == null) {
            log.warn("[free-flow] 未找到首人待办，跳过并列待办补齐. instId={}, nodeKey={}",
                inst.getId(), next.getNodeKey());
            return;
        }
        for (int i = 1; i < assignees.size(); i++) {
            Long uid = assignees.get(i);
            Long exists = taskMapper.selectCount(Wrappers.<WfTask>lambdaQuery()
                .eq(WfTask::getInstId, inst.getId())
                .eq(WfTask::getNodeKey, next.getNodeKey())
                .eq(WfTask::getAssignee, uid)
                .eq(WfTask::getStatus, WfTask.STATUS_TODO));
            if (exists != null && exists > 0) {
                continue;
            }
            WfTask t = new WfTask();
            t.setInstId(inst.getId());
            t.setEngineTaskId(sample.getEngineTaskId());
            t.setNodeKey(next.getNodeKey());
            t.setAssignee(uid);
            t.setSignOrder(next.getSignType());
            t.setStatus(WfTask.STATUS_TODO);
            t.setReceiveTime(new Date());
            t.setIsTest(inst.getIsTest());
            taskMapper.insert(t);
        }
    }

    /** 给该步骤下的所有待办统一写展示上下文（含首人，避免名单不一致） */
    private void stampTaskExtJson(WfInstance inst, WfFreeNode next) {
        Map<String, Object> ext = new LinkedHashMap<>(2);
        ext.put("nodeName", next.getNodeName());
        ext.put("flowOrder", next.getFlowOrder());
        String json = JsonUtil.toJson(ext);
        List<WfTask> rows = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, inst.getId())
            .eq(WfTask::getNodeKey, next.getNodeKey())
            .eq(WfTask::getStatus, WfTask.STATUS_TODO));
        for (WfTask r : rows) {
            if (!json.equals(r.getExtJson())) {
                r.setExtJson(json);
                taskMapper.updateById(r);
            }
        }
    }
}
