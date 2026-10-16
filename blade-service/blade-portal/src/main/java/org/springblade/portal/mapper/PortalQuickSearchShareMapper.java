package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalQuickSearchShare;

/**
 * 快捷搜索使用限制 Mapper
 * 对应表：blade_portal_quick_search_share
 */
@Mapper
public interface PortalQuickSearchShareMapper extends BaseMapper<PortalQuickSearchShare> {
}
