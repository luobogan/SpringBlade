package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 文种转换记录（按实例，对应 E9 Odoc_TypeChangeRevision）。
 *
 * <p>每次对某实例执行文种变更落一条：原实例 → 目标流程/文种、原因、操作人。
 * chanInstanceId 为变更后新实例（引擎级重路由/起新实例为后续增强，当前可空）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_doc_type_change_revision")
@Schema(description = "文种转换记录(按实例)")
public class WfOdocDocTypeChangeRevision extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "原流程实例ID")
    private Long instanceId;

    @Schema(description = "源流程定义ID")
    private Long defId;

    @Schema(description = "目标流程定义ID")
    private Long targetDefId;

    @Schema(description = "目标公文类型 1发文 2收文 3签报")
    private Integer targetOfficalType;

    @Schema(description = "变更原因")
    private String reasons;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "变更后新实例ID(引擎级重路由后续增强,当前可空)")
    private Long chanInstanceId;

    @Schema(description = "测试态标记 1=测试产生(可一键清理)")
    private Integer isTest;

}
