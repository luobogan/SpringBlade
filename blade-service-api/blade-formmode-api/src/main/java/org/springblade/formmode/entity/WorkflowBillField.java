package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 表单字段定义实体（对应 workflow_billfield 表）
 */
@Data
@TableName("workflow_billfield")
@Schema(description = "表单字段定义")
public class WorkflowBillField implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "字段ID（表单关联用负数）")
    private Integer id;

    @Schema(description = "所属表单ID（workflow_bill.id）")
    private Integer billid;

    @Schema(description = "字段名")
    private String fieldname;

    @Schema(description = "字段数据库列名")
    private String fielddbname;

    @Schema(description = "HTML表单元素类型")
    private Integer fieldhtmltype;

    @Schema(description = "字段类型")
    private Integer fieldtype;

    @Schema(description = "字段长度")
    private Integer fieldlen;

    @Schema(description = "小数点位数")
    private Integer decimaldigit;

    @Schema(description = "字段默认值")
    private String defaultvalue;

    @Schema(description = "显示顺序")
    private Integer dsporder;

    @Schema(description = "是否必填：1是 0否")
    private Integer isnull;

    @Schema(description = "是否唯一：1是 0否")
    private Integer uniquevalue;

    @Schema(description = "字段提示信息")
    private String fieldmsg;

    @Schema(description = "数据库字段类型")
    private Integer fielddbtype;

    @Schema(description = "浏览框类型")
    private Integer browtype;

    @Schema(description = "浏览框URL ID")
    private Integer browserurlid;

    @Schema(description = "选择项数据（下拉框/单选/复选的选项）")
    private String selectitem;

    @Schema(description = "关联明细表ID")
    private Integer detailtable;

    @Schema(description = "是否主表字段：1是 0否")
    private Integer ismain;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "租户ID")
    private String tenantId;

}
