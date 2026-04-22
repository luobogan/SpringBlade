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

import org.springblade.core.mp.support.Condition;
import org.springblade.core.tool.node.ForestNodeMerger;
import org.springblade.core.tool.utils.Func;
import org.springblade.system.entity.Menu;
import org.springblade.system.entity.PackageMenu;
import org.springblade.system.entity.TenantPackage;
import org.springblade.system.mapper.MenuMapper;
import org.springblade.system.mapper.TenantPackageMapper;
import org.springblade.system.service.IPackageMenuService;
import org.springblade.system.service.ITenantPackageService;
import org.springblade.system.vo.CheckedTreeVO;
import org.springblade.system.vo.GrantTreeVO;
import org.springblade.system.vo.MenuVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 租户产品包服务实现类
 *
 * @author Chill
 */
@Service
@AllArgsConstructor
public class TenantPackageServiceImpl extends ServiceImpl<TenantPackageMapper, TenantPackage> implements ITenantPackageService {

	private final IPackageMenuService packageMenuService;
	private final MenuMapper menuMapper;

	@Override
	public IPage<TenantPackage> page(IPage<TenantPackage> page, TenantPackage tenantPackage) {
		return baseMapper.selectPage(page, Condition.getQueryWrapper(tenantPackage));
	}

	@Override
	public GrantTreeVO grantTree() {
		GrantTreeVO vo = new GrantTreeVO();
		vo.setMenu(ForestNodeMerger.merge(menuMapper.grantTree("000000")));
		vo.setDataScope(ForestNodeMerger.merge(menuMapper.grantDataScopeTree("000000")));
		vo.setApiScope(ForestNodeMerger.merge(menuMapper.grantApiScopeTree("000000")));
		return vo;
	}

	@Override
	public CheckedTreeVO packageMenuKeys(Long packageId) {
		List<PackageMenu> packageMenus = packageMenuService.list(Wrappers.<PackageMenu>query().lambda().eq(PackageMenu::getPackageId, packageId));
		List<String> menuIds = packageMenus.stream().map(pm -> Func.toStr(pm.getMenuId())).collect(Collectors.toList());

		CheckedTreeVO vo = new CheckedTreeVO();
		CheckedTreeVO.TreeKeys menuKeys = new CheckedTreeVO.TreeKeys();
		menuKeys.setCheckedKeys(menuIds);
		vo.setMenu(menuKeys);

		return vo;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean grantMenu(Long packageId, List<Long> menuIds) {
		removeMenu(packageId);

		if (Func.isEmpty(menuIds)) {
			return true;
		}

		List<PackageMenu> packageMenus = new ArrayList<>();
		for (Long menuId : menuIds) {
			PackageMenu packageMenu = new PackageMenu();
			packageMenu.setPackageId(packageId);
			packageMenu.setMenuId(menuId);
			packageMenus.add(packageMenu);
		}

		return packageMenuService.saveBatch(packageMenus);
	}

	@Override
	public boolean removeMenu(Long packageId) {
		packageMenuService.remove(Wrappers.<PackageMenu>query().lambda().eq(PackageMenu::getPackageId, packageId));
		return true;
	}

	@Override
	public List<Long> getMenuIdsByPackageId(Long packageId) {
		List<PackageMenu> packageMenus = packageMenuService.list(Wrappers.<PackageMenu>query().lambda().eq(PackageMenu::getPackageId, packageId));
		return packageMenus.stream().map(PackageMenu::getMenuId).collect(Collectors.toList());
	}

	@Override
	public List<String> getMenuCodesByPackageId(Long packageId) {
		List<PackageMenu> packageMenus = packageMenuService.list(Wrappers.<PackageMenu>query().lambda().eq(PackageMenu::getPackageId, packageId));
		List<Long> menuIds = packageMenus.stream().map(PackageMenu::getMenuId).collect(Collectors.toList());
		if (menuIds.isEmpty()) {
			return new ArrayList<>();
		}
		List<Menu> menus = menuMapper.selectBatchIds(menuIds);
		return menus.stream().map(Menu::getCode).collect(Collectors.toList());
	}
}
