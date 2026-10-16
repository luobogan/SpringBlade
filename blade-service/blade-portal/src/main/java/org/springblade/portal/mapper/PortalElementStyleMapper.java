package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalElementStyle;

/**
 * 门户元素样式 Mapper
 * 对应表：blade_portal_element_style
 */
@Mapper
public interface PortalElementStyleMapper extends BaseMapper<PortalElementStyle> {
}
