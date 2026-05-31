package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 模块树/应用分类实体（对应 modeTreeField 表）
 */
@Data
@TableName("modeTreeField")
@Schema(description = "模块树/应用分类")
public class ModeTreeField implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "父分类ID")
    private Long parentid;

    @Schema(description = "层级")
    private Integer level;

    @Schema(description = "排序号")
    private Integer dsporder;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "租户ID")
    private String tenantId;

}
