package org.springblade.workflow.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.core.tool.utils.DateUtil;

import java.io.Serializable;
import java.util.Date;

/**
 * 待办 / 已办任务视图对象（语义层任务）
 *
 * <p>与引擎任务 {@link TaskVO} 区分：本对象面向业务列表展示，
 * 携带流程标题、节点名称、发起人等语义信息。</p>
 */
@Data
@Schema(description = "待办/已办任务")
public class WfTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID（wf_task.id）")
    private Long id;

    @Schema(description = "流程实例ID")
    private Long instId;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "流程定义名称")
    private String defName;

    @Schema(description = "表单ID")
    private Long formId;

    @Schema(description = "业务数据ID")
    private Long dataId;

    @Schema(description = "节点Key")
    private String nodeKey;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "办理人")
    private Long assignee;

    @Schema(description = "任务状态 0待办 2已办 4办结 6自动提交 7协办 8抄送 11传阅")
    private Integer status;

    @Schema(description = "发起人")
    private Long starter;

    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "流程发起时间")
    private Date startTime;

    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "任务接收时间")
    private Date receiveTime;

    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "任务处理时间")
    private Date operateTime;

    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "截止时间")
    private Date dueTime;

    @Schema(description = "紧急程度 0/1/2")
    private Integer urgency;

}
