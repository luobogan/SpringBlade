package org.springblade.portal.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 门户权限 Mapper
 * <p>blade-portal 与 blade 库同数据源，直查 blade_role_menu ⋈ blade_menu 判定授权（与 PortalDataMapper 直查 blade_notice 一致）。</p>
 */
@Mapper
public interface PortalRightMapper {

	/**
	 * 当前用户角色是否被授予某权限码
	 *
	 * @param roleIds 角色ID（逗号分隔，兼容多角色）
	 * @param code    权限码（blade_menu.code）
	 */
	@Select("SELECT COUNT(*) FROM `blade_role_menu` rm "
		+ " JOIN `blade_menu` m ON m.`id` = rm.`menu_id` "
		+ " WHERE m.`is_deleted` = 0 AND m.`code` = #{code} "
		+ "   AND FIND_IN_SET(rm.`role_id`, #{roleIds}) > 0")
	int countGranted(@Param("roleIds") String roleIds, @Param("code") String code);

	/**
	 * 当前用户角色是否包含 administrator
	 *
	 * @param roleIds 角色ID（逗号分隔）
	 */
	@Select("SELECT COUNT(*) FROM `blade_role` "
		+ " WHERE `is_deleted` = 0 AND `role_alias` = 'administrator' "
		+ "   AND FIND_IN_SET(`id`, #{roleIds}) > 0")
	int countAdminRole(@Param("roleIds") String roleIds);
}
