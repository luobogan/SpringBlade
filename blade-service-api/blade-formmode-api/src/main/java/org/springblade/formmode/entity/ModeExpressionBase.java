package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 表达式基础实体（对应 mode_expressionbase 表）
 */
@Data
@TableName("mode_expressionbase")
@Schema(description = "表达式基础")
public class ModeExpressionBase implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "表达式ID")
    private Long id;

    @Schema(description = "模块ID")
    private Integer modeid;

    @Schema(description = "表达式名称")
    private String expressname;

    @Schema(description = "表达式内容")
    private String expresscontent;

    @Schema(description = "目标字段ID")
    private Integer targetfieldid;

    @Schema(description = "表达式类型：0计算公式 1校验公式")
    private Integer expresstype;

    @Schema(description = "租户ID")
    private String tenantId;

}
