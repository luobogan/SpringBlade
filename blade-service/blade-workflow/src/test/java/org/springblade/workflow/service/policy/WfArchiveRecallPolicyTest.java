package org.springblade.workflow.service.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link WfArchiveRecallPolicy} 解析与判定口径回归（纯单测，无 DB / 无引擎）。
 *
 * <p>覆盖本次「归档管理 / 撤回管理」落地的关键口径：</p>
 * <ul>
 *   <li><b>未配置 vs 显式关闭</b>：既有能力（允许强制归档 / 允许强制收回 / 允许监控回退）
 *       未配置时缺省<b>开启</b>（保持存量流程原有行为），只有显式 {@code false} 才关闭；
 *       全新能力（归档后强制收回 / 归档后流程干预）缺省<b>关闭</b>，必须显式开启；</li>
 *   <li>非法 JSON 一律降级为「全关」且<b>不抛异常</b>（监控列表批量读取，不能因一条脏配置打断渲染）；</li>
 *   <li>可归档节点范围：{@code ["*"]} 通配 / nodeKey 白名单 / 空范围=不限制；</li>
 *   <li>强制收回落点范围 {@code scopeAllows}：create / history / all / specified 四种语义。</li>
 * </ul>
 */
class WfArchiveRecallPolicyTest {

	// ───────────────────────── 未配置（存量流程）─────────────────────────

	@Test
	void unconfigured_shouldKeepLegacyActionsEnabledAndNewActionsDisabled() {
		WfArchiveRecallPolicy policy = WfArchiveRecallPolicy.empty();

		// 既有能力：缺省开启 → 存量流程的强制归档 / 强制收回 / 监控回退不受影响
		assertThat(policy.isForceArchiveEnabled()).isTrue();
		assertThat(policy.isRecallEnabled()).isTrue();
		assertThat(policy.isRejectEnabled()).isTrue();
		// 全新能力：缺省关闭 → 归档后收回 / 干预必须显式开启
		assertThat(policy.isRecallFromArchiveEnabled()).isFalse();
		assertThat(policy.isInterveneFromArchiveEnabled()).isFalse();
		// 未配置标记
		assertThat(policy.isArchiveConfigured()).isFalse();
		assertThat(policy.isRecallConfigured()).isFalse();
	}

	@Test
	void nullOrBlankJson_shouldBehaveAsUnconfigured() {
		assertThat(WfArchiveRecallPolicy.of(null, null).isForceArchiveEnabled()).isTrue();
		assertThat(WfArchiveRecallPolicy.of("", "   ").isRecallEnabled()).isTrue();
		assertThat(WfArchiveRecallPolicy.of(null, null).isRecallFromArchiveEnabled()).isFalse();
	}

	@Test
	void malformedJson_shouldDegradeWithoutThrowing() {
		assertThatCode(() -> WfArchiveRecallPolicy.of("{not-json", "]["))
			.doesNotThrowAnyException();
		WfArchiveRecallPolicy policy = WfArchiveRecallPolicy.of("{not-json", "][");
		// 解析失败 → 视为「未配置」：既有能力保持开启（不因一条脏配置打断存量流程），
		// 全新能力保持关闭（须显式开启才可用）
		assertThat(policy.isForceArchiveEnabled()).isTrue();
		assertThat(policy.isRecallEnabled()).isTrue();
		assertThat(policy.isRejectEnabled()).isTrue();
		assertThat(policy.isRecallFromArchiveEnabled()).isFalse();
		assertThat(policy.isInterveneFromArchiveEnabled()).isFalse();
		assertThat(policy.isArchiveConfigured()).isFalse();
		assertThat(policy.isRecallConfigured()).isFalse();
	}

	// ───────────────────────── 显式配置 ─────────────────────────

	@Test
	void explicitFalse_shouldDisableLegacyAction() {
		String archive = "{\"forceArchiveEnabled\":false,\"recallFromArchiveEnabled\":true}";
		WfArchiveRecallPolicy policy = WfArchiveRecallPolicy.of(archive, null);

		assertThat(policy.isForceArchiveEnabled()).isFalse();
		assertThat(policy.isRecallFromArchiveEnabled()).isTrue();
		assertThat(policy.isArchiveConfigured()).isTrue();
	}

	@Test
	void explicitTrue_shouldEnableArchiveActions() {
		String archive = "{\"recallFromArchiveEnabled\":true,\"interveneFromArchiveEnabled\":true}";
		WfArchiveRecallPolicy policy = WfArchiveRecallPolicy.of(archive, null);

		assertThat(policy.isRecallFromArchiveEnabled()).isTrue();
		assertThat(policy.isInterveneFromArchiveEnabled()).isTrue();
	}

