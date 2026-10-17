package org.springblade.portal.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springblade.portal.entity.BladeUserBrief;

import java.util.Collection;
import java.util.List;

/**
 * 系统用户只读 Mapper（blade_user）
 * <p>用于主题批量指定（按范围展开用户）与主题使用查询（人员搜索/反查）；
 * 与 BladeMenuMapper 同库只读模式一致（dev 多服务共用数据源）。</p>
 */
@Mapper
public interface BladeUserMapper {

	/**
	 * 按共享范围展开用户ID集合。
	 *
	 * @param scopeType 1全员 2部门 3个人 4角色（暂不支持分部：分部展开需部门树反向遍历，UI 不呈现）
	 * @param scopeIds  范围对象ID（部门/个人/角色）；scopeType=1 时可空
	 */
	@Select("<script>"
		+ "SELECT `id` FROM `blade_user` "
		+ "WHERE `tenant_id` = #{tenantId} AND `is_deleted` = 0 AND `status` = 1 "
		+ "<choose>"
		+ "  <when test='scopeType == 2'> AND `dept_id` IN "
		+ "    <foreach collection='scopeIds' item='d' open='(' separator=',' close=')'>#{d}</foreach> </when>"
		+ "  <when test='scopeType == 3'> AND `id` IN "
		+ "    <foreach collection='scopeIds' item='u' open='(' separator=',' close=')'>#{u}</foreach> </when>"
		+ "  <when test='scopeType == 4'> AND ("
		+ "    <foreach collection='scopeIds' item='r' separator=' OR '> FIND_IN_SET(#{r}, `role_id`) </foreach>"
		+ "  ) </when>"
		+ "  <otherwise></otherwise>"
		+ "</choose>"
		+ " ORDER BY `id`"
		+ "</script>")
	List<Long> selectIdsByScope(@Param("tenantId") String tenantId,
		@Param("scopeType") int scopeType,
		@Param("scopeIds") Collection<Long> scopeIds);

	/**
	 * 人员搜索（账号/姓名模糊，供「查人员当前主题」选择人员）。
	 */
	@Select("SELECT `id`, `account`, `real_name`, `dept_id` FROM `blade_user` "
		+ "WHERE `tenant_id` = #{tenantId} AND `is_deleted` = 0 "
		+ "AND (`account` LIKE CONCAT('%', #{kw}, '%') OR `real_name` LIKE CONCAT('%', #{kw}, '%')) "
		+ "ORDER BY `id` LIMIT #{limit}")
	List<BladeUserBrief> searchUsers(@Param("tenantId") String tenantId,
		@Param("kw") String kw,
		@Param("limit") int limit);

	/**
	 * 按ID查用户简要信息。
	 */
	@Select("SELECT `id`, `account`, `real_name`, `dept_id` FROM `blade_user` WHERE `id` = #{userId}")
	BladeUserBrief selectBriefById(@Param("userId") Long userId);

	/**
	 * 批量查用户简要信息。
	 */
	@Select("<script>"
		+ "SELECT `id`, `account`, `real_name`, `dept_id` FROM `blade_user` WHERE `id` IN "
		+ "<foreach collection='userIds' item='u' open='(' separator=',' close=')'>#{u}</foreach>"
		+ "</script>")
	List<BladeUserBrief> selectBriefByIds(@Param("userIds") Collection<Long> userIds);
}
