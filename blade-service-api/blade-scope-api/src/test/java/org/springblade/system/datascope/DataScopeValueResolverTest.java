package org.springblade.system.datascope;

import org.junit.jupiter.api.Test;
import org.springblade.core.secure.BladeUser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DataScopeValueResolver} 语义固化测试。
 *
 * <p>目的：把「与上游 {@code BladeDataScopeHandler#sqlCondition} 对齐」的关键口径钉死，
 * 防止后续改动再次漂移。历史上已发生过的三类漂移：
 * 多部门丢失（scope_type=3/4 只取单个部门）、自定义值域解析失败即 fail-open（越权）、
 * 未知 scope_type 被放过（等于没配权限）。</p>
 *
 * <p>纯单测：无 Spring 容器、无 DB、无 Feign。</p>
 */
class DataScopeValueResolverTest {

	/** 构造测试用户 */
	private static BladeUser user(Long userId, String deptId, String roleName) {
		BladeUser user = new BladeUser();
		user.setUserId(userId);
		user.setDeptId(deptId);
		user.setRoleName(roleName);
		user.setAccount("tester");
		return user;
	}

	// ───────────────────────── 不过滤 ─────────────────────────

	@Test
	void scopeTypeAll_unrestricted() {
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			1, "dept_id", null, user(59L, "100", "normal"), null);

		assertThat(value.isUnrestricted()).isTrue();
		assertThat(value.isRejected()).isFalse();
	}

	@Test
	void adminRole_unrestricted() {
		// 管理员角色不套行级过滤（与上游一致）
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			2, "create_user", null, user(59L, "100", "administrator"), null);

		assertThat(value.isUnrestricted()).isTrue();
	}

	// ───────────────────────── 本人 / 本部门 ─────────────────────────

	@Test
	void scopeTypeOwn_filterByUserId() {
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			2, "create_user", null, user(59L, "100", "normal"), null);

		assertThat(value.isRestricted()).isTrue();
		assertThat(value.getColumn()).isEqualTo("create_user");
		assertThat(value.getIds()).containsExactly(59L);
	}

	@Test
	void scopeTypeOwnDept_singleDept() {
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			3, "dept_id", null, user(59L, "100", "normal"), null);

		assertThat(value.isRestricted()).isTrue();
		assertThat(value.getIds()).containsExactly(100L);
	}

	@Test
	void scopeTypeOwnDept_multiDeptNotLost() {
		// 上游用 Func.toLongList(deptId)：deptId 形如 "100,200" 表示多部门，不得只取第一个
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			3, "dept_id", null, user(59L, "100,200", "normal"), null);

		assertThat(value.isRestricted()).isTrue();
		assertThat(value.getIds()).containsExactly(100L, 200L);
	}

	@Test
	void scopeTypeOwnDeptChild_includesDescendants() {
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			4, "dept_id", null, user(59L, "100", "normal"),
			deptId -> Arrays.asList(101L, 102L));

		assertThat(value.isRestricted()).isTrue();
		assertThat(value.getIds()).containsExactly(100L, 101L, 102L);
	}

	@Test
	void scopeTypeOwnDeptChild_deduplicate() {
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			4, "dept_id", null, user(59L, "100", "normal"),
			deptId -> Arrays.asList(100L, 101L));

		assertThat(value.getIds()).containsExactly(100L, 101L);
	}

	// ───────────────────────── 自定义值域（受限形态）─────────────────────────

	@Test
	void scopeTypeCustom_inList() {
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			5, null, "where scope.hrm_id in (1,2)", user(59L, "100", "normal"), null);

		assertThat(value.isRestricted()).isTrue();
		assertThat(value.getColumn()).isEqualTo("hrm_id");
		assertThat(value.getIds()).containsExactly(1L, 2L);
	}

	@Test
	void scopeTypeCustom_eqSingle() {
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			5, null, "scope.hrm_id = 5", user(59L, "100", "normal"), null);

		assertThat(value.isRestricted()).isTrue();
		assertThat(value.getColumn()).isEqualTo("hrm_id");
		assertThat(value.getIds()).containsExactly(5L);
	}

	@Test
	void scopeTypeCustom_placeholderResolved() {
		// 占位符 ${userId} 用当前用户解析（与上游 BladeDataScopeHandler 同源）
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			5, null, "where scope.hrm_id in (${userId})", user(59L, "100", "normal"), null);

		assertThat(value.isRestricted()).isTrue();
		assertThat(value.getIds()).containsExactly(59L);
	}

	@Test
	void scopeTypeCustom_illegalShape_rejected() {
		// 不是受限形态 → 拒绝（绝不 fail-open 放行全部）
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			5, null, "1=1 or 1=1", user(59L, "100", "normal"), null);

		assertThat(value.isRejected()).isTrue();
		assertThat(value.getIds()).isEmpty();
	}

	@Test
	void scopeTypeCustom_injectionShape_rejected() {
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			5, null, "where scope.hrm_id in (1) or 1=1", user(59L, "100", "normal"), null);

		assertThat(value.isRejected()).isTrue();
	}

	@Test
	void scopeTypeCustom_blankValue_rejected() {
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			5, null, "   ", user(59L, "100", "normal"), null);

		assertThat(value.isRejected()).isTrue();
	}

	// ───────────────────────── 拒绝路径 ─────────────────────────

	@Test
	void scopeTypeUnknown_rejected() {
		// 未知类型：上游会拼出 in () 让 SQL 报错（fail-closed），这里显式拒绝
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			99, "dept_id", null, user(59L, "100", "normal"), null);

		assertThat(value.isRejected()).isTrue();
		assertThat(value.getReason()).contains("99");
	}

	@Test
	void scopeTypeNull_rejected() {
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			null, "dept_id", null, user(59L, "100", "normal"), null);

		assertThat(value.isRejected()).isTrue();
	}

	@Test
	void userNull_rejected() {
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			2, "create_user", null, null, null);

		assertThat(value.isRejected()).isTrue();
	}

	@Test
	void blankScopeColumn_rejected() {
		// 没有过滤列就无法构造行级条件 → 拒绝（上游拼出的 SQL 同样会报错）
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			2, "  ", null, user(59L, "100", "normal"), null);

		assertThat(value.isRejected()).isTrue();
	}

	@Test
	void blankDeptId_rejected() {
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			3, "dept_id", null, user(59L, null, "normal"), null);

		assertThat(value.isRejected()).isTrue();
	}

	@Test
	void descendantProviderNull_stillKeepsOwnDept() {
		// 子孙提供器不可用时退化为「仅本部门」，不放开成「全部」
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			4, "dept_id", null, user(59L, "100", "normal"), null);

		assertThat(value.isRestricted()).isTrue();
		assertThat(value.getIds()).containsExactly(100L);
	}

	@Test
	void idsAlwaysImmutableAndNonNull() {
		DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
			2, "create_user", null, user(59L, "100", "normal"), null);

		assertThat(value.getIds()).isNotNull();
		assertThat(Collections.unmodifiableList(value.getIds())).isNotNull();
		List<Long> ids = value.getIds();
		assertThat(ids).hasSize(1);
	}
}
