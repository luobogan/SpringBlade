package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 表单布局实体类（Excel设计器）
 * 对应数据库表：form_layout
 */
@Data
@TableName("form_layout")
@Schema(name = "表单布局", description = "表单布局实体（Excel设计器）")
public class FormLayout implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(name = "布局ID")
    private Long id;

    @TableField("form_id")
    @Schema(name = "表单ID（关联workflow_bill）")
    private Long formId;

    @TableField("layout_name")
    @Schema(name = "布局名称")
    private String layoutName;

    @TableField("layout_json")
    @Schema(name = "布局JSON（兼容泛微E9格式）")
    private String layoutJson;

    @TableField("layout_config")
    @Schema(name = "布局配置JSON")
    private String layoutConfig;

    @TableField("status")
    @Schema(name = "状态：1启用 0禁用")
    private Integer status;

    @TableField("tenant_id")
    @Schema(name = "租户ID")
    private String tenantId;

    @TableField("create_time")
    @Schema(name = "创建时间")
    private Date createTime;

    @TableField("update_time")
    @Schema(name = "修改时间")
    private Date updateTime;
}
