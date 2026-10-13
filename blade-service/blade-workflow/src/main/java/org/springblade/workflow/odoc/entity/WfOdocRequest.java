package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.time.LocalDateTime;

/**
 * 公文信息（按实例冗余，对应 E9 odoc_requestdoc）
 *
 * <p>每次流转提交后重建，冗余公文关键信息（文号、单位、日期、密级等），便于检索与交换；
 * sourceInstanceId 用于收文转发文等转文场景。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_request")
@Schema(description = "公文信息(按实例)")
public class WfOdocRequest extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程实例ID")
    private Long instanceId;

    @Schema(description = "流程定义ID")
    private Long defId;

    @Schema(description = "正文ID")
    private Long docId;

    @Schema(description = "转文来源实例ID(收文转发文等)")
    private Long sourceInstanceId;

    @Schema(description = "公文类型 1发文 2收文 3签报")
    private Integer officalType;

    @Schema(description = "发文文号")
    private String issuedNum;

    @Schema(description = "收文编号")
    private String receiveNum;

    @Schema(description = "印发/来文单位")
    private String issuedUnit;

    @Schema(description = "主送/收文单位")
    private String receiveUnit;

    @Schema(description = "成文日期")
    private LocalDateTime writtenDate;

    @Schema(description = "印发日期")
    private LocalDateTime issuedDate;

    @Schema(description = "密级")
    private String secretLevel;

    @Schema(description = "紧急程度")
    private String urgency;

    @Schema(description = "主题词")
    private String topicType;

}
