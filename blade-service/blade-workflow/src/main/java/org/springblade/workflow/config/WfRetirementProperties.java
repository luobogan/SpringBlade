package org.springblade.workflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * wf_* 表运行期去依赖主开关（T-14 · 决策 B · 2026-10-01）。
 *
 * <p>决策背景：经全量 Mapper 读写盘点，wf_* 候选表仍被设计期 store、节点测试、回填桥、运行期直读直接主用读写，
 * 仅闸控「回退分支」不足以 DROP；故本期定位为「运行期读源不再静默回退 wf_*」，<b>wf_* 表保留</b>（遗留设计期镜像/测试存储），放弃 DROP。</p>
 *
 * <p>默认 {@code false}：运行期仍允许「BPMN/ACT 读到空 → 回退 wf_*」的兼容行为，零行为变化、可回滚。</p>
 *
 * <p>翻转为 {@code true}（可选加固，<b>非退役</b>）：BPMN/ACT 读空即按空/报错，禁用回退分支——</p>
 * <ul>
 *   <li>各消费方的「BPMN/ACT 读到空 → 回退 wf_process_node/link/operator/timeout/field_perm/detail_perm/detail_filter」分支被禁用；</li>
 *   <li>wf_task / wf_instance 的双写、wf_approval_log 的合并（mergeMissingFromWf）被禁用。</li>
 * </ul>
 * <p>注意：开关<b>只闸控回退分支</b>，设计期 store / 节点测试 / 回填桥对 wf_* 的直接读写不受影响（表仍被写）。</p>
 *
 * <p>前置（翻转 true 时）：草稿/未部署定义必须先部署再发起（前端保证）。</p>
 *
 * <p>注意：Nacos {@code blade-workflow-dev.yaml} 若配了该键以 Nacos 为准（届时需同步）。</p>
 */
@Component
public class WfRetirementProperties {

	@Value("${blade.workflow.wf-table-retirement.enabled:false}")
	private boolean enabled;

	/** 是否已启用退役（BPMN/ACT 唯一源，禁用所有 wf_* 读/写分支）。 */
	public boolean isEnabled() {
		return enabled;
	}

}
