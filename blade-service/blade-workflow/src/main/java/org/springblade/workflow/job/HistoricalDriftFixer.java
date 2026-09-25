package org.springblade.workflow.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.service.IProcessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 一次性历史漂移修复脚本（P0 止血后的存量清理）。
 *
 * <p>修复前，withdraw/cancel/stop/resume 只改 wf_* 不碰引擎，导致历史实例在引擎里
 * 残留 {@code ACT_RU_EXECUTION/ACT_RU_TASK}（业务已撤销/终止/暂停，引擎却仍运行中）。
 * 本脚本调用修复后新增的引擎同步方法（{@code deleteProcessInstance} /
 * {@code suspendProcessInstance}）把存量孤儿回滚到与业务一致。</p>
 *
 * <p><b>特性：幂等、可重复执行、默认不启用。</b>
 * 引擎方法自身对「实例已不存在/已挂起」做了兜底（捕获 FlowableException 忽略），
 * 因此重复跑不会报错，也不会重复产生副作用。</p>
 *
 * <p><b>启用方式（一次性）</b>：以 {@code --workflow.drift-fix.enabled=true} 启动
 * blade-workflow 服务一次；先保持 {@code workflow.drift-fix.dry-run=true}（默认）观察
 * 将要修复的条数，确认识别准确后再设 {@code dry-run=false} 真正执行；执行后跑
 * {@code drift_check.sql} 复核应为 0；最后移除该参数并建议删除本类。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "workflow.drift-fix", name = "enabled", havingValue = "true")
public class HistoricalDriftFixer implements CommandLineRunner {

	private final IProcessService processService;
	private final WfInstanceMapper instanceMapper;
	private final WfTaskMapper taskMapper;
	private final RuntimeService runtimeService;

	/** 是否仅打印将修复的条数而不实际写（dry-run）。默认 true。 */
	@Value("${workflow.drift-fix.dry-run:true}")
	private boolean dryRun;

	@Override
	public void run(String... args) {
		log.info("[drift-fix] 历史漂移修复开始. dryRun={}", dryRun);
		int deleted = fixTerminalInstances();
		int suspended = fixSuspendedInstances();
		int closedTasks = fixDanglingTasks();
		log.info("[drift-fix] 完成. 引擎实例删除(业务终态)={}, 引擎实例挂起(业务暂停)={}, 悬挂待办关闭={}, dryRun={}",
			deleted, suspended, closedTasks, dryRun);
	}

	/**
	 * 业务已终态（通过1 / 不通过2 / 撤销3）但引擎实例仍在运行 → deleteProcessInstance。
	 * 同时覆盖「正常办结后引擎未清理」的历史实例（delete 对不存在的实例是安全 no-op）。
	 */
	private int fixTerminalInstances() {
		List<WfInstance> list = instanceMapper.selectList(new LambdaQueryWrapper<WfInstance>()
			.in(WfInstance::getStatus,
				WfInstance.STATUS_APPROVED, WfInstance.STATUS_REJECTED, WfInstance.STATUS_CANCELED)
			.isNotNull(WfInstance::getEngineInstId)
			.eq(WfInstance::getIsTest, 0));
		int n = 0;
		for (WfInstance inst : list) {
			log.info("[drift-fix] 终态实例→同步删除引擎实例. instId={}, engineInstId={}, status={}",
				inst.getId(), inst.getEngineInstId(), inst.getStatus());
			if (!dryRun) {
				processService.deleteProcessInstance(inst.getEngineInstId(), "历史漂移修复-业务已终态");
			}
			n++;
		}
		return n;
	}

	/**
	 * 业务已暂停(4) 但引擎实例仍「活跃未挂起」 → suspendProcessInstance。
	 * 先查活跃态避免对已挂起/已结束的实例重复调用。
	 */
	private int fixSuspendedInstances() {
		List<WfInstance> list = instanceMapper.selectList(new LambdaQueryWrapper<WfInstance>()
			.eq(WfInstance::getStatus, WfInstance.STATUS_SUSPENDED)
			.isNotNull(WfInstance::getEngineInstId)
			.eq(WfInstance::getIsTest, 0));
		int n = 0;
		for (WfInstance inst : list) {
			boolean stillActive = runtimeService.createProcessInstanceQuery()
				.processInstanceId(inst.getEngineInstId())
				.active()
				.count() > 0;
			if (!stillActive) {
				continue; // 引擎已挂起或已结束，无需再挂起
			}
			log.info("[drift-fix] 暂停实例→同步挂起引擎实例. instId={}, engineInstId={}",
				inst.getId(), inst.getEngineInstId());
			if (!dryRun) {
				processService.suspendProcessInstance(inst.getEngineInstId());
			}
			n++;
		}
		return n;
	}

	/**
	 * 悬挂待办：wf_task 仍 status=0（待办）但其 engine_task_id 已不在 ACT_RU_TASK
	 * （引擎该任务已结束/推进）→ 置为办结(4)，避免「永远待办」。
	 * 仅关联引擎任务 ID 且确实不在运行态任务表中的行才会被关闭，运行中的正常待办不会被误伤。
	 */
	private int fixDanglingTasks() {
		LambdaQueryWrapper<WfTask> q = new LambdaQueryWrapper<WfTask>()
			.eq(WfTask::getStatus, WfTask.STATUS_TODO)
			.eq(WfTask::getIsTest, 0)
			.isNotNull(WfTask::getEngineTaskId)
			.notInSql(WfTask::getEngineTaskId, "SELECT ID_ FROM ACT_RU_TASK");
		if (dryRun) {
			long cnt = taskMapper.selectCount(q);
			log.info("[drift-fix] 悬挂待办(待关闭)预估={}", cnt);
			return (int) cnt;
		}
		int updated = taskMapper.update(null, new LambdaUpdateWrapper<WfTask>()
			.set(WfTask::getStatus, WfTask.STATUS_FINISHED)
			.set(WfTask::getOperateTime, new Date())
			.eq(WfTask::getStatus, WfTask.STATUS_TODO)
			.eq(WfTask::getIsTest, 0)
			.isNotNull(WfTask::getEngineTaskId)
			.notInSql(WfTask::getEngineTaskId, "SELECT ID_ FROM ACT_RU_TASK"));
		log.info("[drift-fix] 悬挂待办已关闭={}", updated);
		return updated;
	}
}
