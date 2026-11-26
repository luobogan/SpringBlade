/**
 * Copyright (c) 2018-2099, Chill Zhuang 庄骞 (bladejava@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.system.datascope;

import org.springblade.core.datascope.enums.DataScopeEnum;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.PlaceholderUtil;
import org.springblade.core.tool.utils.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据权限「值域」解析器。
 *
 * <p><b>为什么存在</b>：上游 {@code blade-starter-datascope} 把 scope_type 的值域语义写死在
 * {@code BladeDataScopeHandler#sqlCondition}（外部 jar，无法改动），动态 SQL 链路
 * （formmode 的 {@code JdbcTemplate}、后续文档中心等）拿不到也复用不了；
 * 本项目此前在 formmode 内另写了一套，导致两处口径漂移：多部门丢失、
 * 自定义值域解析失败即 fail-open（越权）、部门子孙匹配漏掉直属子部门。
 * 本类把语义收敛到<b>唯一一处</b>，供所有动态 SQL 链路复用。</p>
 *
 * <p><b>与上游逐条对齐</b>（依据 blade-starter-datascope 5.0.2 反编译结果）：</p>
 * <ul>
 *   <li>1 全部 / 管理员角色 → 不过滤（上游直接 return null）</li>
 *   <li>2 本人 → 当前用户 id</li>
 *   <li>3 本部门 → 当前部门 id（<b>逗号分隔时取多个</b>，上游用 {@code Func.toLongList}）</li>
 *   <li>4 本部门及子级 → 本部门 + 其子孙部门（子孙由调用方提供）</li>
 *   <li>5 自定义 → 先用当前用户解析 {@code ${...}} 占位符，再按下述受限语法解析</li>
 * </ul>
 *
 * <p><b>与上游的有意差异</b>（均朝更严格的方向）：</p>
 * <ol>
 *   <li>scope_type=5：上游把 {@code scope_value} 原样拼成 SQL 片段；动态 SQL 链路不能照做
 *       （拼进去就是注入面），这里只接受「列 IN (纯数字列表)」与「列 = 数字」两种受限形态。
 *       <b>解析不出来 → 拒绝（REJECTED）而不是放行全部</b>：放行全部会让
 *       「以为配了权限」变成「实际没限制」，属于越权。</li>
 *   <li>未知 / 空 scope_type：上游会拼出 {@code in ()} 让 SQL 报错（fail-closed），
 *       这里显式返回 REJECTED（同样 fail-closed，但不抛 500）。</li>
 * </ol>
 *
 * @author BladeX
 */
public final class DataScopeValueResolver {

	/**
	 * 自定义值域受限形态之一：{@code [where|and] [scope.] 列 IN (纯数字列表)}
	 */
	private static final Pattern CUSTOM_IN = Pattern.compile(
		"^(?:(?:where|and)\\s+)?(?:scope\\.)?([A-Za-z_][A-Za-z0-9_$]*)\\s+in\\s*\\(\\s*(\\d+(?:\\s*,\\s*\\d+)*)\\s*\\)$",
		Pattern.CASE_INSENSITIVE);

	/**
	 * 自定义值域受限形态之二：{@code [where|and] [scope.] 列 = 数字}
	 */
	private static final Pattern CUSTOM_EQ = Pattern.compile(
		"^(?:(?:where|and)\\s+)?(?:scope\\.)?([A-Za-z_][A-Za-z0-9_$]*)\\s*=\\s*(\\d+)$",
		Pattern.CASE_INSENSITIVE);

	private DataScopeValueResolver() {
	}

	/**
	 * 部门子孙提供器（由调用方实现：数据库查询 / 远程接口 / 缓存均可）。
	 */
	@FunctionalInterface
	public interface DeptDescendantProvider {

		/**
		 * 查询某部门的全部子孙部门 id。
		 *
		 * @param deptId 部门 id
		 * @return 子孙部门 id 列表（无则返回空列表，不可为 null）
		 */
		List<Long> descendants(Long deptId);
	}

	/**
	 * 值域解析结果。
	 */
	public static final class ScopeValue {

		/**
		 * 解析结果类型。
		 */
		public enum Kind {
			/**
			 * 不过滤（全部数据 / 管理员）
			 */
			UNRESTRICTED,
			/**
			 * 按值域过滤
			 */
			RESTRICTED,
			/**
			 * 规则非法或无法安全解析 → 拒绝（等价于无可见数据）
			 */
			REJECTED
		}

		private final Kind kind;
		private final String column;
		private final List<Long> ids;
		private final String reason;

		private ScopeValue(Kind kind, String column, List<Long> ids, String reason) {
			this.kind = kind;
			this.column = column;
			this.ids = ids == null ? Collections.emptyList() : Collections.unmodifiableList(ids);
			this.reason = reason;
		}

