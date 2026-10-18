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
package org.springblade.system.wrapper;

import org.springblade.common.constant.CommonConstant;
import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.node.ForestNodeMerger;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.SpringUtil;
import org.springblade.system.cache.DictCache;
import org.springblade.system.entity.Menu;
import org.springblade.system.service.IDictService;
import org.springblade.system.service.IMenuService;
import org.springblade.system.vo.MenuVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 包装类,返回视图层所需的字段
 *
 * @author Chill
 */
public class MenuWrapper extends BaseEntityWrapper<Menu, MenuVO> {

	private static IMenuService menuService;

	private static IDictService dictService;

	static {
		menuService = SpringUtil.getBean(IMenuService.class);
		dictService = SpringUtil.getBean(IDictService.class);
	}

	public static MenuWrapper build() {
		return new MenuWrapper();
	}

	@Override
	public MenuVO entityVO(Menu menu) {
		MenuVO menuVO = BeanUtil.copyProperties(menu, MenuVO.class);
		Long parentId = menu.getParentId();
		if (Func.equals(parentId, CommonConstant.TOP_PARENT_ID)) {
			menuVO.setParentName(CommonConstant.TOP_PARENT_NAME);
		} else if (parentId == null) {
			// 防御：parent_id 为空（脏数据）时不能拿去查库，按顶级节点展示
			menuVO.setParentName(CommonConstant.TOP_PARENT_NAME);
		} else {
			Menu parent = menuService.getById(parentId);
			// 防御：父菜单可能已被删除/不存在，不能直接 parent.getName()（会 NPE）
			menuVO.setParentName(parent == null ? CommonConstant.TOP_PARENT_NAME : parent.getName());
		}
		menuVO.setCategoryName(DictCache.getValue("menu_category", Func.toInt(menuVO.getCategory())));
		menuVO.setActionName(DictCache.getValue("button_func", Func.toInt(menuVO.getAction())));
		menuVO.setIsOpenName(DictCache.getValue("yes_no", Func.toInt(menuVO.getIsOpen())));
		menuVO.setComponent(menu.getComponent());
		menuVO.setComponentType(menu.getComponentType());
		menuVO.setRemoteUrl(menu.getRemoteUrl());
		menuVO.setCacheVersion(menu.getCacheVersion());
		return menuVO;
	}


	public List<MenuVO> listNodeVO(List<Menu> list) {
		List<MenuVO> collect = list.stream().map(menu -> {
			MenuVO menuVO = BeanUtil.copyProperties(menu, MenuVO.class);
			// 防御：parent_id 为空时 ForestNodeManager 建树会对每个节点取
			// getParentId().longValue()，直接 NPE 导致接口 500（全站按钮权限为空）。
			// 顶级节点约定用 CommonConstant.TOP_PARENT_ID(0)，这里把空值归一化过去。
			if (menuVO.getParentId() == null) {
				menuVO.setParentId(CommonConstant.TOP_PARENT_ID);
			}
			return menuVO;
		}).collect(Collectors.toList());
		return ForestNodeMerger.merge(collect);
	}

	public List<MenuVO> listNodeLazyVO(List<MenuVO> list) {
		return ForestNodeMerger.merge(list);
	}

}
