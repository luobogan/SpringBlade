package org.springblade.workflow.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 待办 / 已办列表筛选条件（去 wf_ 表 T-10：补齐 OA 常见列表筛选能力）。
 *
 * <p>同时作用于<b>读源=act</b>（{@code ACT_RU_TASK}/{@code ACT_HI_TASKINST} JOIN {@code ACT_HI_PROCINST}）
 * 与<b>回退 wf_*</b>（{@code wf_task} JOIN {@code wf_instance}）两条路径，保证翻源 / 降级时筛选口径一致。</p>
 *
 * <p>全部字段可选（{@code null} = 不筛选），向前兼容原仅按 {@code assignee} 过滤的行为。</p>
 */
public class WfTaskListFilter implements Serializable {

	private static final long serialVersionUID = 1L;

	/** 流程标题模糊（关联 {@code wf_instance.title} / {@code ACT_HI_PROCINST.TITLE_}） */
	private String title;

	/** 流程定义ID（{@code wf_definition.id} / {@code ACT_HI_PROCINST.DEF_ID_}） */
	private Long defId;

	/** 表单ID（{@code workflow_bill.id} / {@code ACT_HI_PROCINST.FORM_ID_}） */
	private Long formId;

	/** 多状态（待办=0；已办=2/4/6/7/8/11），由 todo()/done() 注入，调用方一般不传 */
	private List<Integer> statuses;

	/** 时间范围起点：待办=接收时间（{@code CREATE_TIME_}），已办=处理时间（{@code END_TIME_}） */
	private Date beginTime;

	/** 时间范围终点 */
	private Date endTime;

	/** 是否存在「需要跨实例维度过滤」的条件（标题 / 流程定义 / 表单） */
	public boolean hasInstanceFilter() {
		return (title != null && !title.isBlank()) || defId != null || formId != null;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Long getDefId() {
		return defId;
	}

	public void setDefId(Long defId) {
		this.defId = defId;
	}

	public Long getFormId() {
		return formId;
	}

	public void setFormId(Long formId) {
		this.formId = formId;
	}

	public List<Integer> getStatuses() {
		return statuses;
	}

	public void setStatuses(List<Integer> statuses) {
		this.statuses = statuses;
	}

	public Date getBeginTime() {
		return beginTime;
	}

	public void setBeginTime(Date beginTime) {
		this.beginTime = beginTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
}
