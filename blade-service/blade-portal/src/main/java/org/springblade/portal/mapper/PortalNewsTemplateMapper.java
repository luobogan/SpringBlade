package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalNewsTemplate;

/**
 * 门户新闻模板 Mapper
 * 对应表：blade_portal_news_template
 */
@Mapper
public interface PortalNewsTemplateMapper extends BaseMapper<PortalNewsTemplate> {
}
