package org.springblade.portal.service;

import org.springblade.portal.dto.PortalSettingSaveDTO;
import org.springblade.portal.entity.PortalSetting;

/**
 * 门户应用设置服务（对标泛微 portalsetting §16.3，租户单例）
 */
public interface IPortalSettingService {

	/**
	 * 读取租户全局设置（无记录时返回字段默认值兜底的实例，不返回 null）
	 */
	PortalSetting get();

	/**
	 * 保存租户全局设置（upsert；无记录时播种）
	 *
	 * @return true=成功
	 */
	boolean save(PortalSettingSaveDTO dto);

	/**
	 * 同步到所有人：把全局字体/布局同步到所有已有个性化设置的用户
	 * （对标泛微 portalsetting synchronize/layoutSynchronize §16.3）
	 *
	 * @return 受影响行数（异常返回 -1）
	 */
	int syncToAll();
}
