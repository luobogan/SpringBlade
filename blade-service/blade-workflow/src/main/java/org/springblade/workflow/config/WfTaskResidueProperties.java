package org.springblade.workflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 「wf_task 兜底残留合并」开关（去 {@code wf_ 表} · P6 退役的第一台阶）。
 *
 * <p><b>背景</b>：读源已切 {@code act}（{@code ACT_RU_TASK} / {@code ACT_HI_TASKINST}），
 * 但 {@code ACT} 单行表达不了的行仍需从 {@code wf_task} 补进来，否则会丢单——
 * ① 存量 N:1 会签（同一引擎任务多人，{@code ACT} 只有一行）；
 * ② 孤儿（{@code wf_task} 有引擎任务 ID 但引擎侧已无此任务）；
 * ③ 无 {@code engineTaskId} 的合成待办（草稿待办、退回发起人重提交，<b>永不进 ACT</b>）。
 * 该合并即所谓「兜底残留合并」。</p>
 *
 * <p><b>本开关的语义</b>：</p>
 * <ul>
 *   <li>{@code true}（默认，零行为变化）：待办/已办<b>列表</b>与顶栏<b>角标</b>都合并 {@code wf_task} 残留，
 *       数字与翻源前一致，零丢单；</li>
 *   <li>{@code false}（纯 ACT 单源）：完全不查 {@code wf_task}，列表与角标 100% 来自 {@code ACT_*}。</li>
 * </ul>
 *
 * <p><b>为什么列表与角标必须同开关</b>：角标走 {@code WfTaskActReader#countFromAct}，
 * 该方法自带残留兜底计数。若只关列表不关角标，会出现「角标 25、列表只有 24」的割裂。
 * 故两处由同一个开关统一门控。</p>
 *
 * <p><b>关闭前的硬前置</b>（详见 {@code doc/md/去wf_表-任务读源翻源上线检查.md} §2）：</p>
 * <ol>
 *   <li>活跃 N:1 残留 = 0、孤儿 = 0（孤儿若含 {@code is_test=0} 真实数据须先修复）；</li>
 *   <li>双写漂移 = 0；</li>
 *   <li>ACT 可读行 ≥ {@code wf_task} 列表行 99%（覆盖率硬指标）；</li>
 *   <li><b>业务侧确认</b>：合成待办（草稿待办 / 退回发起人重提交）永远不在 {@code ACT} 里，
 *       关闭后这些行会从待办列表消失——发起人被退回却看不到待办会形成死循环。</li>
 * </ol>
 *
 * <p><b>关闭后的收益</b>：① {@code total} 与实际可翻行数<b>严格一致</b>，消除
 * 「{@code actTotal + residueTotal}」错位导致的深页漏行（归并只在第一页做、深页取 ACT 连续切片的固有缺陷）；
 * ② 真实暴露 ACT 覆盖率缺口——否则名义上读 ACT、实际绕回 {@code wf_task}，既没真正翻源、
 * 又掩盖缺口（这正是 dev 当初回退到 {@code wf} 的原因）。</p>
 *
 * <p>注意：Nacos {@code blade-workflow-dev.yaml} 若配了该键以 Nacos 为准（届时需同步）。
 * 本开关只管「读侧合并」，<b>不控制 {@code wf_task} 的写入</b>（写入退役属 P6，须先解决
 * 合成待办归属与 blade 6 态到 ACT 的映射）。</p>
 */
@Component
public class WfTaskResidueProperties {

	/**
	 * 是否启用 wf_task 兜底残留合并（true=合并并保持现状；false=纯 ACT 单源）。
	 *
	 * <p>字段初值与配置默认值同为 {@code true}：本类可能被 {@code new WfTaskResidueProperties()}
	 * 兜底实例化（未走 Spring 装配时），若初值为 false 会让「合并」被静默关闭 ——
	 * 与本开关「默认零行为变化」的语义相反。</p>
	 */
	@Value("${blade.workflow.task-residue-merge.enabled:true}")
	private boolean enabled = true;

	/** 是否启用 wf_task 兜底残留合并。 */
	public boolean isEnabled() {
		return enabled;
	}

}
