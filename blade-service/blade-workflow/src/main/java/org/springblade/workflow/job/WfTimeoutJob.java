package org.springblade.workflow.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.service.IWfTimeoutService;
import org.springblade.workflow.service.helper.WfTaskActWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * 超时扫描任务：消费节点超时规则（wf_node_timeout，多条）。
 *
 * <p>扫描 {@code wf_task} 中 {@code status=待办 且 due_time <= now 且 timeout_handled=0 且 is_test=0} 的任务，
 * 取第一条已到期规则由 {@link IWfTimeoutService#fire} 执行动作（自动通过 / 流转 / 指定操作者 / 提醒）。
 * 无规则节点回退旧 {@code settings.timeout} 单条配置（由 WfTimeoutServiceImpl 处理）。</p>
 *
 * <p><b>P3-3 改造（本类）</b>：
 * <ol>
 *   <li><b>D9 多副本选主</b>：基于 MySQL 命名锁 {@code GET_LOCK}/{@code RELEASE_LOCK} 保证集群中仅一个
 *       实例执行扫描，避免多副本重复触发超时动作。锁在同一条连接上获取并在 finally 释放，实例宕机连接断开即自动释放。</li>
 *   <li><b>R4 失败不误标记</b>：{@link IWfTimeoutService#fire} 仅成功才置 {@code timeout_handled=1}（已实现）；
 *       本类 {@link #handle} 进一步收敛——{@code firstOverdue} 返回 null 不再无条件置位，区分
 *       「无规则（确实无需超时）」与「规则存在但无到期项 / 计算异常（疑似配置异常）」，后者以 ERROR 级告警暴露，
 *       避免静默吞掉本该触发的超时，也避免对损坏配置任务的无限空转。重试计数 / 死信（防风暴）由 T-8 迁移后补。</li>
 * </ol>
 * </p>
 *
 * <p><b>运行期切换（P3-6）</b>：当前任务仍在 {@code wf_task}；去 wf_* 表后，扫描源将切到原生
 * {@code ACT_RU_TASK.DUE_DATE_}（Flowable 任务查询 {@code taskDueDateMax}），本扫描壳与 D9/R4 逻辑保持不变。</p>
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

    /** D9 选主锁名（MySQL 实例级命名锁；前缀区分业务，避免与其它应用撞名） */
    private static final String LOCK_NAME = "blade_wf_timeout_scan";

    private final WfTaskMapper taskMapper;
    private final WfInstanceMapper instanceMapper;
    private final IWfTimeoutService timeoutService;
    /** 任务业务列双写收口器（方案 A1） */
    private final WfTaskActWriter taskActWriter;
    /** 仅用于 D9 选主锁的获取/释放（在同一条连接上完成，保证 GET_LOCK 与 RELEASE_LOCK 同会话） */
    private final JdbcTemplate jdbcTemplate;

    /** 定时扫描（默认 10 分钟一次；首次延迟 1 分钟，等应用完全就绪） */
    @Scheduled(initialDelayString = "${blade.workflow.timeout-initial-delay-ms:60000}",
        fixedDelayString = "${blade.workflow.timeout-scan-ms:600000}")
    public void scan() {
        // D9：同一 MySQL 实例中仅一个副本能拿到命名锁，其余跳过本轮扫描。
        jdbcTemplate.execute((Connection conn) -> {
            boolean acquired;
            try (PreparedStatement ps = conn.prepareStatement("SELECT GET_LOCK(?, ?)")) {
                ps.setString(1, LOCK_NAME);
                ps.setInt(2, 0); // 非阻塞：拿不到立即返回 0，而非等待
                try (ResultSet rs = ps.executeQuery()) {
                    acquired = rs.next() && rs.getInt(1) == 1;
                }
            }
            if (!acquired) {
                log.debug("[blade-workflow] 超时扫描由其它副本执行，本副本跳过 (D9 选主). lock={}", LOCK_NAME);
                return null;
            }
            try {
                doScan();
            } finally {
                // 同连接释放，确保锁随扫描结束归还；若本副本宕机，连接断开也会自动释放。
                try (PreparedStatement ps = conn.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                    ps.setString(1, LOCK_NAME);
                    ps.executeQuery();
                } catch (SQLException e) {
                    log.warn("[blade-workflow] 释放超时扫描锁失败. lock={}", LOCK_NAME, e);
                }
            }
            return null;
        });
    }

    /** 实际扫描逻辑（已持有 D9 选主锁） */
    private void doScan() {
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
        // 纵深防御：实例测试态不参与（与扫描阶段一致）
        if (inst.getIsTest() != null && inst.getIsTest() == 1) {
            return;
        }
        long overdueMinutes = (now.getTime() - task.getDueTime().getTime()) / 60000L;

        // R4 防护：规则求值异常不得把本该触发的超时静默吞掉，也不得对损坏配置无限空转。
        // 先判定「是否有启用规则」，再在 try 内求值，区分三种分支：
        //  - 无启用规则：确实无需超时，标记已处理避免空转；
        //  - 规则存在但 firstOverdue 抛异常：保留未处理，下一周期重试（记录告警）；
        //  - 规则存在但无到期项（疑似配置异常）：标记已处理打破无限空转 + ERROR 告警暴露。
        List<org.springblade.workflow.entity.WfNodeTimeout> rules =
            timeoutService.listEnabled(inst.getDefId(), task.getNodeKey());
        if (rules.isEmpty()) {
            markHandled(task);
            return;
        }
        org.springblade.workflow.entity.WfNodeTimeout rule;
        try {
            rule = timeoutService.firstOverdue(inst.getDefId(), task.getNodeKey(), task, inst, now);
        } catch (Exception e) {
            log.error("[blade-workflow] 超时规则计算异常，保留待重试. taskId={}, nodeKey={}",
                task.getId(), task.getNodeKey(), e);
            return;
        }
        if (rule == null) {
            log.error("[blade-workflow] 超时规则存在但无到期项（疑似配置异常），标记已处理并告警. "
                + "taskId={}, nodeKey={}", task.getId(), task.getNodeKey());
            markHandled(task);
            return;
        }

        log.info("[blade-workflow] 超时规则命中. taskId={}, nodeKey={}, 超时={}分钟, action={}",
            task.getId(), task.getNodeKey(), overdueMinutes, rule.getActionWay());
        // fire 内部仅成功才置 timeout_handled=1（R4）；失败保留未处理由后续周期重试。
        timeoutService.fire(rule, task, inst);
    }

    private void markHandled(WfTask task) {
        task.setTimeoutHandled(1);
        taskMapper.updateById(task);
        // 超时防重标记同步到 ACT_*（TIMEOUT_HANDLED_ 为 wf_task 独有列，须双写）
        taskActWriter.sync(task);
    }
}
