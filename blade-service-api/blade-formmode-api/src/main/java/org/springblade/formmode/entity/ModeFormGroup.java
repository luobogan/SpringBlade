package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 布局组实体（对应 modeformgroup 表）
 */
@Data
@TableName("modeformgroup")
@Schema(description = "布局组")
public class ModeFormGroup implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "组ID")
    private Long id;

    @Schema(description = "模块ID")
    private Integer modeid;

    @Schema(description = "组名称")
    private String groupname;

    @Schema(description = "组类型：1主表 2明细表")
    private Integer grouptype;

    @Schema(description = "排序号")
    private Integer dsporder;

    @Schema(description = "显示条件")
    private String showcondition;

    @Schema(description = "租户ID")
    private String tenantId;

}
