package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalLayout;

/**
 * 门户布局模板 Mapper
 * 对应表：blade_portal_layout
 */
@Mapper
public interface PortalLayoutMapper extends BaseMapper<PortalLayout> {
}
