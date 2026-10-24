package org.springblade.workflow.service.impl;

import org.junit.jupiter.api.Test;
import org.springblade.core.tool.api.R;
import org.springblade.system.user.feign.IUserClient;
import org.springblade.workflow.entity.WfMonitorScope;
import org.springblade.workflow.mapper.WfMonitorScopeMapper;
import org.springblade.workflow.vo.MonitorScopeView;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link WfMonitorScopeServiceImpl#resolve(Long)} 与 {@code normalize} 回归：
 * 委托人范围解析（设计文档 §1.3）的核心逻辑，纯单测（Mockito 桩，无 DB / 无引擎）。
 *
 * <p>覆盖：人员范围→发起人集合、定义限制集合、部门范围经用户中心展开为人员、非委托人返回空、
 * 多范围累积、以及 {@code defId/scopeValue} 哨兵归一化（{@code -1/≤0 → null}）。</p>
 */
class WfMonitorScopeResolveTest {

	/** 构造一条委托范围 */
	private static WfMonitorScope scope(Long defId, Integer type, Long value, String monitorUserIds) {
		WfMonitorScope s = new WfMonitorScope();
		s.setId(1L);
		s.setDefId(defId);
		s.setScopeType(type);
		s.setScopeValue(value);
		s.setMonitorUserIds(monitorUserIds);
		return s;
	}

	private static WfMonitorScopeServiceImpl svc(WfMonitorScopeMapper mapper, IUserClient client) {
		return new WfMonitorScopeServiceImpl(mapper, client);
	}

	// ───────────────────────── 人员范围（type=3）─────────────────────────

	@Test
	void resolve_personScope_allDefs() {
		WfMonitorScopeMapper mapper = mock(WfMonitorScopeMapper.class);
		IUserClient client = mock(IUserClient.class);
		when(mapper.selectList(any())).thenReturn(
			List.of(scope(null, 3, 3L, "4")));

		MonitorScopeView v = svc(mapper, client).resolve(4L);

		assertThat(v.isHasAny()).isTrue();
		assertThat(v.isAllDefs()).isTrue();                 // defId=null → 全部流程
		assertThat(v.getStarterIds()).containsExactly(3L);  // 人员范围直接作为发起人
		assertThat(v.getDefIds()).isEmpty();
	}

	@Test
	void resolve_personScope_defRestriction() {
		WfMonitorScopeMapper mapper = mock(WfMonitorScopeMapper.class);
		IUserClient client = mock(IUserClient.class);
		when(mapper.selectList(any())).thenReturn(
			List.of(scope(5L, 3, 3L, "4")));

		MonitorScopeView v = svc(mapper, client).resolve(4L);

		assertThat(v.isHasAny()).isTrue();
		assertThat(v.isAllDefs()).isFalse();
		assertThat(v.getDefIds()).containsExactly(5L);      // 仅授权定义 5
		assertThat(v.getStarterIds()).containsExactly(3L);
	}

	// ───────────────────────── 部门/分部范围（type=1/2）→ 经用户中心展开 ─────────────────────────

	@Test
	void resolve_deptScope_expandsToUsers() {
		WfMonitorScopeMapper mapper = mock(WfMonitorScopeMapper.class);
		IUserClient client = mock(IUserClient.class);
		when(mapper.selectList(any())).thenReturn(
			List.of(scope(null, 2, 10L, "4")));
		when(client.userIdsByDept(10L, true)).thenReturn(R.data(Arrays.asList(7L, 8L)));

		MonitorScopeView v = svc(mapper, client).resolve(4L);

		assertThat(v.isHasAny()).isTrue();
		assertThat(v.isAllDefs()).isTrue();
		assertThat(v.getStarterIds()).containsExactlyInAnyOrder(7L, 8L); // 部门展开为具体人员
	}

