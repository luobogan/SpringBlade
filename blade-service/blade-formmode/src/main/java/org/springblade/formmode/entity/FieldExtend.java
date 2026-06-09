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
 * 字段扩展属性实体类
 * 对应数据库表：mode_form_field_extend
 * 对标泛微E9 ModeFormFieldExtend 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mode_form_field_extend")
@Schema(name = "字段扩展属性", description = "字段扩展属性实体（对标泛微E9）")
public class FieldExtend extends Model<FieldExtend> {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(name = "主键ID")
    private Long id;

    @TableField("fieldid")
    @Schema(name = "字段ID")
    private Long fieldId;

    @TableField("formid")
    @Schema(name = "表单ID")
    private Long formId;

    @TableField("expendattr")
    @Schema(name = "扩展属性（JSON格式）")
    private String expendAttr;

    @TableField("createdate")
    @Schema(name = "创建日期")
    private String createDate;

    @TableField("createtime")
    @Schema(name = "创建时间")
    private String createTime;

    @TableField("modifydate")
    @Schema(name = "修改日期")
    private String modifyDate;

    @TableField("modifytime")
    @Schema(name = "修改时间")
    private String modifyTime;

    // =================== 逻辑删除字段 ===================

    @TableField("is_deleted")
    @Schema(name = "是否删除（0未删除 1已删除）")
    private Integer isDeleted;

    @TableField("status")
    @Schema(name = "状态（1正常 0禁用 -1已删除）")
    private Integer status;
}
