package org.springblade.workflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * `wf_task` 台账写入路径开关（【去 wf_ 表 · 批次 1 / 2026-11-10】）。
 *
 * <p><b>单路径，不是双写</b>：任意时刻只有一个写入路径生效。</p>
 * <ul>
 *   <li>{@code direct}（默认）：任务创建不落 {@code wf_task}，业务列直写
 *       {@code ACT_RU_TASK}/{@code ACT_HI_TASKINST}（{@code WfTaskActDirectWriter}）。</li>
 *   <li>{@code legacy}：回退路径，恢复「落 {@code wf_task} 行 + sync」的旧链路，
 *       用于批次 1 上线异常时秒级回退；回退后可用 {@code WfTaskActRebuilder}
 *       从存量台账行重建 ACT 业务列。</li>
 * </ul>
 *
 * <p>参照 {@code WfTaskResidueProperties} 的写法：代码默认值即安全值，
 * 配置缺失时不改变行为。</p>
 */
@Component
public class WfLedgerModeProperties {

	@Value("${blade.workflow.task-ledger-mode:direct}")
	private String mode;

	public boolean isDirect() {
		return !isLegacy();
	}

	public boolean isLegacy() {
		return "legacy".equalsIgnoreCase(mode);
	}
}