	@Test
	void stringBooleans_shouldBeAccepted() {
		// 兼容手写 JSON："true" / "false" 字符串写法
		WfArchiveRecallPolicy policy = WfArchiveRecallPolicy.of(
			"{\"forceArchiveEnabled\":\"false\",\"recallFromArchiveEnabled\":\"true\"}", null);
		assertThat(policy.isForceArchiveEnabled()).isFalse();
		assertThat(policy.isRecallFromArchiveEnabled()).isTrue();
	}

	@Test
	void recallManage_shouldParseScopeAndSpecifiedNodes() {
		String recall = "{\"recallEnabled\":true,\"rejectEnabled\":false,\"remindSelect\":true,"
			+ "\"recallTargetScope\":\"specified\",\"recallTargetNodeKeys\":[\"n1\",\"n2\"]}";
		WfArchiveRecallPolicy policy = WfArchiveRecallPolicy.of(null, recall);

		assertThat(policy.isRecallEnabled()).isTrue();
		assertThat(policy.isRejectEnabled()).isFalse();
		assertThat(policy.isRemindSelect()).isTrue();
		assertThat(policy.getRecallTargetScope()).isEqualTo(WfArchiveRecallPolicy.SCOPE_SPECIFIED);
		assertThat(policy.getRecallTargetNodeKeys()).containsExactlyInAnyOrder("n1", "n2");
	}

	@Test
	void recallTargetScope_shouldDefaultToAll() {
		// 未配置范围 → all（不改变既有行为：后端另有 nodeType 0/1/2 硬校验兜底）
		assertThat(WfArchiveRecallPolicy.of(null, null).getRecallTargetScope())
			.isEqualTo(WfArchiveRecallPolicy.SCOPE_ALL);
		assertThat(WfArchiveRecallPolicy.of(null, "{}").getRecallTargetScope())
			.isEqualTo(WfArchiveRecallPolicy.SCOPE_ALL);
	}

	// ───────────────────────── 可归档节点范围 ─────────────────────────

	@Test
	void nodeArchiveApplicable_wildcardShouldAllowAnyNode() {
		WfArchiveRecallPolicy policy = WfArchiveRecallPolicy.of(
			"{\"archiveApplicableNodeKeys\":[\"*\"]}", null);
		assertThat(policy.isNodeArchiveApplicable("anyNode")).isTrue();
		assertThat(policy.isNodeArchiveApplicable(null)).isTrue();
	}

	@Test
	void nodeArchiveApplicable_whitelistShouldMatchExactly() {
		WfArchiveRecallPolicy policy = WfArchiveRecallPolicy.of(
			"{\"archiveApplicableNodeKeys\":[\"nodeA\",\"nodeB\"]}", null);
		assertThat(policy.isNodeArchiveApplicable("nodeA")).isTrue();
		assertThat(policy.isNodeArchiveApplicable("nodeB")).isTrue();
		assertThat(policy.isNodeArchiveApplicable("nodeC")).isFalse();
		assertThat(policy.isNodeArchiveApplicable(null)).isFalse();
	}

	@Test
	void nodeArchiveApplicable_emptyRangeShouldNotLimit() {
		// 范围为空 = 未限制（是否允许归档仍由 forceArchiveEnabled 决定）
		WfArchiveRecallPolicy policy = WfArchiveRecallPolicy.of("{}", null);
		assertThat(policy.isNodeArchiveApplicable("anyNode")).isTrue();
	}

	// ───────────────────────── 落点范围裁决 ─────────────────────────

	@Test
	void scopeAllows_create_shouldOnlyAllowCreateNode() {
		assertThat(WfArchiveRecallPolicy.scopeAllows("create", true, false, false)).isTrue();
		assertThat(WfArchiveRecallPolicy.scopeAllows("create", false, true, false)).isFalse();
	}

	@Test
	void scopeAllows_history_shouldAllowCreateAndHandled() {
		assertThat(WfArchiveRecallPolicy.scopeAllows("history", true, false, false)).isTrue();
		assertThat(WfArchiveRecallPolicy.scopeAllows("history", false, true, false)).isTrue();
		assertThat(WfArchiveRecallPolicy.scopeAllows("history", false, false, false)).isFalse();
	}

	@Test
	void scopeAllows_specified_shouldFollowSpecifiedList() {
		assertThat(WfArchiveRecallPolicy.scopeAllows("specified", false, false, true)).isTrue();
		assertThat(WfArchiveRecallPolicy.scopeAllows("specified", true, true, false)).isFalse();
	}

	@Test
	void scopeAllows_allOrUnknown_shouldAllow() {
		assertThat(WfArchiveRecallPolicy.scopeAllows("all", false, false, false)).isTrue();
		assertThat(WfArchiveRecallPolicy.scopeAllows(null, false, false, false)).isTrue();
		assertThat(WfArchiveRecallPolicy.scopeAllows("unknown-scope", false, false, false)).isTrue();
	}
}
