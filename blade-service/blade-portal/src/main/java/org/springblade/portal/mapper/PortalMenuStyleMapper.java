package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalMenuStyle;

/**
 * 门户菜单样式 Mapper
 * 对应表：blade_portal_menu_style
 */
@Mapper
public interface PortalMenuStyleMapper extends BaseMapper<PortalMenuStyle> {
}
