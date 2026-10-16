package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalAccountSetting;

/**
 * 门户个人通用设置 Mapper
 * 对应表：blade_portal_account_setting
 */
@Mapper
public interface PortalAccountSettingMapper extends BaseMapper<PortalAccountSetting> {
}
