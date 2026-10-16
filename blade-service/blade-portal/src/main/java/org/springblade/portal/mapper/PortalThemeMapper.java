package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalTheme;

/**
 * 门户主题 Mapper
 * 对应表：blade_portal_theme
 */
@Mapper
public interface PortalThemeMapper extends BaseMapper<PortalTheme> {
}