		/**
		 * 构造「不过滤」结果。
		 */
		public static ScopeValue unrestricted() {
			return new ScopeValue(Kind.UNRESTRICTED, null, Collections.emptyList(), null);
		}

		/**
		 * 构造「按值域过滤」结果。
		 *
		 * @param column 行级过滤列
		 * @param ids    值域（空列表表示无可见数据）
		 */
		public static ScopeValue restricted(String column, List<Long> ids) {
			return new ScopeValue(Kind.RESTRICTED, column, ids, null);
		}

		/**
		 * 构造「拒绝」结果。
		 *
		 * @param reason 拒绝原因（用于日志排障）
		 */
		public static ScopeValue rejected(String reason) {
			return new ScopeValue(Kind.REJECTED, null, Collections.emptyList(), reason);
		}

		public Kind getKind() {
			return kind;
		}

		public String getColumn() {
			return column;
		}

		public List<Long> getIds() {
			return ids;
		}

		public String getReason() {
			return reason;
		}

		public boolean isUnrestricted() {
			return Kind.UNRESTRICTED == kind;
		}

		public boolean isRestricted() {
			return Kind.RESTRICTED == kind;
		}

		public boolean isRejected() {
			return Kind.REJECTED == kind;
		}
	}

	/**
	 * 解析 scope_type 对应的值域。
	 *
	 * <p>调用方拿到 REJECTED 时<b>必须</b>按「无可见数据」处理（安全优先），
	 * 不得降级为「不过滤」——否则配了权限等于没配。</p>
	 *
	 * @param scopeType        数据权限类型：1全部 2本人 3本部门 4本部门及子级 5自定义
	 * @param scopeColumn      行级过滤列（scope_type=5 时可为空，由自定义片段自带列名）
	 * @param scopeValue       自定义值域（scope_type=5 时使用，其余忽略）
	 * @param user             当前登录用户
	 * @param descendantProvider 部门子孙提供器（scope_type=4 时使用，可为 null）
	 * @return 值域解析结果
	 */
	public static ScopeValue resolve(Integer scopeType, String scopeColumn, String scopeValue,
									 BladeUser user, DeptDescendantProvider descendantProvider) {
		return resolve(scopeType, scopeColumn, scopeValue, user, descendantProvider, true);
	}

	/**
	 * 解析值域；{@code trustRoleName} 为 false 时不按 token 里的 roleName 放行管理员。
	 *
	 * <p>表单建模权限链路外层已按真实角色 ID 判定管理员，token 中可能残留历史
	 * roleName（如 yjl 的 roleId 为空但 roleName 仍含 administrator），此处再按名称放行
	 * 会导致所有数据范围被绕过。</p>
	 */
	public static ScopeValue resolve(Integer scopeType, String scopeColumn, String scopeValue,
									 BladeUser user, DeptDescendantProvider descendantProvider,
									 boolean trustRoleName) {
		if (user == null) {
			return ScopeValue.rejected("未获取到当前登录用户");
		}
		DataScopeEnum type = DataScopeEnum.of(scopeType);
		// 全部数据 / 管理员：与上游一致，直接不过滤
		if (type == DataScopeEnum.ALL) {
			return ScopeValue.unrestricted();
		}
		if (trustRoleName && StringUtil.containsAny(user.getRoleName(), RoleConstant.ADMIN)) {
			return ScopeValue.unrestricted();
		}
		if (type == DataScopeEnum.OWN) {
			Long userId = user.getUserId();
			if (userId == null) {
				return ScopeValue.rejected("scope_type=2（本人）但当前用户 id 为空");
			}
			String column = requireColumn(scopeColumn, type);
			return column == null
				? ScopeValue.rejected("scope_type=2（本人）但 scopeColumn 为空")
				: ScopeValue.restricted(column, Collections.singletonList(userId));
		}
		if (type == DataScopeEnum.OWN_DEPT) {
			// 上游用 Func.toLongList(deptId)：deptId 形如 "1,2" 时表示多部门
			List<Long> deptIds = parseDeptIds(user.getDeptId());
			if (deptIds.isEmpty()) {
				return ScopeValue.rejected("scope_type=3（本部门）但当前用户部门 id 为空");
			}
			String column = requireColumn(scopeColumn, type);
			return column == null
				? ScopeValue.rejected("scope_type=3（本部门）但 scopeColumn 为空")
				: ScopeValue.restricted(column, deptIds);
		}
		if (type == DataScopeEnum.OWN_DEPT_CHILD) {
			List<Long> deptIds = parseDeptIds(user.getDeptId());
			if (deptIds.isEmpty()) {
				return ScopeValue.rejected("scope_type=4（本部门及子级）但当前用户部门 id 为空");
			}
			String column = requireColumn(scopeColumn, type);
			if (column == null) {
				return ScopeValue.rejected("scope_type=4（本部门及子级）但 scopeColumn 为空");
			}
			List<Long> ids = new ArrayList<>(deptIds);
			if (descendantProvider != null) {
				for (Long deptId : deptIds) {
					List<Long> descendants = descendantProvider.descendants(deptId);
					if (descendants != null) {
						for (Long descendant : descendants) {
							if (descendant != null && !ids.contains(descendant)) {
								ids.add(descendant);
							}
						}
					}
				}
			}
			return ScopeValue.restricted(column, ids);
		}
		if (type == DataScopeEnum.CUSTOM) {
			String resolved = resolvePlaceholders(scopeValue, user);
			if (resolved == null) {
				return ScopeValue.rejected("scope_type=5（自定义）但 scopeValue 为空");
			}
			return parseCustomScope(resolved);
		}
		// 未知 / 空 scope_type：与上游等效的 fail-closed（上游拼出 in () 让 SQL 报错）
		return ScopeValue.rejected("不支持的数据权限类型: " + scopeType);
	}

