package org.springblade.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springblade.portal.entity.PortalPageVisitor;
import org.springblade.portal.vo.PortalPageStatVO;

import java.util.List;

/**
 * 门户页面独立访客 Mapper
 * 对应表：blade_portal_page_visitor
 */
@Mapper
public interface PortalPageVisitorMapper extends BaseMapper<PortalPageVisitor> {

	/**
	 * 记录独立访客（同 页面+日期+租户+用户 已存在则忽略）。依赖唯一键去重。
	 */
	@Insert("INSERT IGNORE INTO `blade_portal_page_visitor` "
		+ "(`page_id`, `stat_date`, `tenant_id`, `user_id`, `is_deleted`) "
		+ "VALUES (#{pageId}, #{statDate}, #{tenantId}, #{userId}, 0)")
	int insertIgnore(@Param("pageId") Long pageId, @Param("statDate") String statDate,
		@Param("tenantId") String tenantId, @Param("userId") Long userId);

	/**
	 * 按租户聚合最近 fromDate 起的每日独立访客数（UV）。
	 */
	@Select("SELECT `stat_date` AS statDate, COUNT(*) AS uv "
		+ "FROM `blade_portal_page_visitor` "
		+ "WHERE `tenant_id` = #{tenantId} AND `is_deleted` = 0 AND `stat_date` >= #{fromDate} "
		+ "GROUP BY `stat_date` ORDER BY `stat_date`")
	List<PortalPageStatVO> dailyUv(@Param("tenantId") String tenantId, @Param("fromDate") String fromDate);
}
