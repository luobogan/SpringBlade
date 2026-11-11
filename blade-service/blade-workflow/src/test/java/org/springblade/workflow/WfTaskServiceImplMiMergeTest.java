package org.springblade.workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.service.impl.WfTaskServiceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link WfTaskServiceImpl#mergeMiAssignees(Object, List)} 的纯函数单测（H2 无关、无需 Spring）。
 *
 * <p>守护 D17/V6 补偿：驳回/重入（reject / doApprove）经 {@code injectMiCollectionVars} 重入 MI 节点时，
 * 不得把运行期加签人（addSign 经 appendMiAssignee 写回的人）按配置覆盖抹掉。</p>
 *
 * <p>背景：{@code WfAddSignMiSpikeTest.writeBackCollectionKeepsAddedAssigneeAfterReject} 曾用
 * 空 vars 调 moveActivity 给出「写回可保人」的<b>假阳性</b>——真实 reject 会传非空（纯配置）vars 触发覆盖。
 * 本测试直接锁死合并语义。</p>
 */
class WfTaskServiceImplMiMergeTest {

	@Test
	@DisplayName("已有运行期集合含加签人 → 合并后保留加签人（核心回归：加签→驳回→重提不丢人）")
	void preservesAddedSigneeFromExisting() {
		// 运行期变量（addSign 写回后）：原 279 + 加签 676
		List<Object> existing = List.of(279L, 676L);
		// 配置操作者（节点配置，仅原办理人）
		List<Long> config = List.of(279L);

		List<Object> merged = WfTaskServiceImpl.mergeMiAssignees(existing, config);

		assertThat(merged).containsExactlyInAnyOrder(279L, 676L);
	}

	@Test
	@DisplayName("变量尚不存在（发起首入）→ 退化为纯配置")
	void freshInstanceUsesConfigOnly() {
		List<Object> merged = WfTaskServiceImpl.mergeMiAssignees(null, List.of(279L));
		assertThat(merged).containsExactly(279L);
	}

	@Test
	@DisplayName("配置新增了操作者 → 补入，且不丢弃已有运行期办理人")
	void addsNewConfigAssigneeWithoutDroppingExisting() {
		List<Object> existing = List.of(279L);
		List<Long> config = List.of(279L, 676L);

		List<Object> merged = WfTaskServiceImpl.mergeMiAssignees(existing, config);

		assertThat(merged).containsExactlyInAnyOrder(279L, 676L);
	}

	@Test
	@DisplayName("现有变量为 String 形态也按值判等，不重复、不丢失")
	void toleratesStringTypedExisting() {
		List<Object> existing = List.of("279", "676");
		List<Long> config = List.of(279L);

		List<Object> merged = WfTaskServiceImpl.mergeMiAssignees(existing, config);

		assertThat(merged).containsExactlyInAnyOrder("279", "676");
	}

	@Test
	@DisplayName("配置为空 → 原样保留已有运行期办理人，不抹成空集合")
	void emptyConfigKeepsExisting() {
		List<Object> existing = List.of(279L, 676L);
		List<Object> merged = WfTaskServiceImpl.mergeMiAssignees(existing, List.of());
		assertThat(merged).containsExactlyInAnyOrder(279L, 676L);
	}
}
