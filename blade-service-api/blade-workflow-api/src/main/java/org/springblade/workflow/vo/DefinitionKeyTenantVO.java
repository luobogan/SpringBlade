package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 流程定义的引擎身份（「以 Flowable 为唯一事实源」改造）。
 *
 * <p>供跨服务回填 / 运行时定位使用：给定 {@code wf_process_definition.id}，返回其对应的
 * Flowable 原生身份 {@code procKey}（= BPMN process id）与 {@code tenantId}。</p>
 */
@Data
@Schema(description = "流程定义引擎身份：procKey + tenantId")
public class DefinitionKeyTenantVO implements Serializable {

	@Schema(description = "流程定义Key（BPMN process id）")
	private String procKey;

	@Schema(description = "租户ID（wf_process_definition.tenant_id）")
	private String tenantId;

}
