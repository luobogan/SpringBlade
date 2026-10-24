package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 流程监控列表查询条件（管理员视角，全租户）。
 *
 * <p>全部可空；为空即不筛选。超时与异常维度分别走 {@code overdue} / {@code anomalyTypes}，
 * 在 {@code WfInstanceServiceImpl.monitorList} 内落地为 {@code wf_task} 过期子查与 L3 自检列过滤。</p>
 */
@Data
@Schema(description = "流程监控列表查询条件")
public class MonitorInstanceQuery implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "流程定义ID")
	private Long defId;

	@Schema(description = "流程定义名称（模糊）")
	private String defName;

	@Schema(description = "发起人")
	private Long starter;

	@Schema(description = "当前节点Key（精确）")
	private String currentNodeKey;

	@Schema(description = "状态集合 0运行中 1通过 2不通过 3撤销 4暂停 5草稿")
	private List<Integer> statuses;

	@Schema(description = "发起时间起点")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date beginTime;

	@Schema(description = "发起时间终点")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date endTime;

	@Schema(description = "仅看超时（运行中且存在已过期未办任务）")
	private Boolean overdue;

	@Schema(description = "紧急程度 0/1/2")
	private Integer urgent;

	@Schema(description = "标题关键字（模糊）")
	private String keyword;

	@Schema(description = "测试态过滤 1=仅测试 0=仅正式 null=全部")
	private Integer isTest;

	@Schema(description = "异常类型集合 1=业务行缺失 2=requestId未回填 3=部署漂移（取 L3 自检列 =0 维度）")
	private List<Integer> anomalyTypes;

}