	/**
	 * 解析自定义值域（scope_type=5）的受限形态。
	 *
	 * <p>仅接受「列 IN (纯数字列表)」与「列 = 数字」；其余形态一律拒绝，
	 * 绝不把自由文本拼进 SQL。</p>
	 *
	 * @param resolvedScopeValue 已完成占位符解析的值域文本
	 * @return 值域解析结果
	 */
	public static ScopeValue parseCustomScope(String resolvedScopeValue) {
		if (StringUtil.isBlank(resolvedScopeValue)) {
			return ScopeValue.rejected("自定义数据权限值域为空");
		}
		String text = resolvedScopeValue.trim();
		Matcher inMatcher = CUSTOM_IN.matcher(text);
		if (inMatcher.matches()) {
			List<Long> ids = parseIdList(inMatcher.group(2));
			if (ids.isEmpty()) {
				return ScopeValue.rejected("自定义数据权限值域的 id 列表为空: " + text);
			}
			return ScopeValue.restricted(inMatcher.group(1), ids);
		}
		Matcher eqMatcher = CUSTOM_EQ.matcher(text);
		if (eqMatcher.matches()) {
			List<Long> ids = parseIdList(eqMatcher.group(2));
			if (ids.isEmpty()) {
				return ScopeValue.rejected("自定义数据权限值域的 id 为空: " + text);
			}
			return ScopeValue.restricted(eqMatcher.group(1), ids);
		}
		return ScopeValue.rejected("自定义数据权限值域不是受限形态（仅支持「列 IN (数字列表)」/「列 = 数字」）: " + text);
	}

	/**
	 * 用当前用户解析值域中的 {@code ${...}} 占位符（与上游 BladeDataScopeHandler 同源）。
	 */
	private static String resolvePlaceholders(String scopeValue, BladeUser user) {
		if (StringUtil.isBlank(scopeValue)) {
			return null;
		}
		try {
			return PlaceholderUtil.getDefaultResolver().resolveByMap(scopeValue, BeanUtil.toMap(user));
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 取行级过滤列；空白列名视为非法（上游拼出的 SQL 会直接报错，等效 fail-closed）。
	 */
	private static String requireColumn(String scopeColumn, DataScopeEnum type) {
		return StringUtil.isBlank(scopeColumn) ? null : scopeColumn.trim();
	}

	/**
	 * 解析部门 id（支持逗号分隔的多部门，与上游 Func.toLongList 语义一致）。
	 */
	private static List<Long> parseDeptIds(String deptId) {
		if (StringUtil.isBlank(deptId)) {
			return Collections.emptyList();
		}
		List<Long> ids = Func.toLongList(deptId.trim());
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		List<Long> result = new ArrayList<>(ids.size());
		for (Long id : ids) {
			if (id != null && !result.contains(id)) {
				result.add(id);
			}
		}
		return result;
	}

	/**
	 * 解析逗号分隔的数字列表。
	 */
	private static List<Long> parseIdList(String csv) {
		List<Long> ids = new ArrayList<>();
		if (StringUtil.isBlank(csv)) {
			return ids;
		}
		for (String part : csv.split(",")) {
			String text = part.trim();
			if (text.isEmpty()) {
				continue;
			}
			try {
				ids.add(Long.parseLong(text));
			} catch (NumberFormatException ignored) {
				// 正则已限定为数字，这里只是兜底
			}
		}
		return ids;
	}
}
