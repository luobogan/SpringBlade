package org.springblade.portal.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 门户部门查询 Mapper
 * <p>blade-portal 与 blade 库同数据源，直查 blade_dept 取部门祖先链（与 PortalDataMapper 直查 blade_notice 一致）。</p>
 */
@Mapper
public interface PortalDeptMapper {

	/**
	 * 取部门祖先链（如 "0,100,"），用于"配在父部门、子孙部门也生效"的默认首页解析。
	 *
	 * @param deptId 部门ID
	 * @return ancestors 字符串；部门不存在返回 null
	 */
	@Select("SELECT `ancestors` FROM `blade_dept` WHERE `id` = #{deptId} AND `is_deleted` = 0")
	String getAncestors(@Param("deptId") Long deptId);

	/**
	 * 取分部下属部门ID集合（含分部自身），用于「按分部过滤」时对 create_dept 做 IN 过滤。
	 * ancestors 存储格式为逗号分隔且无首尾逗号（如 "1,2,3"），兼容多种匹配形态。
	 */
	@Select("SELECT `id` FROM `blade_dept` WHERE `is_deleted` = 0 "
		+ "AND (`id` = #{branchId} "
		+ "OR `ancestors` = CAST(#{branchId} AS CHAR) "
		+ "OR `ancestors` LIKE CONCAT(#{branchId}, ',%') "
		+ "OR `ancestors` LIKE CONCAT('%,', #{branchId}, ',%') "
		+ "OR `ancestors` LIKE CONCAT('%,', #{branchId}))")
	List<Long> selectBranchDeptIds(@Param("branchId") Long branchId);
}
