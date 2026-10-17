package org.springblade.portal.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalGridLayout;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 特色门户布局 Mapper（对标 hp_grid_layout）
 */
@Mapper
public interface PortalGridLayoutMapper extends BaseMapper<PortalGridLayout> {
}
