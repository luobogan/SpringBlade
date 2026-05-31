package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字段授权实体（对应 modefieldauthorize 表）
 */
@Data
@TableName("modefieldauthorize")
@Schema(description = "字段授权")
public class ModeFieldAuthorize implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "授权ID")
    private Long id;

    @Schema(description = "模块ID")
    private Integer modeid;

    @Schema(description = "字段ID（workflow_billfield.id）")
    private Integer fieldid;

    @Schema(description = "授权对象类型：1角色 2部门 3岗位 4人员")
    private Integer objtype;

    @Schema(description = "授权对象ID")
    private Integer objid;

    @Schema(description = "权限：1可见 2可编辑")
    private Integer rightvalue;

    @Schema(description = "租户ID")
    private String tenantId;

}
