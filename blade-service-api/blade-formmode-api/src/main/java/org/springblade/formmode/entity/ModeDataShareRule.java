package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 模块数据范围共享规则（对应 modedatasharerule 表）。
 *
 * <p>对标 ecology 的 modedatasharerule，但做精简够用子集：把模块的查看/维护权限
 * 共享给 角色 / 部门 / 人员 / 岗位；命中者可见该模块全部数据，未命中者默认仅看自己创建的。</p>
 *
 * @see org.springblade.formmode.service.IDataScopeService
 */
@Data
@TableName("modedatasharerule")
@Schema(description = "模块数据范围共享规则")
public class ModeDataShareRule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "模块ID（modeinfo.id）")
    private Long modeid;

    @Schema(description = "权限类型：1查看 2维护")
    private Integer type;

    @Schema(description = "授权对象类型（已统一）：1角色 2部门 3岗位 4人员 5分部")
    private Integer objtype;

    @Schema(description = "授权对象ID（人员/部门/角色/岗位）")
    private Long objid;

    @Schema(description = "是否含子级（部门/角色生效）：1是 0否")
    private Integer includeSub;

    @Schema(description = "附加：仅本人创建的数据可见（与共享叠加）：1是 0否")
    private Integer creatorSelf;

    /** 条件化授权字段（workflow_billfield.fielddbname，运行时按白名单校验，禁止任意 SQL 片段） */
    private String conditionField;

    /** 条件操作符：eq/ne/gt/ge/lt/le/like/in（小写） */
    private String conditionOp;

    /** 条件值（in 用英文逗号分隔；运行时参数化拼接，不直接入 SQL） */
    private String conditionValue;

    /** 是否启用条件化授权：1是 0否（默认否，存量规则行为不变） */
    private Integer conditionEnabled;

    /** 矩阵主体（objtype=7）结果字段名；conditionField 复用为业务条件字段名 */
    private String matrixValueField;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;
}
