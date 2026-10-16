package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalLoginTemplate;

/**
 * 登录前门户皮肤 Mapper
 * 对应表：blade_portal_login_template
 */
@Mapper
public interface PortalLoginTemplateMapper extends BaseMapper<PortalLoginTemplate> {
}
