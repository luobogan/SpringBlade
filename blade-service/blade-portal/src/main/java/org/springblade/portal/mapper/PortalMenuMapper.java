package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalMenu;

/**
 * 门户菜单 Mapper
 * 对应表：blade_portal_menu
 */
@Mapper
public interface PortalMenuMapper extends BaseMapper<PortalMenu> {
}
