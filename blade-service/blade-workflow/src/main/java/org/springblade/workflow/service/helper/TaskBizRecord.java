package org.springblade.workflow.service.helper;

import lombok.Data;

import java.util.Date;

/**
 * 任务业务快照（<b>纯值对象，不落库</b>）。
 *
 * <p>【去 wf_ 表 · 批次 1 / 2026-11-10】取代 {@code WfTask} 实体的「写入载体」角色：
 * 引擎任务创建后由业务侧用 {@code IdWorker.getId()} 预生成 {@code bizTaskId}，
 * 经 {@link WfTaskActDirectWriter} 直写 {@code ACT_RU_TASK}/{@code ACT_HI_TASKINST}
 * 的自定义业务列，<b>不再向 {@code wf_task} 插入行</b>。</p>
 *
 * <p><b>不得继承 {@code WfTask}</b>：该实体已标记废弃，此处保持零耦合。
 * {@code WfTask} 仅作为历史运行期语义对象在 {@link WfTaskBizResolver} 中还原。</p>
 */
@Data
public class TaskBizRecord {

	/** 业务任务ID（雪花，写入 ACT.BIZ_TASK_ID_，列表 VO.id 与操作链契约） */
	private Long bizTaskId;

	/** 引擎任务ID（ACT_RU_TASK.ID_ / ACT_HI_TASKINST.ID_） */
	private String engineTaskId;

	private Long instId;
	private String nodeKey;
	private Long assignee;
	private Long originalUser;
	private Integer status;
	private Integer isTest;
	private Integer signOrder;
	private Date viewTime;
	private Integer timeoutHandled;
	private Date dueTime;
	private Date receiveTime;
	private Date operateTime;
	private String tenantId;
}
