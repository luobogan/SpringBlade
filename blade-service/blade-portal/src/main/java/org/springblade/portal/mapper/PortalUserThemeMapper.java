package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalUserTheme;

/**
 * 用户主题偏好 Mapper
 * 对应表：blade_portal_user_theme
 */
@Mapper
public interface PortalUserThemeMapper extends BaseMapper<PortalUserTheme> {
}
