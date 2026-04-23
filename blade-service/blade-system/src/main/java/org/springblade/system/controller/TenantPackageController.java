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
package org.springblade.system.controller;

import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.cache.utils.CacheUtil;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.node.ForestNodeMerger;
import org.springblade.core.tool.node.TreeNode;
import org.springblade.system.entity.TenantPackage;
import org.springblade.system.mapper.MenuMapper;
import org.springblade.system.service.ITenantPackageService;
import org.springblade.system.vo.CheckedTreeVO;
import org.springblade.system.vo.GrantTreeVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 租户产品包控制器
 *
 * @author Chill
 */
@RestController
@RequestMapping("/tenant-package")
@Hidden
@Tag(name = "租户产品包管理")
@AllArgsConstructor
public class TenantPackageController extends BladeController {

	private final ITenantPackageService tenantPackageService;
	private final MenuMapper menuMapper;

	/**
	 * 产品包列表
	 */
	@GetMapping("/list")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "产品包列表", description = "产品包列表")
	public R<IPage<TenantPackage>> list(Query query, TenantPackage tenantPackage) {
		IPage<TenantPackage> pages = tenantPackageService.page(Condition.getPage(query), tenantPackage);
		return R.data(pages);
	}

	/**
	 * 产品包详情
	 */
	@GetMapping("/detail")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "产品包详情", description = "产品包详情")
	public R<TenantPackage> detail(Long id) {
		TenantPackage tenantPackage = tenantPackageService.getById(id);
		return R.data(tenantPackage);
	}

	/**
	 * 新增产品包
	 */
	@PostMapping("/submit")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "新增产品包", description = "新增产品包")
	public R submit(@Valid @RequestBody TenantPackage tenantPackage) {
		tenantPackage.setTenantId(BladeConstant.ADMIN_TENANT_ID);
		boolean save = tenantPackageService.save(tenantPackage);
		return save ? R.data(tenantPackage) : R.fail("创建失败");
	}

	/**
	 * 更新产品包
	 */
	@PostMapping("/update")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "更新产品包", description = "更新产品包")
	public R update(@Valid @RequestBody TenantPackage tenantPackage) {
		boolean update = tenantPackageService.updateById(tenantPackage);
		CacheUtil.clear(CacheUtil.SYS_CACHE);
		return R.status(update);
	}

	/**
	 * 删除产品包
	 */
	@GetMapping("/remove")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "删除产品包", description = "删除产品包")
	public R remove(Long id) {
		boolean remove = tenantPackageService.removeById(id);
		CacheUtil.clear(CacheUtil.SYS_CACHE);
		return R.status(remove);
	}

	/**
	 * 获取菜单权限分配树形结构
	 */
	@GetMapping("/menu-grant-tree")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "菜单权限分配树", description = "获取菜单权限分配树形结构")
	public R<GrantTreeVO> menuGrantTree() {
		return R.data(tenantPackageService.grantTree());
	}

	/**
	 * 获取产品包已分配的菜单权限
	 */
	@GetMapping("/package-menu-keys")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "产品包已分配菜单", description = "获取指定产品包已分配的菜单权限")
	public R<CheckedTreeVO> packageMenuKeys(Long packageId) {
		return R.data(tenantPackageService.packageMenuKeys(packageId));
	}

	/**
	 * 为产品包分配菜单权限
	 */
	@PostMapping("/grant-menu")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "分配菜单权限", description = "为指定产品包分配菜单权限")
	public R grantMenu(@RequestParam Long packageId, @RequestBody Map<String, List<Long>> body) {
		List<Long> menuIds = body.get("menuIds");
		if (Func.isEmpty(menuIds)) {
			return R.fail("请选择要分配的菜单");
		}
		boolean success = tenantPackageService.grantMenu(packageId, menuIds);
		CacheUtil.clear(CacheUtil.SYS_CACHE);
		return success ? R.success("菜单权限分配成功") : R.fail("菜单权限分配失败");
	}

	/**
	 * 获取产品包关联的菜单树（用于分配时预览）
	 */
	@GetMapping("/package-menu-tree")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "产品包菜单树预览", description = "获取指定产品包关联的菜单树形结构")
	public R<List<TreeNode>> packageMenuTree(Long packageId) {
		List<Long> menuIds = tenantPackageService.getMenuIdsByPackageId(packageId);
		if (Func.isEmpty(menuIds)) {
			return R.data(new ArrayList<>());
		}
		List<TreeNode> allTreeNodes;
		try {
			Object rawResult = menuMapper.grantTree(BladeConstant.ADMIN_TENANT_ID);
			allTreeNodes = (List<TreeNode>) rawResult;
		} catch (ClassCastException e) {
			allTreeNodes = new ArrayList<>();
		}
		List<TreeNode> filtered = filterTreeByPackage(allTreeNodes, menuIds);
		return R.data(ForestNodeMerger.merge(filtered));
	}

	@SuppressWarnings("unchecked")
	private List<TreeNode> filterTreeByPackage(List<TreeNode> nodes, List<Long> packageMenuIds) {
		List<TreeNode> result = new ArrayList<>();
		for (TreeNode node : nodes) {
			if (node.getId() != null && packageMenuIds.contains(node.getId())) {
				TreeNode copy = new TreeNode();
				copy.setId(node.getId());
				copy.setParentId(node.getParentId());
				copy.setTitle(node.getTitle());
				if (node.getChildren() != null && !node.getChildren().isEmpty()) {
					copy.setChildren(filterTreeByPackage((List<TreeNode>) node.getChildren(), packageMenuIds));
				}
				result.add(copy);
			} else if (node.getChildren() != null && !node.getChildren().isEmpty()) {
				List<TreeNode> childMatches = filterTreeByPackage((List<TreeNode>) node.getChildren(), packageMenuIds);
				if (!childMatches.isEmpty()) {
					TreeNode copy = new TreeNode();
					copy.setId(node.getId());
					copy.setParentId(node.getParentId());
					copy.setTitle(node.getTitle());
					copy.setChildren(childMatches);
					result.add(copy);
				}
			}
		}
		return result;
	}
}
