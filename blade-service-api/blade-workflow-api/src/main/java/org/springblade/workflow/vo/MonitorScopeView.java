package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

/**
 * 监控委托范围解析结果（服务内部使用，供 monitorList / 异常聚合 做数据范围裁剪）
 *
 * <p>由 {@code IWfMonitorScopeService#resolve(Long)} 计算：给定当前用户，
 * 汇总其所有委托范围得到「允许查看的发起人集合」与「允许查看的流程定义集合」。</p>
 */
@Data
@Schema(description = "监控委托范围解析结果")
public class MonitorScopeView {

	@Schema(description = "当前用户是否拥有任意委托范围（false=无任何监控委托，应返回空列表）")
	private boolean hasAny;

	@Schema(description = "是否覆盖全部流程定义（任一范围 defId 为 null）")
	private boolean allDefs;

	@Schema(description = "允许查看的流程定义ID集合（allDefs=true 时忽略）")
	private Set<Long> defIds;

	@Schema(description = "允许查看的发起人ID集合（按部门/分部解析为具体人员）")
	private Set<Long> starterIds;
}
