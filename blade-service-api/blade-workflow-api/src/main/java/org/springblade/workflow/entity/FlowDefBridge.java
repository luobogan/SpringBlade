package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 业务定义与 Flowable 引擎定义桥接（DEF_KEY_，决策 D6）。
 *
 * <p>原 {@code wf_process_definition} 将在 P6 退役，本表独立存活，承载
 * 业务 defId ↔ 引擎 processDefinition（KEY_ + 版本）的 1:N 反查与版本统计。</p>
 * <p>命名 {@code flow_} 前缀（非 {@code wf_}，符合"不保留 wf_ 表"目标）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("flow_def_bridge")
@Schema(description = "业务定义与 Flowable 引擎定义桥接（DEF_KEY_，D6）")
public class FlowDefBridge extends TenantEntity {

	private static final long serialVersionUID = 1L;

	@Schema(description = "业务定义 key（DEF_KEY_，D6 桥接键；默认取引擎 process key）")
	private String defKey;

	@Schema(description = "业务侧流程定义ID（原 wf_process_definition.id）")
	private Long defId;

	@Schema(description = "Flowable process id（KEY_）")
	private String engineDefKey;

	@Schema(description = "Flowable 流程定义ID（含版本，如 key:ver:rand）")
	private String engineDefId;

	@Schema(description = "引擎流程定义版本")
	private Integer engineVersion;

	@Schema(description = "1启用 0停用")
	private Integer status;

}
