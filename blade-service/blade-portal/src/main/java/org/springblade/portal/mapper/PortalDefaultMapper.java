package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalDefault;

/**
 * 门户默认首页（按机构）Mapper
 * 对应表：blade_portal_default
 */
@Mapper
public interface PortalDefaultMapper extends BaseMapper<PortalDefault> {
}
