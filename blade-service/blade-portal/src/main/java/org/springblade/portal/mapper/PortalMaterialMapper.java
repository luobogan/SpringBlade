package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.portal.entity.PortalMaterial;

/**
 * 门户素材 Mapper
 * 对应表：blade_portal_material
 */
@Mapper
public interface PortalMaterialMapper extends BaseMapper<PortalMaterial> {
}
