package org.springblade.workflow.enums;

/**
 * 流程推进来源。
 *
 * <p>用于区分「本次推进实例」是发起 / 提交 / 退回，使运行时能按来源施加不同策略。
 * 首个消费方是「流程异常处理」（{@code settings.exceptionHandle}）：该兜底
 * <b>只在提交链路生效、退回链路不生效</b>，对齐 ecology
 * {@code RequestNodeFlow} 中「以下几种情况忽略异常处理设置：退回、分叉合并点、分支第一个分叉中间点」的判定。</p>
 *
 * <p>设计为「带默认值」的枚举：{@link #of(AdvanceSrc)} 把 null 归一为 {@link #SUBMIT}，
 * 这样历史调用点（不带来源的 1/2/4 参 {@code advance} 重载）行为完全不变。</p>
 */
public enum AdvanceSrc {

	/** 发起流程后的首次推进 */
	START,

	/** 提交 / 同意 / 自动通过 / 测试推进 */
	SUBMIT,

	/** 退回（引擎 token 已由 {@code moveActivity} 移到目标节点后的同步推进） */
	REJECT;

	/** null 归一为 {@link #SUBMIT}，保证既有调用方行为不变 */
	public static AdvanceSrc of(AdvanceSrc src) {
		return src == null ? SUBMIT : src;
	}

	/** 是否退回来源（异常兜底据此跳过） */
	public boolean isReject() {
		return this == REJECT;
	}
}
