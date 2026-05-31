package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 表单基本信息实体（对应 workflow_formbase 表）
 */
@Data
@TableName("workflow_formbase")
@Schema(description = "表单基本信息")
public class WorkflowFormBase implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "表单ID")
    private Integer id;

    @Schema(description = "表单名称")
    private String formname;

    @Schema(description = "表单类型")
    private Integer formtype;

    @Schema(description = "表单HTML内容")
    private String formhtml;

    @Schema(description = "表单宽度")
    private Integer formwidth;

    @Schema(description = "租户ID")
    private String tenantId;

}
