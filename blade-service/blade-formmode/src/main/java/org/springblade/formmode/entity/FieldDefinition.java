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
 * 字段定义实体类
 * 对应数据库表：workflow_billfield
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_billfield")
@Schema(name = "字段定义", description = "字段定义实体")
public class FieldDefinition extends Model<FieldDefinition> {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(name = "字段ID")
    private Long id;

    @TableField("billid")
    @Schema(name = "所属表单ID")
    private Long billId;

    @TableField("fieldname")
    @Schema(name = "字段名")
    private String fieldName;

    @TableField("fieldlabel")
    @Schema(name = "字段标签（显示名称）")
    private String fieldLabel;

    @TableField("fielddbname")
    @Schema(name = "数据库列名")
    private String fieldDbName;

    @TableField("fieldhtmltype")
    @Schema(name = "HTML表单元素类型")
    private Integer fieldHtmlType;

    @TableField("fieldtype")
    @Schema(name = "字段类型")
    private Integer fieldType;

    @TableField("fieldlen")
    @Schema(name = "字段长度")
    private Integer fieldLen;

    @TableField("decimaldigit")
    @Schema(name = "小数点位数")
    private Integer decimalDigit;

    @TableField("defaultvalue")
    @Schema(name = "默认值")
    private String defaultValue;

    @TableField("dsporder")
    @Schema(name = "显示顺序")
    private Integer dsOrder;

    @TableField("isnull")
    @Schema(name = "是否必填")
    private Integer isNull;

    @TableField("uniquevalue")
    @Schema(name = "是否唯一")
    private Integer uniqueValue;

    @TableField("fieldmsg")
    @Schema(name = "字段提示")
    private String fieldMsg;

    @TableField("fielddbtype")
    @Schema(name = "数据库字段类型")
    private String fieldDbType;

    @TableField("browtype")
    @Schema(name = "浏览框类型")
    private Integer browType;

    @TableField("browserurlid")
    @Schema(name = "浏览框URL ID")
    private Integer browserUrlId;

    @TableField("selectitem")
    @Schema(name = "选项数据")
    private String selectItem;

    @TableField("detailtable")
    @Schema(name = "关联明细表ID")
    private Integer detailTable;

    @TableField("ismain")
    @Schema(name = "是否主表字段")
    private Integer isMain;

    @TableField("remark")
    @Schema(name = "备注")
    private String remark;

    @TableField("tenant_id")
    @Schema(name = "租户ID")
    private String tenantId;
}
