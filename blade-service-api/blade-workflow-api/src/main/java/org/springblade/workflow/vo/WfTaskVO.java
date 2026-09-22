package org.springblade.workflow.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "流程实例ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long instId;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "流程定义名称")
    private String defName;

    @Schema(description = "流程定义ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long defId;

    @Schema(description = "所属实例状态 0运行中 1通过 2不通过 3撤销 4暂停 5草稿")
    private Integer instStatus;

    @Schema(description = "表单ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long formId;

    @Schema(description = "业务数据ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long dataId;

    @Schema(description = "节点Key")
    private String nodeKey;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "办理人")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long assignee;

    @Schema(description = "任务状态 0待办 2已办 4办结 6自动提交 7协办 8抄送 11传阅")
    private Integer status;

    @Schema(description = "发起人")
    @JsonSerialize(using = ToStringSerializer.class)
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

    /**
     * 是否测试态任务（1=流程测试产生，0=正式）。
     *
     * <p>用于前端识别「这条是测试数据」并据此着色 / 分栏 / 置为只读（方案 §6.4 **C3** / S5）。
     * 生产列表（待办/已办/角标）已由 C1 在服务端排除 `is_test=1`，故正常情况下
     * 生产列表拿不到测试任务；本字段主要为「测试域内的真人办理」（C12）与
     * 将来放开管理员可见性时提供判别依据。</p>
     */
    @Schema(description = "是否测试态 1=流程测试 0=正式")
    private Integer isTest;

}
