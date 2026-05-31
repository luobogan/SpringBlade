package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 代码/编码规则实体（对应 ModeCode 表）
 */
@Data
@TableName("ModeCode")
@Schema(description = "编码规则")
public class ModeCode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "编码ID")
    private Long id;

    @Schema(description = "模块ID")
    private Integer modeid;

    @Schema(description = "编码名称")
    private String codename;

    @Schema(description = "编码规则格式")
    private String codeformat;

    @Schema(description = "是否启用：1启用 0禁用")
    private Integer status;

    @Schema(description = "租户ID")
    private String tenantId;

}
