package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 自定义搜索配置实体（对应 mode_customsearch 表）
 */
@Data
@TableName("mode_customsearch")
@Schema(description = "自定义搜索配置")
public class ModeCustomSearch implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "搜索ID")
    private Long id;

    @Schema(description = "模块ID")
    private Integer modeid;

    @Schema(description = "搜索名称")
    private String searchname;

    @Schema(description = "搜索条件JSON")
    private String searchcondition;

    @Schema(description = "是否默认：1是 0否")
    private Integer isdefault;

    @Schema(description = "排序号")
    private Integer dsporder;

    @Schema(description = "创建人")
    private Integer creater;

    @Schema(description = "租户ID")
    private String tenantId;

}
