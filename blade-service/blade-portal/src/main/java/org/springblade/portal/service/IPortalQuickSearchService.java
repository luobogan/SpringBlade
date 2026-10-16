package org.springblade.portal.service;

import org.springblade.portal.dto.PortalQuickSearchSaveDTO;
import org.springblade.portal.entity.PortalQuickSearch;
import org.springblade.portal.entity.PortalQuickSearchShare;

import java.util.List;

/**
 * 快捷搜索管理服务（对标泛微 quicksearchmanage §14.2 H）
 */
public interface IPortalQuickSearchService {

	/** 全部启用/停用的搜索类型（按 order_num 排序） */
	List<PortalQuickSearch> list();

	/** 新增/编辑（同租户 module+type 冲突返回 false） */
	boolean save(PortalQuickSearchSaveDTO dto);

	/** 删除（系统内置不可删） */
	boolean remove(Long id);

	/** 使用限制列表 */
	List<PortalQuickSearchShare> listShare(Long searchId);

	/** 差量保存使用限制（1全员/2部门/3个人/4角色/5分部；传空=清空） */
	boolean saveShare(Long searchId, List<PortalQuickSearchShare> shares);
}
