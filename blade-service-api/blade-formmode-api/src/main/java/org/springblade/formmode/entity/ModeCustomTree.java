package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 自定义树配置实体（对应 mode_customtree 表）
 */
@Data
@TableName("mode_customtree")
@Schema(description = "自定义树配置")
public class ModeCustomTree implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "树ID")
    private Long id;

    @Schema(description = "模块ID")
    private Integer modeid;

    @Schema(description = "树名称")
    private String treename;

    @Schema(description = "数据SQL/类型")
    private String datasql;

    @Schema(description = "显示字段（标签）")
    private String showfield;

    @Schema(description = "ID字段")
    private String idfield;

    @Schema(description = "父ID字段")
    private String parentidfield;

    @Schema(description = "是否启用：1启用 0禁用")
    private Integer status;

    @Schema(description = "排序号")
    private Integer dsporder;

    @Schema(description = "租户ID")
    private String tenantId;

}
