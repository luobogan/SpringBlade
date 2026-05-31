package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 编码规则明细实体（对应 ModeCodeDetail 表）
 */
@Data
@TableName("ModeCodeDetail")
@Schema(description = "编码规则明细")
public class ModeCodeDetail implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "明细ID")
    private Long id;

    @Schema(description = "编码主表ID")
    private Long codeid;

    @Schema(description = "序号")
    private Integer seq;

    @Schema(description = "段类型：0固定值 1日期 2流水号 3字段值")
    private Integer segmenttype;

    @Schema(description = "段值")
    private String segmentvalue;

    @Schema(description = "长度")
    private Integer length;

    @Schema(description = "租户ID")
    private String tenantId;

}
