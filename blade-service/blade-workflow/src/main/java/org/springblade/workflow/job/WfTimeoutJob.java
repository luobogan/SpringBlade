package org.springblade.workflow.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.service.IWfTimeoutService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 超时扫描任务：消费节点超时规则（wf_node_timeout，多条）。
 *
 * <p>扫描 {@code wf_task} 中 {@code status=待办 且 due_time <= now 且 timeout_handled=0 且 is_test=0} 的任务，
 * 取第一条已到期规则由 {@link IWfTimeoutService#fire} 执行动作（自动通过 / 流转 / 指定操作者 / 提醒）。
 * 无规则节点回退旧 {@code settings.timeout} 单条配置（由 WfTimeoutServiceImpl 处理）。</p>
 *
 * <p><b>性能</b>：只查「待办 + due_time 非空 + 已到期 + 未处理」，用 {@code LIMIT} 限制单批规模。
 * 扫描周期可用 {@code blade.workflow.timeout-scan-ms} 配置（默认 10 分钟）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfTimeoutJob {

    /** 单批扫描上限 */
    private static final int BATCH_LIMIT = 200;

    private final WfTaskMapper taskMapper;
    private final WfInstanceMapper instanceMapper;
    private final IWfTimeoutService timeoutService;

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
                .eq(WfTask::getTimeoutHandled, 0)
                // 测试态任务一律不参与超时扫描（方案 §6.4 C4 / V5）。
                // 否则测试待办到期后会触发「自动通过 / 转办给真人 / 催办留痕」：
                // 既干扰测试结果，还会主动给真人新建任务、扩大测试数据的可见面。
                // 注：wf_task.is_test 为 tinyint NOT NULL DEFAULT 0，不存在 NULL 漏网。
                .eq(WfTask::getIsTest, 0)
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
        long overdueMinutes = (now.getTime() - task.getDueTime().getTime()) / 60000L;
        org.springblade.workflow.entity.WfNodeTimeout rule =
            timeoutService.firstOverdue(inst.getDefId(), task.getNodeKey(), task, inst, now);
        if (rule == null) {
            // 无到期规则（如规则被禁用）：标记已处理，避免反复扫描
            task.setTimeoutHandled(1);
            taskMapper.updateById(task);
            return;
        }
        log.info("[blade-workflow] 超时规则命中. taskId={}, nodeKey={}, 超时={}分钟, action={}",
            task.getId(), task.getNodeKey(), overdueMinutes, rule.getActionWay());
        timeoutService.fire(rule, task, inst);
    }
}
