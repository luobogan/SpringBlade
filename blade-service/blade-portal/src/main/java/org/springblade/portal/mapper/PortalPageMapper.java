package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springblade.portal.entity.PortalPage;

/**
 * 门户页面 Mapper
 * 对应表：blade_portal_page
 */
@Mapper
public interface PortalPageMapper extends BaseMapper<PortalPage> {

	/**
	 * 访问次数原子 +1（热度统计）。
	 *
	 * @param id 页面ID
	 * @return 影响行数
	 */
	@Update("UPDATE `blade_portal_page` SET `view_count` = IFNULL(`view_count`, 0) + 1 WHERE `id` = #{id}")
	int incrViewCount(@Param("id") Long id);
}
