package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 模块信息实体（对应 modeinfo 表）
 */
@Data
@TableName("modeinfo")
@Schema(description = "模块信息")
public class ModeInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "模块ID")
    private Long id;

    @Schema(description = "模块名称")
    private String modename;

    @Schema(description = "模块描述")
    private String modedescription;

    @Schema(description = "关联表单ID")
    private Integer billid;

    @Schema(description = "模块类型")
    private Integer modetype;

    @Schema(description = "应用分类ID")
    private Integer modeTreeField;

    @Schema(description = "状态：1启用 0禁用")
    private Integer status;

    @Schema(description = "排序号")
    private Integer dsporder;

    @Schema(description = "图标URL")
    private String modeicon;

    @Schema(description = "详细URL")
    private String detailurl;

    @Schema(description = "列表URL")
    private String listurl;

    @Schema(description = "创建人")
    private Integer creater;

    @Schema(description = "创建日期")
    private String createdate;

    @Schema(description = "创建时间")
    private String createtime;

    @Schema(description = "修改人")
    private Integer modifier;

    @Schema(description = "修改日期")
    private String modifydate;

    @Schema(description = "修改时间")
    private String modifytime;

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
