package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 流程（路径）类型。
 *
 * <p>对齐 ecology {@code workflow_type}（E9「路径类型」浏览框 wftype 的数据源）。
 * 「路径类型」由后端浏览框服务下发，前端弹窗选值时回写其 id。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_workflow_type")
@Schema(description = "流程（路径）类型")
public class WfWorkflowType extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "类型名称")
    private String typeName;

    @Schema(description = "类型描述")
    private String typeDesc;

    @Schema(description = "显示顺序")
    private Integer sortOrder;

}
