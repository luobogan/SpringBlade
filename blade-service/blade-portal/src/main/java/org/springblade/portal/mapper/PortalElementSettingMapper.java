package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalElementSetting;

/**
 * 门户元素个性化 Mapper
 * 对应表：blade_portal_element_setting
 */
@Mapper
public interface PortalElementSettingMapper extends BaseMapper<PortalElementSetting> {
}