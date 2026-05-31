package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 属性联动实体（对应 modeattrlinkage 表）
 */
@Data
@TableName("modeattrlinkage")
@Schema(description = "属性联动")
public class ModeAttrLinkage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "联动ID")
    private Long id;

    @Schema(description = "模块ID")
    private Integer modeid;

    @Schema(description = "源字段ID")
    private Integer sourcefieldid;

    @Schema(description = "目标字段ID")
    private Integer targetfieldid;

    @Schema(description = "联动规则JSON")
    private String linkageRule;

    @Schema(description = "是否启用：1启用 0禁用")
    private Integer status;

    @Schema(description = "租户ID")
    private String tenantId;

}
