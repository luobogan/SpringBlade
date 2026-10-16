package org.springblade.portal.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 门户跨业务只读 Mapper
 * <p>
 * 仅用于「元素数据源直查」场景：dev 环境多服务共用同一数据源，
 * 通知公告所在 blade-desk 服务并未在 start-all.bat 启动，
 * 故 notice 元素直接读 blade_notice，避免依赖未启动服务。
 * 待办（todo）与消息（message）走 Feign 调用各自服务的内部端点。
 * </p>
 */
@Mapper
public interface PortalDataMapper {

	/**
	 * 查询通知公告（按发布时间倒序）
	 * <p>
	 * 组织过滤（§22 数据型元素按组织过滤）：
	 * <ul>
	 *   <li>租户：始终生效（tenant_id 过滤，租户隔离底线）；</li>
	 *   <li>部门：deptId 非空时仅取本用户所属部门创建（create_dept）；</li>
	 *   <li>分部：branchDeptIds 非空时仅取分部及其下属部门创建（create_dept IN）。</li>
	 * </ul>
	 *
	 * @param limit          条数
	 * @param tenantId       租户ID（必填）
	 * @param deptId         部门ID（按部门过滤时传，其余为 null）
	 * @param branchDeptIds  分部部门集合（按分部过滤时传，其余为 null）
	 * @return 通知列表（字段 id/title/releaseTime/category）
	 */
	@Select("<script>"
		+ "SELECT `id` AS `id`, `title` AS `title`, `category` AS `category`, "
		+ "COALESCE(`release_time`, `create_time`) AS `releaseTime` "
		+ "FROM `blade_notice` "
		+ "<where>"
		+ "  `is_deleted` = 0 AND (`status` IS NULL OR `status` = 1) "
		+ "  AND `tenant_id` = #{tenantId} "
		+ "  <if test='deptId != null'> AND `create_dept` = #{deptId} </if>"
		+ "  <if test='branchDeptIds != null and branchDeptIds.size() > 0'> "
		+ "    AND `create_dept` IN "
		+ "    <foreach collection='branchDeptIds' item='d' open='(' separator=',' close=')'>#{d}</foreach>"
		+ "  </if>"
		+ "</where>"
		+ "ORDER BY COALESCE(`release_time`, `create_time`) DESC LIMIT #{limit}"
		+ "</script>")
	List<Map<String, Object>> selectNotices(@Param("limit") int limit, @Param("tenantId") String tenantId,
		@Param("deptId") Long deptId, @Param("branchDeptIds") List<Long> branchDeptIds);

	/**
	 * 查询单条公告详情（含正文 content）
	 *
	 * @param id 公告ID
	 * @return 公告详情（字段 id/title/content/category/releaseTime），不存在返回 null
	 */
	@Select("SELECT `id` AS `id`, `title` AS `title`, `content` AS `content`, "
		+ "`category` AS `category`, "
		+ "COALESCE(`release_time`, `create_time`) AS `releaseTime` "
		+ "FROM `blade_notice` "
		+ "WHERE `id` = #{id} AND `is_deleted` = 0")
	Map<String, Object> selectNoticeById(@Param("id") Long id);
}