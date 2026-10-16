package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalElementLib;

/**
 * 门户独立元素库 Mapper
 * 对应表：blade_portal_element_lib
 */
@Mapper
public interface PortalElementLibMapper extends BaseMapper<PortalElementLib> {
}
