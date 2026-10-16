package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalElementTemplate;

/**
 * 门户元素模板 Mapper
 * 对应表：blade_portal_element_template
 */
@Mapper
public interface PortalElementTemplateMapper extends BaseMapper<PortalElementTemplate> {
}
