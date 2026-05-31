package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 浏览框类型配置实体（对应 mode_browser_type 表）
 */
@Data
@TableName("mode_browser_type")
@Schema(description = "浏览框类型配置")
public class ModeBrowserType implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "类型ID")
    private Long id;

    @Schema(description = "浏览框ID")
    private Long browserid;

    @Schema(description = "类型名称")
    private String typename;

    @Schema(description = "类型值")
    private String typevalue;

    @Schema(description = "排序号")
    private Integer dsporder;

    @Schema(description = "租户ID")
    private String tenantId;

}
