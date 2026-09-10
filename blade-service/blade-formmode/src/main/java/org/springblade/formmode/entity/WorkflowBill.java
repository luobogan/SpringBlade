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

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "表单ID")
    private Long id;

    @Schema(description = "表单名称")
    private String formName;

    @Schema(description = "数据库表名")
    private String tableName;

    /**
     * 表单类型：0-自定义表单（表设计器创建） 1-系统表单（平台预置）
     * 对齐 ecology 路径设置「对应表单」的 isBill 联动：选择表单类型后只展示同类型表单。
     */
    @Schema(description = "表单类型：0-自定义表单 1-系统表单")
    private Integer formType;

    @Schema(description = "表单描述")
    private String description;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "模块ID")
    private Long moduleId;

    @Schema(description = "创建人")
    private Integer createUser;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "修改人")
    private Integer updateUser;

    @Schema(description = "修改时间")
    private Date updateTime;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "逻辑删除：1已删 0未删")
    @TableLogic
    private Integer isDeleted;

    @TableField("detailtablecount")
    @Schema(description = "明细表数量")
    private Integer detailTableCount;

}
