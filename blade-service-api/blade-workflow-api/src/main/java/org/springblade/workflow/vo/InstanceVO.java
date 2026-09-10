package org.springblade.workflow.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.core.tool.utils.DateUtil;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程实例视图对象（实例详情）
 */
@Data
@Schema(description = "流程实例详情")
public class InstanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "实例ID")
    private Long id;

    @Schema(description = "流程定义ID")
    private Long defId;

    @Schema(description = "流程定义名称")
    private String defName;

    @Schema(description = "表单ID")
    private Long formId;

    @Schema(description = "业务数据ID")
    private Long dataId;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "业务主键 formId:dataId")
    private String bizKey;

    @Schema(description = "状态 0运行中 1通过 2不通过 3撤销 4暂停")
    private Integer status;

    @Schema(description = "当前节点Key")
    private String currentNodeKey;

    @Schema(description = "当前节点名称")
    private String currentNodeName;

    @Schema(description = "发起人")
    private Long starter;

    @Schema(description = "发起人姓名")
    private String starterName;

    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "发起时间")
    private Date startTime;

    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "结束时间")
    private Date endTime;

    @Schema(description = "紧急程度 0/1/2")
    private Integer urgency;

}
