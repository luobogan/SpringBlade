package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalQuickSearch;

/**
 * 快捷搜索类型 Mapper
 * 对应表：blade_portal_quick_search
 */
@Mapper
public interface PortalQuickSearchMapper extends BaseMapper<PortalQuickSearch> {
}
