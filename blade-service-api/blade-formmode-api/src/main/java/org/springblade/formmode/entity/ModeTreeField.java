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
@TableName("modetreefield")
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

    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    @TableLogic
    @Schema(description = "逻辑删除")
    private Integer isDeleted;

    @Schema(description = "创建人")
    private Long createUser;

    @Schema(description = "创建时间")
    private java.time.LocalDateTime createTime;

    @Schema(description = "更新人")
    private Long updateUser;

    @Schema(description = "更新时间")
    private java.time.LocalDateTime updateTime;

}
