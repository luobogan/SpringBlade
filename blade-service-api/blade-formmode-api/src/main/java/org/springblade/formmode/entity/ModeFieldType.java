package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字段类型实体（对应 mode_fieldtype 表）
 */
@Data
@TableName("mode_fieldtype")
@Schema(description = "字段类型")
public class ModeFieldType implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "类型ID")
    private Integer id;

    @Schema(description = "类型名称")
    private String typename;

    @Schema(description = "HTML字段类型")
    private Integer htmltype;

    @Schema(description = "数据库字段类型")
    private Integer dbtype;

    @Schema(description = "租户ID")
    private String tenantId;

}
