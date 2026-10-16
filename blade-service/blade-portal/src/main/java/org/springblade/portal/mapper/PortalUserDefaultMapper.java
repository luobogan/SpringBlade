package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalUserDefault;

/**
 * 用户个人默认门户 Mapper
 * 对应表：blade_portal_user_default
 */
@Mapper
public interface PortalUserDefaultMapper extends BaseMapper<PortalUserDefault> {
}
