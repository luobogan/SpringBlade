/**
 * Copyright (c) 2018-2099, Chill Zhuang 庄骞 (bladejava@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.system.user.entity.User;
import org.springblade.system.excel.UserExcel;

import java.util.List;

/**
 * Mapper 接口
 *
 * @author Chill
 */
public interface UserMapper extends BaseMapper<User> {

	/**
	 * 自定义分页
	 *
	 * @param page  分页参数
	 * @param user  用户查询条件
	 * @return 用户分页数据
	 */
	List<User> selectUserPage(IPage page, User user);

	/**
	 * 获取用户
	 *
	 * @param tenantId 租户ID
	 * @param account  账号
	 * @param password 密码
	 * @return 用户信息
	 */
	User getUser(String tenantId, String account, String password);

	/**
	 * 获取角色名
	 *
	 * @param ids 角色ID数组
	 * @return 角色名称集合
	 */
	List<String> getRoleName(String[] ids);

	/**
	 * 获取角色别名
	 *
	 * @param ids 角色ID数组
	 * @return 角色别名集合
	 */
	List<String> getRoleAlias(String[] ids);

	/**
	 * 获取部门名
	 *
	 * @param ids 部门ID数组
	 * @return 部门名称集合
	 */
	List<String> getDeptName(String[] ids);

	/**
	 * 获取导出用户数据
	 *
	 * @param queryWrapper 查询条件包装器
	 * @return 用户导出数据集合
	 */
	List<UserExcel> exportUser(@Param("ew") Wrapper<User> queryWrapper);

	/**
	 * 根据角色ID查询用户列表
	 *
	 * @param roleId
	 * @return
	 */
	List<User> selectUsersByRoleId(Long roleId);

	// ------------------------------------------------------------------
	// 组织维度查用户ID（供流程节点操作者解析：部门/角色/岗位/所有人）
	// blade_user 的 dept_id / role_id / post_id 为逗号串，SQL 用 FIND_IN_SET 匹配
	// ------------------------------------------------------------------

	/**
	 * 按一组部门ID查用户ID（多值匹配）
	 *
	 * @param deptIds 部门ID集合（调用方保证非空）
	 * @return 用户ID集合
	 */
	List<Long> selectUserIdsByDeptIds(@Param("deptIds") List<Long> deptIds);

	/**
	 * 按角色ID查用户ID
	 *
	 * @param roleId 角色ID
	 * @return 用户ID集合
	 */
	List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);

	/**
	 * 按岗位ID查用户ID
	 *
	 * @param postId 岗位ID
	 * @return 用户ID集合
	 */
	List<Long> selectUserIdsByPostId(@Param("postId") Long postId);

	/**
	 * 取某部门的全部子孙部门ID（ancestors 逗号串，不含自身）
	 *
	 * @param deptId 部门ID
	 * @return 子孙部门ID集合
	 */
	List<Long> selectChildDeptIds(@Param("deptId") Long deptId);

	/**
	 * 全部有效用户ID（「所有人」类操作者使用）
	 *
	 * @param limit 返回上限
	 * @return 用户ID集合
	 */
	List<Long> selectAllUserIds(@Param("limit") Integer limit);

	/**
	 * 取指定用户的主管用户ID（上级 / 直线经理）
	 *
	 * @param userId 用户ID
	 * @return 主管用户ID；无主管或用户不存在时返回 null
	 */
	Long selectManagerId(@Param("userId") Long userId);

	/**
	 * 取指定用户「所在部门」的成员用户ID（「本部门」类操作者使用）
	 *
	 * @param userId       用户ID（以其 dept_id 作为目标部门）
	 * @param containChild 是否包含下级部门（true 时纳入子孙部门成员）
	 * @return 成员用户ID集合
	 */
	List<Long> selectUserIdsByDeptOfUser(@Param("userId") Long userId, @Param("containChild") Boolean containChild);

}
