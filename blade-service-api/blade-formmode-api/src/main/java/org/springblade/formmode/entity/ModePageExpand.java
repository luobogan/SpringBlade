package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 页面扩展配置实体（对应 mode_pageexpand 表）
 */
@Data
@TableName("mode_pageexpand")
@Schema(description = "页面扩展配置")
public class ModePageExpand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "扩展ID")
    private Long id;

    @Schema(description = "模块ID")
    private Integer modeid;

    @Schema(description = "扩展类型：0保存前 1保存后 2删除前 3删除后")
    private Integer expandtype;

    @Schema(description = "Java扩展类名")
    private String javaclass;

    @Schema(description = "是否启用：1启用 0禁用")
    private Integer status;

    @Schema(description = "排序号")
    private Integer dsporder;

    @Schema(description = "租户ID")
    private String tenantId;

}
