package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 模块权限实体（对应 moderightinfo 表）
 */
@Data
@TableName("moderightinfo")
@Schema(description = "模块权限")
public class ModeRightInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "权限ID")
    private Long id;

    /**
     * 模块ID（modeinfo.id）
     *
     * <p><b>类型修正（2026-10-20）</b>：modeinfo.id 是 19 位雪花 Long，原 int 列存不下，
     * 导致模块权限对自建模块/迁移表单完全失效。DDL 已由 V2026.10.20_003 改 bigint，
     * 实体同步对齐为 {@code Long}。</p>
     */
    @Schema(description = "模块ID（modeinfo.id，bigint）")
    private Long modeid;

    @Schema(description = "权限类型：1可见 2新建 3编辑 4删除 5详情 6导入 7导出")
    private Integer righttype;

    @Schema(description = "授权对象类型：1角色 2部门 3岗位 4人员")
    private Integer objtype;

    @Schema(description = "授权对象ID")
    private Integer objid;

    @Schema(description = "授权对象名称")
    private String objname;

    @Schema(description = "是否包含子部门：1是 0否")
    private Integer includesub;

    @Schema(description = "租户ID")
    private String tenantId;

}
