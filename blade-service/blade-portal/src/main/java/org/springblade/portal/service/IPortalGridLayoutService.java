package org.springblade.portal.service;

import org.springblade.portal.dto.PortalGridLayoutSaveDTO;
import org.springblade.portal.entity.PortalGridLayout;
import org.springblade.portal.entity.PortalGridLayoutShare;

import java.util.List;

/**
 * 特色门户（门户网格布局）服务（对标泛微 tsportal §16.2，hp_grid_layout）
 */
public interface IPortalGridLayoutService {

	/**
	 * 布局列表（layoutType 可空：login/theme）
	 */
	List<PortalGridLayout> list(String layoutType);

	/**
	 * 保存布局（新增/编辑；同租户 类型+名称 冲突返回 false）
	 *
	 * @return true=成功
	 */
	boolean save(PortalGridLayoutSaveDTO dto);

	/**
	 * 删除布局（逻辑删除；默认入口不可删）
	 */
	boolean remove(Long id);

	/**
	 * 启用/停用布局（对标 usePortalGridLayout）
	 */
	boolean setUse(Long id, boolean use);

	/**
	 * 设为默认入口（同类型内唯一，对标 defaultEntrancePortalGridLayout）
	 */
	boolean setDefaultEntrance(Long id);

	/**
	 * 布局授权列表（使用限制）
	 */
	List<PortalGridLayoutShare> listShare(Long layoutId);

	/**
	 * 保存布局授权（差量增删；传空数组=清空）
	 */
	boolean saveShare(Long layoutId, List<PortalGridLayoutShare> shares);
}
