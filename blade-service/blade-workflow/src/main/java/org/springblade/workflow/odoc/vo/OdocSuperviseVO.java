package org.springblade.workflow.odoc.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 公文督办列表行（对应 E9 OdocSuperviceAction.getSuperviseList）。
 */
@Data
@Schema(description = "公文督办记录")
public class OdocSuperviseVO {

	@Schema(description = "督办记录ID")
	private Long id;

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

	@Schema(description = "督办人ID")
	private Long supervisorId;

	@Schema(description = "被督办对象(发起人)ID")
	private Long starterId;

	@Schema(description = "状态 0督办中 1已办结 2已取消")
	private Integer status;

	@Schema(description = "督办期限")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime deadline;

	@Schema(description = "督办说明")
	private String remark;

	@Schema(description = "最近提醒时间")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime lastRemindTime;

	@Schema(description = "发起时间")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

}
