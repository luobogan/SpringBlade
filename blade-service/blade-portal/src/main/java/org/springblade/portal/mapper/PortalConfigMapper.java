package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalConfig;

/**
 * 门户配置项 Mapper（对标 portalconfig）
 */
@Mapper
public interface PortalConfigMapper extends BaseMapper<PortalConfig> {
}
