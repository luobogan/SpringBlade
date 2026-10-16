package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springblade.portal.entity.PortalPageStat;
import org.springblade.portal.vo.PortalPageStatVO;

import java.util.List;

/**
 * 门户页面按日访问统计 Mapper
 * 对应表：blade_portal_page_stat
 */
@Mapper
public interface PortalPageStatMapper extends BaseMapper<PortalPageStat> {

	/**
	 * 当日访问量原子 +1（不存在则插入）。依赖唯一键 (page_id, stat_date, tenant_id)。
	 */
	@Insert("INSERT INTO `blade_portal_page_stat` (`page_id`, `stat_date`, `tenant_id`, `view_count`, `is_deleted`) "
		+ "VALUES (#{pageId}, #{statDate}, #{tenantId}, 1, 0) "
		+ "ON DUPLICATE KEY UPDATE `view_count` = `view_count` + 1")
	int upsertView(@Param("pageId") Long pageId, @Param("statDate") String statDate, @Param("tenantId") String tenantId);

	/**
	 * 按租户聚合最近 fromDate 起的每日总访问量。
	 */
	@Select("SELECT `stat_date` AS statDate, SUM(`view_count`) AS viewCount "
		+ "FROM `blade_portal_page_stat` "
		+ "WHERE `tenant_id` = #{tenantId} AND `is_deleted` = 0 AND `stat_date` >= #{fromDate} "
		+ "GROUP BY `stat_date` ORDER BY `stat_date`")
	List<PortalPageStatVO> dailySum(@Param("tenantId") String tenantId, @Param("fromDate") String fromDate);
}
