package org.springblade.workflow.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.service.IWfTaskService;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 超时扫描任务：消费节点信息「超时设置」（settings.timeout）。
 *
 * <p>扫描 {@code wf_task} 中 {@code status=待办 且 due_time <= now} 的任务：
 * <ul>
 *   <li>{@code settings.timeout.autoApprove=true} → 走 {@link IWfTaskService#autoApprove} 自动通过；</li>
 *   <li>否则 {@code settings.timeout.remind=true} → 输出超时提醒日志（后续可替换为消息/待办催办）。</li>
 * </ul>
 * {@code due_time} 在生成待办时由 {@code WfInstanceServiceImpl#insertTask} 按节点超时小时数写入。</p>
 *
 * <p><b>性能</b>：只查「待办 + due_time 非空 + 已到期」，命中 {@code idx_assignee_status} 之外的
 * {@code due_time} 条件；用 {@code LIMIT} 限制单批规模，避免一次扫描压垮库。扫描周期可用
 * {@code blade.workflow.timeout-scan-ms} 配置（默认 10 分钟）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfTimeoutJob {

    /** 单批扫描上限 */
    private static final int BATCH_LIMIT = 200;

    private final WfTaskMapper taskMapper;
    private final WfProcessNodeMapper nodeMapper;
    private final WfInstanceMapper instanceMapper;
    private final IWfTaskService taskService;

    /** 定时扫描（默认 10 分钟一次；首次延迟 1 分钟，等应用完全就绪） */
    @Scheduled(initialDelayString = "${blade.workflow.timeout-initial-delay-ms:60000}",
        fixedDelayString = "${blade.workflow.timeout-scan-ms:600000}")
    public void scan() {
        Date now = new Date();
        List<WfTask> dueTasks;
        try {
            dueTasks = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
                .eq(WfTask::getStatus, WfTask.STATUS_TODO)
                .isNotNull(WfTask::getDueTime)
                .le(WfTask::getDueTime, now)
                .last("LIMIT " + BATCH_LIMIT));
        } catch (Exception e) {
            log.warn("[blade-workflow] 超时扫描查询失败，本轮跳过", e);
            return;
        }
        if (dueTasks.isEmpty()) {
            return;
        }
        for (WfTask task : dueTasks) {
            try {
                handle(task, now);
            } catch (Exception e) {
                // 单条失败不影响其余任务
                log.warn("[blade-workflow] 超时处理失败. taskId={}, nodeKey={}", task.getId(), task.getNodeKey(), e);
            }
        }
    }

    private void handle(WfTask task, Date now) {
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        if (inst == null) {
            return;
        }
        WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, inst.getDefId())
            .eq(WfProcessNode::getNodeKey, task.getNodeKey())
            .last("LIMIT 1"));
        if (node == null) {
            return;
        }
        long overdueMinutes = (now.getTime() - task.getDueTime().getTime()) / 60000L;
        if (WfNodeSettingsUtil.timeoutAutoApprove(node)) {
            log.info("[blade-workflow] 超时自动通过. taskId={}, nodeKey={}, 超时={}分钟",
                task.getId(), task.getNodeKey(), overdueMinutes);
            taskService.autoApprove(task.getId(), "超时自动通过");
            return;
        }
        if (WfNodeSettingsUtil.timeoutRemind(node)) {
            // 提醒扩展点：后续可替换为发消息 / 生成催办待办
            log.warn("[blade-workflow] 待办已超时，提醒处理人. taskId={}, assignee={}, nodeKey={}, 超时={}分钟",
                task.getId(), task.getAssignee(), task.getNodeKey(), overdueMinutes);
        }
    }
}
