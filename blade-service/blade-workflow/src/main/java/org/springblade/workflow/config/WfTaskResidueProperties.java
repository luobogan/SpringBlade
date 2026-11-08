package org.springblade.workflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 「读 wf_task 的 blade 扩展任务」开关（去 {@code wf_ 表} · ④ 定案）。
 *
 * <p><b>④ 定案后的语义（2026-11-08）</b>：本开关<b>不再</b>表示「要不要兜底 ACT 未覆盖的残留」，
 * 而是表示「要不要读 <b>blade 扩展任务</b>」——
 * ① 协办(7)/抄送(8)/传阅(11)：blade 业务产物，Flowable 引擎无原生物，{@code ACT_*} 表达不了
 * （抄送历史更是以 {@code wf_task} 行为权威存储，{@code ACT_RU_IDENTITYLINK} 任务完成后会被清理）；
 * ② 无 {@code engineTaskId} 的合成待办：草稿待办（已由「方案 A」移出待办，改由「我的请求」承载）、
 * 退回发起人重提交（运行中实例的正常待办，<b>必须在待办列表里</b>）。</p>
 *
 * <p><b>判据是「精确」的而非「兜底」</b>：列表与角标只额外读<b>上述两类</b>，
 * 引擎任务本体（0待办/2已办/4办结/6自动提交）一律以 {@code ACT_*} 为准。
 * 旧判据「ACT 未覆盖」会把任何漏双写的行都补进来 → 既掩盖真实覆盖率缺口、
 * 又让本开关退化成「应急兜底」，无法长期维护。</p>
 *
 * <p><b>开关取值</b>：</p>
 * <ul>
 *   <li>{@code true}（默认，<b>生产安全值</b>）：列表与角标 = ACT（引擎任务本体）+ wf_task（blade 扩展任务）。
 *       缺配置时读到扩展任务 → 不丢行；</li>
 *   <li>{@code false}（<b>仅诊断用</b>）：完全不查 {@code wf_task}，列表与角标 100% 来自 {@code ACT_*}。
 *       ⚠️ 生产环境禁止置 false —— 协办/抄送/传阅/退回发起人重提交会全部从列表消失
 *       （dev 恰好无这些行故看不出丢行，生产必有）。</li>
 * </ul>
 *
 * <p><b>为什么列表与角标必须同开关</b>：角标走 {@code WfTaskActReader#countFromAct}，
 * 该方法自带扩展任务计数。若只关列表不关角标，会出现「角标 25、列表只有 24」的割裂。
 * 故两处由同一个开关统一门控。</p>
 *
 * <p>注意：Nacos {@code blade-workflow-dev.yaml} 若配了该键以 Nacos 为准（届时需同步）。
 * 本开关只管<b>读侧</b>，<b>不控制 {@code wf_task} 的写入</b>——写入退役已<b>暂缓</b>
 * （见 {@code WfTask} 实体类注释的「表退役暂缓」段）。</p>
 */
@Component
public class WfTaskResidueProperties {

	/**
	 * 是否启用「读 wf_task 的 blade 扩展任务」（true=启用；false=纯 ACT 单源）。
	 *
	 * <p><b>【④ 定案后的语义】</b>本开关自 2026-11-08 起<b>不再</b>表示「要不要兜底残留」，
	 * 而是表示「要不要读 <b>blade 扩展任务</b>」—— 即协办(7)/抄送(8)/传阅(11) 与
	 * 无引擎任务的合成待办（草稿待办、退回发起人重提交）。
	 * 这三类是 <b>blade 业务产物，引擎无原生物</b>，{@code ACT_*} 表达不了（抄送历史更是以
	 * {@code wf_task} 行为权威存储，{@code ACT_RU_IDENTITYLINK} 任务完成后会被清理）
	 * → 关闭它们 = 协办/抄送/传阅/退回发起人重提交全部从列表消失。</p>
	 *
	 * <p><b>故默认 true（生产安全值）</b>：缺配置时读到扩展任务 → 不丢行；
	 * 显式置 false 仅供<b>诊断</b>（临时切纯 ACT 单源、观察列表条数差异以暴露覆盖率缺口），
	 * <b>生产环境禁止置 false</b>。</p>
	 *
	 * <p>字段初值与配置默认值同为 {@code true}：本类可能被 {@code new WfTaskResidueProperties()}
	 * 兜底实例化（未走 Spring 装配时），两者不一致会让「读扩展任务」被静默关闭。</p>
	 */
	@Value("${blade.workflow.task-residue-merge.enabled:true}")
	private boolean enabled = true;

	/** 是否启用 wf_task 兜底残留合并。 */
	public boolean isEnabled() {
		return enabled;
	}

}
