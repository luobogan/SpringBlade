package org.springblade.portal.service;

import org.springblade.portal.entity.PortalDefault;

import java.util.List;

/**
 * 门户默认首页（按机构）服务
 * <p>对标泛微 defaultportal（§13.8）：按机构指定登录后默认首页。</p>
 */
public interface IPortalDefaultService {

	/**
	 * 设置/更新某机构的默认首页（按 deptId upsert），并记审计。
	 *
	 * @param deptId  机构/部门ID
	 * @param pageId  门户页面ID
	 * @param remark  备注
	 */
	boolean set(Long deptId, Long pageId, String remark);

	/**
	 * 清除某机构的默认首页（逻辑删除），并记审计。
	 */
	boolean remove(Long deptId);

	/**
	 * 全部未删除的机构默认首页配置。
	 */
	List<PortalDefault> list();
}
