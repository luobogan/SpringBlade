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
 * 单条数据共享授权（对应 modedatashare_single 表，对标 E9 modeDataShare_&lt;modeId&gt;）。
 *
 * <p>把某一条业务数据行单独共享给主体；行级过滤时与「本人创建」OR 叠加。</p>
 */
@Data
@TableName("modedatashare_single")
@Schema(description = "单条数据共享授权")
public class ModeDataShareSingle implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "模块ID")
    private Long modeid;

    @Schema(description = "业务数据行ID")
    private Long dataId;

    @Schema(description = "授权对象类型：1角色 2部门 3岗位 4人员 5分部")
    private Integer objtype;

    @Schema(description = "授权对象ID")
    private Long objid;

    @Schema(description = "共享级别：1查看 2编辑")
    private Integer shareLevel;

    @Schema(description = "矩阵主体（objtype=7）业务条件字段名")
    private String conditionField;

    @Schema(description = "矩阵主体（objtype=7）结果字段名")
    private String matrixValueField;

    @Schema(description = "密级下限（E9 showlevel，0~100；NULL=不限制）")
    private Integer showlevel;

    @Schema(description = "密级上限（E9 showlevel2，NULL=不封顶）")
    private Integer showlevel2;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "创建时间")
    private Date createTime;
}
