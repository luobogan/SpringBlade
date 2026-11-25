package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 表单建模模块目录权限 */
@Data
@TableName("form_dir_auth")
@Schema(description = "模块目录权限")
public class FormDirAuth implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "目录ID")
    private Long dirId;

    @Schema(description = "授权对象类型：1角色 2部门 3岗位 4人员 5分部 7全员")
    private Integer subjectType;

    @Schema(description = "授权对象ID")
    private Long subjectId;

    @Schema(description = "权限类型：1可见 2新增 3编辑 4删除")
    private Integer rightType;

    @Schema(description = "是否包含子级")
    private Integer includeSub;

    @Schema(description = "业务状态")
    private Integer status;

    @Schema(description = "数据范围：1全部 2本人创建 3所在机构 4所在机构及子级 5自定义")
    private Integer scopeType;

    @Schema(description = "自定义表单ID，逗号分隔")
    private String scopeValue;
}
