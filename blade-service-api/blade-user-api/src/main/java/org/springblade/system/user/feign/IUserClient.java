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
package org.springblade.system.user.feign;


import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.tool.api.R;
import org.springblade.system.user.entity.UserInfo;
import org.springblade.system.user.entity.UserOauth;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * User Feign接口类
 *
 * @author Chill
 */
@FeignClient(
	value = AppConstant.APPLICATION_SYSTEM_NAME,
	fallback = IUserClientFallback.class
)
public interface IUserClient {

	String API_PREFIX = "/user";

	/**
	 * 获取用户信息
	 *
	 * @param userId 用户id
	 * @return
	 */
	@GetMapping(API_PREFIX + "/user-info-by-id")
	R<UserInfo> userInfo(@RequestParam("userId") Long userId);

	/**
	 * 获取用户信息
	 *
	 * @param tenantId 租户ID
	 * @param account    账号
	 * @param password   密码
	 * @return
	 */
	@GetMapping(API_PREFIX + "/user-info")
	R<UserInfo> userInfo(@RequestParam("tenantId") String tenantId, @RequestParam("account") String account, @RequestParam("password") String password);

	/**
	 * 获取第三方平台信息
	 *
	 * @param userOauth 第三方授权用户信息
	 * @return UserInfo
	 */
	@PostMapping(API_PREFIX + "/user-auth-info")
	R<UserInfo> userAuthInfo(@RequestBody UserOauth userOauth);

	/**
	 * 新建用户
	 *
	 * @param user 用户实体
	 * @return 保存后的用户对象（含数据库生成的ID）
	 */
	@PostMapping(API_PREFIX + "/save-user")
	R<UserInfo> saveUser(@RequestBody UserInfo user);

	/**
	 * 保存第三方授权用户信息
	 *
	 * @param userOauth 第三方授权用户信息
	 * @return
	 */
	@PostMapping(API_PREFIX + "/save-user-oauth")
	R<Boolean> saveUserOauth(@RequestBody UserOauth userOauth);

	/**
	 * 根据租户ID和账号查询用户
	 *
	 * @param tenantId 租户ID
	 * @param account  账号（微信登录时为openId）
	 * @return 用户对象
	 */
	@GetMapping(API_PREFIX + "/user-by-account")
	R<UserInfo> getUserByAccount(@RequestParam("tenantId") String tenantId, @RequestParam("account") String account);

	/**
	 * 根据租户ID和手机号查询用户
	 *
	 * @param tenantId 租户ID
	 * @param phone    手机号
	 * @return 用户对象
	 */
	@GetMapping(API_PREFIX + "/user-by-phone")
	R<UserInfo> getUserByPhone(@RequestParam("tenantId") String tenantId, @RequestParam("phone") String phone);

	// ------------------------------------------------------------------
	// 以下为「组织维度查用户ID」能力，供流程节点操作者解析（部门/角色/岗位/所有人）使用。
	// 说明：blade_user 的 dept_id / role_id / post_id 均为 varchar(1000) 逗号串，
	//      服务端统一用 FIND_IN_SET 匹配；租户条件由租户拦截器自动注入，无需入参。
	// ------------------------------------------------------------------

	/**
	 * 按部门查询用户ID集合
	 *
	 * @param deptId       部门ID
	 * @param containChild 是否包含下级部门（true 时含所有子孙部门）
	 * @return 用户ID集合
	 */
	@GetMapping(API_PREFIX + "/user-ids-by-dept")
	R<List<Long>> userIdsByDept(@RequestParam("deptId") Long deptId,
								@RequestParam(value = "containChild", required = false) Boolean containChild);

	/**
	 * 按角色查询用户ID集合
	 *
	 * @param roleId 角色ID
	 * @return 用户ID集合
	 */
	@GetMapping(API_PREFIX + "/user-ids-by-role")
	R<List<Long>> userIdsByRole(@RequestParam("roleId") Long roleId);

	/**
	 * 按岗位查询用户ID集合
	 *
	 * @param postId 岗位ID
	 * @return 用户ID集合
	 */
	@GetMapping(API_PREFIX + "/user-ids-by-post")
	R<List<Long>> userIdsByPost(@RequestParam("postId") Long postId);

	/**
	 * 查询全部有效用户ID（用于「所有人」类操作者）
	 *
	 * @param limit 返回上限（为空时服务端取默认上限，防止万级用户一次拉爆）
	 * @return 用户ID集合
	 */
	@GetMapping(API_PREFIX + "/user-ids-all")
	R<List<Long>> allUserIds(@RequestParam(value = "limit", required = false) Integer limit);

	/**
	 * 取指定用户的「主管」用户ID（上级 / 直线经理，贴近 E9 ManagerID）。
	 *
	 * @param userId 用户ID
	 * @return 主管用户ID；无主管或用户不存在时返回 null
	 */
	@GetMapping(API_PREFIX + "/leader-id")
	R<Long> leaderId(@RequestParam("userId") Long userId);

	/**
	 * 取指定用户「所在部门」的成员用户ID集合（用于「本部门」类操作者）。
	 *
	 * @param userId       用户ID（以其 dept_id 作为目标部门）
	 * @param containChild 是否包含下级部门
	 * @return 成员用户ID集合
	 */
	@GetMapping(API_PREFIX + "/user-ids-by-dept-of-user")
	R<List<Long>> userIdsByDeptOfUser(@RequestParam("userId") Long userId,
									  @RequestParam(value = "containChild", required = false) Boolean containChild);

	/**
	 * 校验用户密码（用于流程节点「二次认证」设置）。
	 *
	 * @param userId   用户ID
	 * @param password 明文密码（服务端按登录同规则加密后比对）
	 * @return true=密码正确
	 */
	@GetMapping(API_PREFIX + "/verify-password")
	R<Boolean> verifyPassword(@RequestParam("userId") Long userId, @RequestParam("password") String password);

}
