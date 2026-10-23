package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 目录审批工作流明细（对应 E9 DocSecCategoryApproveWfDetail）：一个目录可挂多条审批流。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_sec_category_approve_wf_detail")
@Schema(description = "目录审批工作流明细")
public class DocSecCategoryApproveWfDetail extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "目录ID")
    private Long categoryId;

    @Schema(description = "审批类型 1生效审批 2失效审批 3批准")
    private Integer approveType;

    @Schema(description = "工作流ID(wf_process_definition.id)")
    private Long workflowId;

    @Schema(description = "流程字段ID")
    private Long workflowFieldId;

    @Schema(description = "文档属性字段ID")
    private Long docPropertyFieldId;
}
