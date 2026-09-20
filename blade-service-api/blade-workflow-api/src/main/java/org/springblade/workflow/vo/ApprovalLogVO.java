package org.springblade.workflow.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.core.tool.utils.DateUtil;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批流转记录视图对象
 */
@Data
@Schema(description = "审批流转记录")
public class ApprovalLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "节点Key")
    private String nodeKey;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "操作人")
    private Long operator;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "流转动作（对齐 RequestLogType）")
    private String logType;

    @Schema(description = "流转动作名称")
    private String logTypeName;

    @Schema(description = "审批意见")
    private String opinion;

    @Schema(description = "下一节点办理人用户ID（逗号分隔；按流程出口解析，部门/角色/人员等已展开为具体用户ID）")
    private String nextHandlerIds;

    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "操作时间")
    private Date operateTime;

}
