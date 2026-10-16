package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalPageShare;

/**
 * 门户页面授权 Mapper
 * 对应表：blade_portal_page_share
 */
@Mapper
public interface PortalPageShareMapper extends BaseMapper<PortalPageShare> {
}