	@Test
	void resolve_deptScope_userClientFails_shouldNotThrow() {
		WfMonitorScopeMapper mapper = mock(WfMonitorScopeMapper.class);
		IUserClient client = mock(IUserClient.class);
		when(mapper.selectList(any())).thenReturn(
			List.of(scope(null, 2, 10L, "4")));
		when(client.userIdsByDept(10L, true)).thenThrow(new RuntimeException("feign down"));

		MonitorScopeView v = svc(mapper, client).resolve(4L);

		// 用户中心异常被吞：该范围不贡献发起人，但不影响其他范围/不抛错
		assertThat(v.isHasAny()).isTrue();
		assertThat(v.getStarterIds()).isEmpty();
	}

	// ───────────────────────── 非委托人 / 空 ─────────────────────────

	@Test
	void resolve_notDelegate_returnsEmpty() {
		WfMonitorScopeMapper mapper = mock(WfMonitorScopeMapper.class);
		IUserClient client = mock(IUserClient.class);
		when(mapper.selectList(any())).thenReturn(
			List.of(scope(null, 3, 3L, "9"))); // 委托人里没有 4

		MonitorScopeView v = svc(mapper, client).resolve(4L);

		assertThat(v.isHasAny()).isFalse();
		assertThat(v.getStarterIds()).isEmpty();
		assertThat(v.getDefIds()).isEmpty();
	}

	@Test
	void resolve_nullUser_returnsEmpty() {
		WfMonitorScopeMapper mapper = mock(WfMonitorScopeMapper.class);
		IUserClient client = mock(IUserClient.class);
		when(mapper.selectList(any())).thenReturn(
			List.of(scope(null, 3, 3L, "4")));

		MonitorScopeView v = svc(mapper, client).resolve(null);

		assertThat(v.isHasAny()).isFalse();
		assertThat(v.getStarterIds()).isEmpty();
	}

	// ───────────────────────── 多范围累积 ─────────────────────────

	@Test
	void resolve_multipleScopes_accumulates() {
		WfMonitorScopeMapper mapper = mock(WfMonitorScopeMapper.class);
		IUserClient client = mock(IUserClient.class);
		when(mapper.selectList(any())).thenReturn(List.of(
			scope(5L, 3, 3L, "4"),     // 人员 3 + 定义 5
			scope(null, 3, 7L, "4")     // 人员 7 + 全部定义
		));

		MonitorScopeView v = svc(mapper, client).resolve(4L);

		assertThat(v.isHasAny()).isTrue();
		assertThat(v.isAllDefs()).isTrue();  // 任一范围 defId=null → 全部
		assertThat(v.getStarterIds()).containsExactlyInAnyOrder(3L, 7L);
		assertThat(v.getDefIds()).containsExactly(5L);
	}

	// ───────────────────────── 哨兵归一化（saveScope 入口）─────────────────────────

	@Test
	void normalize_saveScope_sentinelToNull() {
		WfMonitorScopeMapper mapper = mock(WfMonitorScopeMapper.class);
		IUserClient client = mock(IUserClient.class);
		when(mapper.insert(any(WfMonitorScope.class))).thenReturn(1);

		WfMonitorScope s = scope(-1L, 3, -1L, "4"); // -1 哨兵
		boolean ok = svc(mapper, client).saveScope(s);

		assertThat(ok).isTrue();
		assertThat(s.getDefId()).isNull();       // -1 → null（全部流程）
		assertThat(s.getScopeValue()).isNull();  // -1 → null（未设置范围值）
	}

	@Test
	void normalize_saveScope_keepsRealValues() {
		WfMonitorScopeMapper mapper = mock(WfMonitorScopeMapper.class);
		IUserClient client = mock(IUserClient.class);
		when(mapper.insert(any(WfMonitorScope.class))).thenReturn(1);

		WfMonitorScope s = scope(5L, 3, 3L, "4"); // 真实值
		svc(mapper, client).saveScope(s);

		assertThat(s.getDefId()).isEqualTo(5L);  // 原样保留
		assertThat(s.getScopeValue()).isEqualTo(3L);
	}
}
