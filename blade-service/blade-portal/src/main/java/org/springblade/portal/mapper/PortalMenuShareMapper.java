package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalMenuShare;

/**
 * 门户菜单授权 Mapper
 * 对应表：blade_portal_menu_share
 */
@Mapper
public interface PortalMenuShareMapper extends BaseMapper<PortalMenuShare> {
}
