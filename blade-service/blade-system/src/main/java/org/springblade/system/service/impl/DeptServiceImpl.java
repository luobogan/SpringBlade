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
package org.springblade.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springblade.core.cache.utils.CacheUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tenant.TenantGuard;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.node.ForestNodeMerger;
import org.springblade.core.cache.utils.CacheUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringPool;
import org.springblade.system.entity.Dept;
import org.springblade.system.user.entity.User;
import org.springblade.system.mapper.DeptMapper;
import org.springblade.system.mapper.UserMapper;
import org.springblade.system.service.IDeptService;
import org.springblade.system.vo.DeptVO;
import org.springblade.system.wrapper.DeptWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springblade.core.tenant.TenantGuard.EntityType.DEPT;
import static org.springblade.core.tenant.TenantGuard.EntityType.DEPT_PARENT;

/**
 * 服务实现类
 *
 * @author Chill
 */
@Service
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements IDeptService {

	/**
	 * 用户 Mapper：封存前置校验「组织下无在职人员」时直接查表，
	 * 避免与 UserService 相互注入成环（UserServiceImpl 已注入 IDeptService）。
	 */
	@Autowired
	private UserMapper userMapper;

	@Override
	public IPage<DeptVO> selectDeptPage(IPage<DeptVO> page, DeptVO dept) {
		return page.setRecords(baseMapper.selectDeptPage(page, dept));
	}

	@Override
	public List<DeptVO> tree(String tenantId) {
		String resolvedTenantId = SecureUtil.isAdministrator()
			? Func.toStr(tenantId, SecureUtil.getTenantId())
			: SecureUtil.getTenantId();
		return ForestNodeMerger.merge(baseMapper.tree(resolvedTenantId));
	}

	@Override
	public List<DeptVO> treeScope(String tenantId) {
		// 超管不受数据权限限制，直接返回全量部门树
		if (SecureUtil.isAdministrator()) {
			return tree(tenantId);
		}
		String myDept = currentUserDeptId();
		List<DeptVO> all = baseMapper.tree(tenantId);
		if (myDept == null || all.stream().noneMatch(d -> myDept.equals(String.valueOf(d.getId())))) {
			return ForestNodeMerger.merge(all);
		}
		// 以「当前用户所在部门」为根，保留祖先链（保证树结构连续、可向上选择）与下属部门
		Map<String, DeptVO> byId = all.stream()
			.collect(Collectors.toMap(d -> String.valueOf(d.getId()), Function.identity(), (a, b) -> a));
		Set<String> allowed = new HashSet<>();
		String cursor = myDept;
		while (cursor != null && !"0".equals(cursor) && byId.containsKey(cursor)) {
			allowed.add(cursor);
			DeptVO node = byId.get(cursor);
			cursor = node.getParentId() == null ? null : String.valueOf(node.getParentId());
		}
		Queue<String> queue = new LinkedList<>();
		queue.add(myDept);
		while (!queue.isEmpty()) {
			String id = queue.poll();
			if (!allowed.add(id)) {
				continue;
			}
			for (DeptVO node : all) {
				Long parent = node.getParentId();
				if (parent != null && String.valueOf(parent).equals(id)) {
					queue.add(String.valueOf(node.getId()));
				}
			}
		}
		List<DeptVO> filtered = all.stream()
			.filter(d -> allowed.contains(String.valueOf(d.getId())))
			.collect(Collectors.toList());
		return ForestNodeMerger.merge(filtered);
	}

	/** 取当前登录用户所属部门 ID（用于数据权限裁剪；dept_id 可能为逗号分隔多部门，取首段；取不到返回 null 退化为全量） */
	private String currentUserDeptId() {
		Long userId = SecureUtil.getUserId();
		if (userId == null) {
			return null;
		}
		User user = userMapper.selectById(userId);
		if (user == null) {
			return null;
		}
		String deptId = user.getDeptId();
		if (Func.isEmpty(deptId)) {
			return null;
		}
		return deptId.split(",")[0];
	}

	@Override
	public List<DeptVO> selectList(Map<String, Object> dept) {
		QueryWrapper<Dept> queryWrapper = Condition.getQueryWrapper(dept, Dept.class);
		if (!SecureUtil.isAdministrator()) {
			queryWrapper.lambda().eq(Dept::getTenantId, SecureUtil.getTenantId());
		}
		return DeptWrapper.build().listNodeVO(list(queryWrapper));
	}

	@Override
	public String getDeptIds(String tenantId, String deptNames) {
		List<Dept> deptList = baseMapper.selectList(Wrappers.<Dept>query().lambda().eq(Dept::getTenantId, tenantId).in(Dept::getDeptName, Func.toStrList(deptNames)));
		if (deptList != null && deptList.size() > 0) {
			return deptList.stream().map(dept -> Func.toStr(dept.getId())).distinct().collect(Collectors.joining(","));
		}
		return null;
	}

	@Override
	public List<String> getDeptNames(String deptIds) {
		if (Func.isEmpty(deptIds)) {
			return java.util.Collections.emptyList();
		}
		Long[] ids = Func.toLongArray(deptIds);
		if (ids == null || ids.length == 0) {
			return java.util.Collections.emptyList();
		}
		return baseMapper.getDeptNames(ids);
	}

	/**
	 * 新增或修改部门（带租户归属校验）
	 * <p>
	 * 顶级部门通过 {@link TenantGuard#bindTenant} 绑定当前会话 tenantId；
	 * 子部门强制继承父节点 tenantId，且<strong>显式禁止跨租户迁移</strong>——
	 * 源部门与目标父部门必须同租户，否则抛业务异常。
	 * <p>
	 * 跨租户迁移会让子部门、子部门下的用户/角色/数据权限范围全部失配，
	 * 应通过专门的"租户迁移"工单流程而非常规修改入口完成。
	 */
	@Override
	public boolean submit(Dept dept) {
		CacheUtil.clear(CacheUtil.SYS_CACHE);
		// 节点类型缺省为部门（分部需显式指定，对齐 ecology 双表模型）
		if (dept.getDeptType() == null) {
			dept.setDeptType(2);
		}
		if (dept.getCanceled() == null) {
			dept.setCanceled(0);
		}
		Dept parent = null;
		// 顶级判断：parentId 为空或 0（前端 TreeSelect 的"无"=0，等价于顶级）
		if (Func.isEmpty(dept.getParentId()) || Func.toLong(dept.getParentId()) == 0L) {
			// 顶级部门：bindTenant 统一处理新增 / 修改路径的租户绑定
			TenantGuard.bindTenant(this, dept, DEPT);
			dept.setParentId(BladeConstant.TOP_PARENT_ID);
			dept.setAncestors(String.valueOf(BladeConstant.TOP_PARENT_ID));
			// 顶级节点无所属分部（避免 TenantGuard 占位 -1 污染）
			dept.setSubcompanyId(null);
		} else {
			// 子部门：先校验自身归属（修改路径），再继承父节点 tenantId
			if (Func.toLong(dept.getParentId()) == Func.toLong(dept.getId())) {
				throw new ServiceException("父节点不可选择自身!");
			}
			Dept self = Func.isNotEmpty(dept.getId())
				? TenantGuard.verify(this, dept.getId(), DEPT) : null;
			parent = TenantGuard.verify(this, dept.getParentId(), DEPT_PARENT);
			if (parent == null) {
				throw new ServiceException("上级部门不存在!");
			}
			// 显式禁止跨租户迁移：保护现有子树的数据一致性
			if (self != null && Func.isNotEmpty(self.getTenantId())
				&& !self.getTenantId().equals(parent.getTenantId())) {
				throw new ServiceException("不允许跨租户迁移部门，请联系运维处理");
			}
			dept.setTenantId(parent.getTenantId());
			dept.setAncestors(parent.getAncestors() + StringPool.COMMA + dept.getParentId());
		}
		// —— 对齐 ecology 的组织校验（改造文档 §8.5 配套改造）——
		// 编号租户内唯一（对齐 subcompanycode/departmentcode 代码级全局唯一）
		if (Func.isNotBlank(dept.getDeptCode())) {
			Long codeCnt = this.count(Wrappers.<Dept>lambdaQuery()
				.eq(Dept::getTenantId, dept.getTenantId())
				.eq(Dept::getDeptCode, dept.getDeptCode())
				.ne(Func.isNotEmpty(dept.getId()), Dept::getId, dept.getId()));
			if (codeCnt > 0) {
				throw new ServiceException("组织编号已存在: " + dept.getDeptCode());
			}
		}
		// 同一上级下名称唯一（对齐存储过程内 flag=2/3 校验）
		if (Func.isNotBlank(dept.getDeptName())) {
			Long nameCnt = this.count(Wrappers.<Dept>lambdaQuery()
				.eq(Dept::getTenantId, dept.getTenantId())
				.eq(Dept::getParentId, Func.toLong(dept.getParentId(), 0L))
				.eq(Dept::getDeptName, dept.getDeptName())
				.ne(Func.isNotEmpty(dept.getId()), Dept::getId, dept.getId()));
			if (nameCnt > 0) {
				throw new ServiceException("同级下已存在同名组织: " + dept.getDeptName());
			}
		}
		// 层级上限 10 级（对齐 ifSubComLevelEquals10/ifDeptLevelEquals10）
		if (Func.isNotBlank(dept.getAncestors())
			&& dept.getAncestors().split(StringPool.COMMA).length >= 10) {
			throw new ServiceException("组织层级不能超过 10 级");
		}
		// 部门节点自动推导所属分部（对齐 subcompanyid1：取最近一级分部祖先）
		if (Integer.valueOf(1).equals(dept.getDeptType())) {
			dept.setSubcompanyId(null);
		} else if (parent != null) {
			dept.setSubcompanyId(Integer.valueOf(1).equals(parent.getDeptType())
				? parent.getId() : parent.getSubcompanyId());
		}
		dept.setIsDeleted(BladeConstant.DB_NOT_DELETED);
		return saveOrUpdate(dept);
	}

	@Override
	public boolean remove(List<Long> ids) {
		CacheUtil.clear(CacheUtil.SYS_CACHE);
		TenantGuard.verifyBatch(this, ids, DEPT);
		// 逻辑删除 × 唯一键对策：置空编号，保证编号可复用（见改造文档 §6/§8.5）
		this.update(Wrappers.<Dept>update().lambda()
			.set(Dept::getDeptCode, null)
			.in(Dept::getId, ids));
		return removeByIds(ids);
	}

	/**
	 * 封存组织（对齐 ecology CancelDepartmentCmd/CancelSubCompanyCmd）：
	 * 前置校验「子组织已全部封存 + 组织下无在职人员」；封存≠删除，可解封。
	 */
	@Override
	public boolean cancel(List<Long> ids) {
		CacheUtil.clear(CacheUtil.SYS_CACHE);
		TenantGuard.verifyBatch(this, ids, DEPT);
		for (Long id : ids) {
			// 前置 1：子组织必须已全部封存
			Long childCnt = this.count(Wrappers.<Dept>lambdaQuery()
				.eq(Dept::getParentId, id)
				.eq(Dept::getCanceled, 0));
			if (childCnt > 0) {
				throw new ServiceException("存在未封存的子组织，请先自底向上封存");
			}
			// 前置 2：组织下无在职人员（person_status 0试用/1正式/2临时/3延期/5退休）
			Long userCnt = userMapper.selectCount(Wrappers.<User>query().lambda()
				.apply("FIND_IN_SET({0}, dept_id) > 0", String.valueOf(id))
				.in(User::getPersonStatus, 0, 1, 2, 3, 5));
			if (userCnt > 0) {
				throw new ServiceException("组织下存在在职人员，无法封存");
			}
		}
		return this.update(Wrappers.<Dept>update().lambda()
			.set(Dept::getCanceled, 1)
			.in(Dept::getId, ids));
	}

	/**
	 * 解封组织（对齐 ecology ISCanceledDepartmentCmd/ISCanceledSubCompanyCmd）
	 */
	@Override
	public boolean isCanceled(List<Long> ids) {
		CacheUtil.clear(CacheUtil.SYS_CACHE);
		TenantGuard.verifyBatch(this, ids, DEPT);
		return this.update(Wrappers.<Dept>update().lambda()
			.set(Dept::getCanceled, 0)
			.in(Dept::getId, ids));
	}

}
