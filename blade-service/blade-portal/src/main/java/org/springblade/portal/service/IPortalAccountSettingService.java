package org.springblade.portal.service;

import org.springblade.portal.dto.PortalAccountSettingSaveDTO;
import org.springblade.portal.entity.PortalAccountSetting;
import org.springblade.portal.vo.AccountMoreVO;

/**
 * 门户个人通用设置服务（对标泛微 accountmore §14.2 I）
 */
public interface IPortalAccountSettingService {

	/**
	 * 读取个人设置（无记录返回 null，由调用方回退全局）
	 */
	PortalAccountSetting get(Long userId);

	/**
	 * 保存个人设置（upsert；userId 强制取当前登录人，不接受入参）
	 *
	 * @return true=成功
	 */
	boolean save(Long userId, PortalAccountSettingSaveDTO dto);

	/**
	 * 读取个人生效设置（个人值优先，NULL 回退全局默认），供门户查看页一次性取数
	 */
	AccountMoreVO getEffective(Long userId);
}
