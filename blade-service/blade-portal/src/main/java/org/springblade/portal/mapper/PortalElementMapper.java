package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalElement;

/**
 * 门户元素 Mapper
 * 对应表：blade_portal_element
 */
@Mapper
public interface PortalElementMapper extends BaseMapper<PortalElement> {
}