package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalPlugin;

/**
 * 门户插件 Mapper
 * 对应表：blade_portal_plugin
 */
@Mapper
public interface PortalPluginMapper extends BaseMapper<PortalPlugin> {
}
