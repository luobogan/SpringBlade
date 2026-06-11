package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字段选项配置实体类
 * 对应数据库表：mode_form_field_option
 * 对标泛微E9 selectItem 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mode_form_field_option")
@Schema(name = "字段选项配置", description = "字段选项配置实体（对标泛微E9 selectItem）")
public class FieldOption extends Model<FieldOption> {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(name = "主键ID")
    private Long id;

    @TableField("fieldid")
    @Schema(name = "字段ID")
    private Long fieldId;

    @TableField("formid")
    @Schema(name = "表单ID")
    private String formId;

    @TableField("optionvalue")
    @Schema(name = "选项值")
    private String optionValue;

    @TableField("optionlabel")
    @Schema(name = "选项标签")
    private String optionLabel;

    @TableField("listorder")
    @Schema(name = "显示顺序")
    private Integer listOrder;

    @TableField("isdefault")
    @Schema(name = "是否默认选中（0否 1是）")
    private Integer isDefault;

    // =================== 逻辑删除字段 ===================

    @TableField("is_deleted")
    @Schema(name = "是否删除（0未删除 1已删除）")
    private Integer isDeleted;

    @TableField("status")
    @Schema(name = "状态（1正常 0禁用 -1已删除）")
    private Integer status;
}
