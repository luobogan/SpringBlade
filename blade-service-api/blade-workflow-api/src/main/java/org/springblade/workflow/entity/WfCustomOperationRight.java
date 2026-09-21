package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;

/**
 * 自定义操作「权限矩阵」（角色 / 部门 / 人员）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_custom_operation_right")
@Schema(description = "自定义操作权限矩阵")
public class WfCustomOperationRight extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关联 wf_custom_operation.id")
    private Long opId;

    @Schema(description = "权限类型 role=角色 dept=部门 person=人员")
    private String rightType;

    @Schema(description = "权限值（角色/部门/人员 ID，多个逗号分隔）")
    private String rightValue;

}
