package org.springblade.portal.service;

import org.springblade.portal.dto.PortalMenuSaveDTO;
import org.springblade.portal.entity.PortalMenu;
import org.springblade.portal.entity.PortalMenuShare;
import org.springblade.portal.vo.PortalMenuNode;

import java.util.List;

/**
 * 门户菜单服务（对标泛微 portalmenu §14）
 */
public interface IPortalMenuService {

	/**
	 * 菜单列表（管理端，扁平，按类型 + 排序）。
	 */
	List<PortalMenu> list(String menuType);

	/**
	 * 保存菜单（新增/编辑）。父菜单存在性、链接模式在此校验。
	 *
	 * @return 是否成功
	 */
	boolean save(PortalMenuSaveDTO dto);

	/**
	 * 逻辑删除菜单（含其子孙菜单）；系统内置不可删。
	 *
	 * @return 是否成功
	 */
	boolean remove(Long menuId);

	/**
	 * 运行时菜单树：当前用户可见的指定类型菜单，按父子层级组装。
	 *
	 * @param menuType 菜单类型（top/toolbar/quick）
	 * @return 菜单树（仅顶级根节点，子节点挂在 children）
	 */
	List<PortalMenuNode> menuTree(String menuType);

	/**
	 * 系统菜单树（前端/后端菜单）：当前用户有权限的系统导航菜单(blade_menu)，按角色授权过滤，超管见全部。
	 *
	 * @param region 区域：front 前端(左整树) / top 后端(顶)
	 * @return 菜单树
	 */
	List<PortalMenuNode> systemMenuTree(String region);

	/**
	 * 菜单授权列表（1全员/2部门/3个人）。
	 */
	List<PortalMenuShare> listShare(Long menuId);

	/**
	 * 保存菜单授权（差量增删）。
	 */
	boolean saveShare(Long menuId, List<PortalMenuShare> shares);
}
