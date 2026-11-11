package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.spring.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 自定义函数实体类
 * 对应数据库表：workflow_formula
 * 对标泛微E9 workflow_formula 表（新建自定义函数）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_formula")
@Schema(name = "自定义函数", description = "自定义函数实体（对标泛微E9 workflow_formula）")
public class WorkflowFormula extends Model<WorkflowFormula> implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(name = "主键ID")
    private Long id;

    @TableField("name")
    @Schema(name = "函数名")
    private String name;

    @TableField("formuladesc")
    @Schema(name = "函数描述")
    private String formulaDesc;

    @TableField("category")
    @Schema(name = "函数分类（自定义分组名）")
    private String category;

    @TableField("fncode")
    @Schema(name = "函数实现（JS代码/公式规则）")
    private String fnCode;

    @TableField("formscope")
    @Schema(name = "作用域（null=全局，或具体表单ID）")
    private String formScope;

    @TableField("status")
    @Schema(name = "状态（1正常 0禁用）")
    private Integer status;

    @TableField("createtime")
    @Schema(name = "创建时间")
    private String createTime;

    @TableField("modifytime")
    @Schema(name = "修改时间")
    private String modifyTime;

    @TableField("is_deleted")
    @Schema(name = "是否删除（0未删除 1已删除）")
    private Integer isDeleted;
}
