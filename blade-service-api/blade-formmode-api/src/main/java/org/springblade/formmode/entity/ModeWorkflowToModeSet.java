package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程到模块设置实体（对应 mode_workflowtomodeset 表）
 */
@Data
@TableName("mode_workflowtomodeset")
@Schema(description = "流程到模块设置")
public class ModeWorkflowToModeSet implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "设置ID")
    private Long id;

    @Schema(description = "模块ID")
    private Integer modeid;

    @Schema(description = "工作流ID")
    private Integer workflowid;

    @Schema(description = "工作流名称")
    private String workflowname;

    @Schema(description = "是否启用：1启用 0禁用")
    private Integer status;

    @Schema(description = "写入模式：0新建 1更新")
    private Integer writemode;

    @Schema(description = "字段映射JSON")
    private String fieldmapping;

    @Schema(description = "排序号")
    private Integer dsporder;

    @Schema(description = "租户ID")
    private String tenantId;

}
