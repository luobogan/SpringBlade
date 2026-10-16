package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalSetting;

/**
 * 门户应用设置 Mapper
 * 对应表：blade_portal_setting
 */
@Mapper
public interface PortalSettingMapper extends BaseMapper<PortalSetting> {
}
