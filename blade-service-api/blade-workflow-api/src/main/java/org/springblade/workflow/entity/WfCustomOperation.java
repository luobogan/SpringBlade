package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;

/**
 * 节点「自定义操作」按钮（对齐泛微节点信息「自定义操作」）。
 *
 * <p>一条节点可配多个自定义按钮（按 {@code btnOrder} 排序），每个按钮关联一条动作明细
 * （URL / 流程操作 / 接口）与一组权限矩阵（角色/部门/人员）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_custom_operation")
@Schema(description = "自定义操作按钮")
public class WfCustomOperation extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义 ID")
    private Long defId;

    @Schema(description = "节点 Key")
    private String nodeKey;

    @Schema(description = "按钮名称")
    private String btnName;

    @Schema(description = "按钮顺序")
    private Integer btnOrder;

    @Schema(description = "是否启用 1=是 0=否")
    private Integer enabled;

    @Schema(description = "动作类型 1=URL 2=流程操作 3=接口")
    private Integer actionType;

}
