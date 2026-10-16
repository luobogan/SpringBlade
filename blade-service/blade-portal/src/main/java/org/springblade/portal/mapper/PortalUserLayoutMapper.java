package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalUserLayout;

/**
 * 用户个人门户布局 Mapper
 * 对应表：blade_portal_user_layout
 */
@Mapper
public interface PortalUserLayoutMapper extends BaseMapper<PortalUserLayout> {
}
