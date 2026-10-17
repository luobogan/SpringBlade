package org.springblade.portal.service;

import org.springblade.portal.entity.PortalConfig;

import java.util.List;

/**
 * 门户配置项服务（对标泛微 portalconfig §16.6 + PortalConfigUtil 缓存语义）
 */
public interface IPortalConfigService {

	/**
	 * 配置项列表（当前租户全部未删除，按 id 升序）
	 */
	List<PortalConfig> list();

	/**
	 * 读取配置值（带进程内缓存；无记录返回 null）
	 *
	 * @param ccode 配置项编码
	 */
	String getValue(String ccode);

	/**
	 * 读取布尔开关（无记录或非 "1" 返回 false；用于 isOpenMenuCache 等开关消费点）
	 */
	boolean isEnabled(String ccode);

	/**
	 * 设置配置值（不存在返回 false；写库后刷新缓存）
	 */
	boolean setValue(String ccode, String vvalue);

	/**
	 * 刷新进程内缓存（对标 PortalConfigUtil.refresh）
	 */
	void refresh();
}
