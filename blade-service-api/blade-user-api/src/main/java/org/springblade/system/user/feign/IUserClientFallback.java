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

import org.springblade.core.tool.api.R;
import org.springblade.system.user.entity.UserInfo;
import org.springblade.system.user.entity.UserOauth;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Feign失败配置
 *
 * @author Chill
 */
@Component
public class IUserClientFallback implements IUserClient {

	@Override
	public R<UserInfo> userInfo(Long userId) {
		return R.fail("未获取到账号信息");
	}

	@Override
	public R<UserInfo> userInfo(String tenantId, String account, String password) {
		return R.fail("未获取到账号信息");
	}

	@Override
	public R<UserInfo> userAuthInfo(UserOauth userOauth) {
		return R.fail("未获取到账号信息");
	}

	@Override
	public R<UserInfo> saveUser(UserInfo user) {
		return R.<UserInfo>fail("创建用户失败");
	}

	@Override
	public R<Boolean> saveUserOauth(UserOauth userOauth) {
		return R.<Boolean>fail("保存OAuth信息失败");
	}

	@Override
	public R<UserInfo> getUserByAccount(String tenantId, String account) {
		return R.fail("查询用户信息失败");
	}

	@Override
	public R<UserInfo> getUserByPhone(String tenantId, String phone) {
		return R.fail("查询用户信息失败");
	}

	// ---------- 组织维度查用户ID：降级返回失败（非空实现），调用方按「空集合 + 告警」处理 ----------

	@Override
	public R<List<Long>> userIdsByDept(Long deptId, Boolean containChild) {
		return R.fail("按部门查询用户失败");
	}

	@Override
	public R<List<Long>> userIdsByRole(Long roleId) {
		return R.fail("按角色查询用户失败");
	}

	@Override
	public R<List<Long>> userIdsByPost(Long postId) {
		return R.fail("按岗位查询用户失败");
	}

	@Override
	public R<List<Long>> allUserIds(Integer limit) {
		return R.fail("查询全部用户失败");
	}

	@Override
	public R<Long> leaderId(Long userId) {
		return R.fail("查询用户主管失败");
	}

	@Override
	public R<List<Long>> userIdsByDeptOfUser(Long userId, Boolean containChild) {
		return R.fail("按用户部门查询用户失败");
	}

	@Override
	public R<Boolean> verifyPassword(Long userId, String password) {
		return R.fail("校验用户密码失败");
	}
}
