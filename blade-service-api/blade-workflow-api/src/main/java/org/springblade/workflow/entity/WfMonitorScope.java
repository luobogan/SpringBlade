package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.util.Set;

/**
 * 流程监控委托范围
 *
 * <p>对应设计文档 §1.3：把「运维级全租户可见」的监控权限，按部门/分部/人员委托给指定用户，
 * 让其仅能查看授权范围内的流程实例（详见 {@code WfMonitorScopeServiceImpl#resolve}）。</p>
 *
 * <ul>
 *   <li>{@code defId}：流程定义ID；为 null 表示覆盖全部流程定义。</li>
 *   <li>{@code scopeType}：1=分部 2=部门 3=人员。</li>
 *   <li>{@code scopeValue}：scopeType=3 时为人员ID；否则为部门/分部ID（分部即顶级部门节点）。</li>
 *   <li>{@code monitorUserIds}：被委托人ID列表，逗号分隔。</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_monitor_scope")
@Schema(description = "流程监控委托范围")
public class WfMonitorScope extends TenantEntity {

	private static final long serialVersionUID = 1L;

	@Schema(description = "流程定义ID（null=全部流程）")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long defId;

	@Schema(description = "范围类型 1分部 2部门 3人员")
	private Integer scopeType;

	@Schema(description = "范围值：人员ID 或 部门/分部ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long scopeValue;

	@Schema(description = "被委托人ID列表，逗号分隔")
	private String monitorUserIds;

	@Schema(description = "备注")
	private String remark;

	/** 判断 userId 是否在被委托人列表中（逗号分隔字符串匹配） */
	public boolean containsMonitorUser(Long userId) {
		if (userId == null || monitorUserIds == null || monitorUserIds.isBlank()) {
			return false;
		}
		for (String s : monitorUserIds.split(",")) {
			if (s.trim().equals(userId.toString())) {
				return true;
			}
		}
		return false;
	}

	/** 把 monitorUserIds 解析为 Long 集合（便于前端回填多选） */
	public Set<Long> parseMonitorUserIds() {
		java.util.Set<Long> set = new java.util.LinkedHashSet<>();
		if (monitorUserIds != null && !monitorUserIds.isBlank()) {
			for (String s : monitorUserIds.split(",")) {
				String t = s.trim();
				if (!t.isEmpty()) {
					try {
						set.add(Long.valueOf(t));
					} catch (NumberFormatException ignored) {
						// 跳过非法片段
					}
				}
			}
		}
		return set;
	}
}
