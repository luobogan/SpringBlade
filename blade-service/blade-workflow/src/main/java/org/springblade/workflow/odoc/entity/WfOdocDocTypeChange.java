package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 文种转换配置（源流程 → 目标流程/文种，对应 E9 OdocTypeChangeWorkflow）。
 *
 * <p>一条源流程定义可配置多个可变更的目标；isDefault 标记默认目标。
 * fieldMaps 承载「赋值设置」（源表单字段 → 目标表单字段的取值映射，JSON）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_doc_type_change")
@Schema(description = "文种转换配置(源流程→目标流程)")
public class WfOdocDocTypeChange extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "源流程定义ID")
    private Long defId;

    @Schema(description = "目标流程定义ID")
    private Long targetDefId;

    @Schema(description = "目标公文类型 1发文 2收文 3签报")
    private Integer targetOfficalType;

    @Schema(description = "是否默认目标 0否 1是")
    private Integer isDefault;

    @Schema(description = "赋值设置(源字段→目标字段 JSON)")
    private String fieldMaps;

    @Schema(description = "是否启用 0否 1是")
    private Integer enabled;

}
