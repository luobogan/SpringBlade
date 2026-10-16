package org.springblade.portal.service;

/**
 * 用户个人默认门户服务
 * <p>对标泛微 hpcurrentuse（§18.2）：用户自选"我的默认门户"，landing 时优先于机构默认/全局默认。</p>
 */
public interface IPortalUserDefaultService {

	/**
	 * 查询某用户的个人默认门户页面ID（无则返回 null）。
	 */
	Long getUserDefaultPageId(Long userId);

	/**
	 * 设置/更新某用户的个人默认门户（按 userId upsert）。
	 *
	 * @param userId 用户ID
	 * @param pageId 门户页面ID
	 * @return 是否成功
	 */
	boolean set(Long userId, Long pageId);

	/**
	 * 清除某用户的个人默认门户（逻辑删除）。
	 *
	 * @param userId 用户ID
	 * @return 是否成功
	 */
	boolean remove(Long userId);
}
