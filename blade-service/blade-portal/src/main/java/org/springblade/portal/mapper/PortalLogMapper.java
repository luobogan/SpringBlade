package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalLog;

/**
 * 门户业务审计 Mapper
 * 对应表：blade_portal_log
 */
@Mapper
public interface PortalLogMapper extends BaseMapper<PortalLog> {
}
