package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 表单定义实体（对应 workflow_bill 表）
 */
@Data
@TableName("workflow_bill")
@Schema(description = "表单定义")
public class WorkflowBill implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "表单ID（负数）")
    private Integer id;

    @Schema(description = "表单名称")
    private String billname;

    @Schema(description = "物理表名（formtable_main_xxx）")
    private String tablename;

    @Schema(description = "表单类型：0普通 1明细 2组合")
    private Integer billtype;

    @Schema(description = "明细表数量")
    private Integer detailtablecount;

    @Schema(description = "排序号")
    private Integer dsporder;

    @Schema(description = "创建人")
    private Integer creater;

    @Schema(description = "创建日期")
    private String createdate;

    @Schema(description = "创建时间")
    private String createtime;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "逻辑删除：1已删 0未删")
    @TableLogic
    private Integer isDeleted;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "修改时间")
    private Date updateTime;

}
