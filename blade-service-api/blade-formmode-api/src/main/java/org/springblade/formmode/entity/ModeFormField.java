package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 布局字段实体（对应 modeformfield 表）
 */
@Data
@TableName("modeformfield")
@Schema(description = "布局字段")
public class ModeFormField implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "字段ID")
    private Long id;

    @Schema(description = "模块ID")
    private Integer modeid;

    @Schema(description = "所属组ID")
    private Long groupid;

    @Schema(description = "workflow_billfield 字段ID")
    private Integer fieldid;

    @Schema(description = "排序号")
    private Integer dsporder;

    @Schema(description = "是否显示：1显示 0隐藏")
    private Integer isshow;

    @Schema(description = "是否只读：1只读 0可编辑")
    private Integer isreadonly;

    @Schema(description = "租户ID")
    private String tenantId;

}
