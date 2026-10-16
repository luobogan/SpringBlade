package org.springblade.workflow.odoc.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 公文待办 / 已办 / 我的请求 列表行（对应 E9 OdocReqListCmd 的公文列表）。
 *
 * <p>task 维度（待办/已办有 taskId/接收或处理时间）与实例维度（我的请求只有 instanceId）共用；
 * 公文属性来自 {@code wf_odoc_request} 镜像，标题/当前节点/发起人来自 {@code wf_instance}。</p>
 */
@Data
@Schema(description = "公文列表行")
public class OdocTodoVO {

	@Schema(description = "任务ID（我的请求为空）")
	private Long taskId;

	@Schema(description = "流程实例ID")
	private Long instanceId;

	@Schema(description = "流程定义ID")
	private Long defId;

	@Schema(description = "公文类型 1发文 2收文 3签报")
	private Integer officalType;

	@Schema(description = "流程标题")
	private String title;

	@Schema(description = "发文文号")
	private String issuedNum;

	@Schema(description = "收文编号")
	private String receiveNum;

	@Schema(description = "印发/来文单位")
	private String issuedUnit;

	@Schema(description = "主送/收文单位")
	private String receiveUnit;

	@Schema(description = "密级")
	private String secretLevel;

	@Schema(description = "紧急程度")
	private String urgency;

	@Schema(description = "主题词")
	private String topicType;

	@Schema(description = "成文日期")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime writtenDate;

	@Schema(description = "印发日期")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime issuedDate;

	@Schema(description = "正文ID")
	private Long docId;

	@Schema(description = "当前节点Key")
	private String currentNodeKey;

	@Schema(description = "发起人ID")
	private Long starter;

	@Schema(description = "任务状态（我的请求为空）")
	private Integer taskStatus;

	@Schema(description = "接收时间（待办）")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date receiveTime;

	@Schema(description = "处理时间（已办）")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date operateTime;

}
