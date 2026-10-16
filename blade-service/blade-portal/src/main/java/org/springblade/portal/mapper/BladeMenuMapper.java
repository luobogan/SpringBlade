package org.springblade.portal.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 系统菜单只读 Mapper（对标泛微 LeftMenu/MainMenu 同一套引擎）
 * <p>对应表：{@code blade_menu}（blade-system 维护）。dev 多服务共用数据源，
 * 门户「前端/后端菜单」直读系统菜单，按角色授权过滤（blade_role_menu）。</p>
 */
@Mapper
public interface BladeMenuMapper {

	/**
	 * 租户下可导航的系统菜单（category=1 菜单，排除按钮；按排序）。
	 */
	@Select("SELECT `id`, `parent_id`, `code`, `name`, `alias`, `path`, `sort`, `category`, `is_open`, `is_component` "
		+ "FROM `blade_menu` "
		+ "WHERE `tenant_id` = #{tenantId} AND `is_deleted` = 0 AND `status` = 1 AND `category` = 1 "
		+ "ORDER BY `sort`, `id`")
	List<org.springblade.portal.entity.BladeMenu> selectNavMenus(@Param("tenantId") String tenantId);

	/**
	 * 角色被授权的菜单ID集合（blade_role_menu）。
	 */
	@Select("<script>"
		+ "SELECT `menu_id` FROM `blade_role_menu` WHERE `role_id` IN "
		+ "<foreach collection='roleIds' item='r' open='(' separator=',' close=')'>#{r}</foreach>"
		+ "</script>")
	List<Long> selectMenuIdsByRoleIds(@Param("roleIds") Collection<Long> roleIds);
}
