package org.springblade.portal.service;

import org.springblade.portal.dto.PortalLayoutItem;

import java.util.List;
import java.util.Map;

/**
 * 用户个人门户布局服务（对标泛微 hpcurrentuse / 用户专属 hplayout，§18.2/§23）
 */
public interface IPortalUserLayoutService {

	/**
	 * 某用户对某门户页的布局项（elementId → item）；无个性化返回空 Map。
	 */
	Map<Long, PortalLayoutItem> getLayoutMap(Long userId, Long pageId);

	/**
	 * 某用户对某门户页的布局项列表。
	 */
	List<PortalLayoutItem> getLayout(Long userId, Long pageId);

	/**
	 * 保存/覆盖某用户对某门户页的布局（按 userId+pageId upsert）。
	 */
	boolean save(Long userId, Long pageId, List<PortalLayoutItem> items);

	/**
	 * 重置：清除某用户对某门户页的个性化布局（恢复创建者默认）。
	 */
	boolean reset(Long userId, Long pageId);
}
