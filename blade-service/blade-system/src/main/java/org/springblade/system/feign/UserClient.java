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
package org.springblade.system.feign;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.system.service.IUserService;
import org.springblade.system.user.entity.User;
import org.springblade.system.user.entity.UserInfo;
import org.springblade.system.user.entity.UserOauth;
import org.springblade.system.user.feign.IUserClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户服务Feign实现类
 *
 * @author Chill
 */
@Hidden
@RestController
@AllArgsConstructor
public class UserClient implements IUserClient {

	private IUserService service;

	// 与接口保持一致的 API 前缀
	private static final String API_PREFIX = "/user";

	@Override
	public R<UserInfo> userInfo(Long userId) {
		return R.data(service.userInfo(userId));
	}

	@Override
	@GetMapping(API_PREFIX + "/user-info")
	public R<UserInfo> userInfo(String tenantId, String account, String password) {
		return R.data(service.userInfo(tenantId, account, password));
	}

	@Override
	@PostMapping(API_PREFIX + "/user-auth-info")
	public R<UserInfo> userAuthInfo(UserOauth userOauth) {
		return R.data(service.userInfo(userOauth));
	}

	@Override
	@PostMapping(API_PREFIX + "/save-user")
	public R<User> saveUser(User user) {
		User existingUser = service.getOne(Wrappers.<User>query().lambda()
			.eq(User::getTenantId, user.getTenantId())
			.eq(User::getAccount, user.getAccount()));
		if (existingUser != null) {
			return R.data(existingUser);
		}
		boolean saved = service.save(user);
		if (saved) {
			return R.data(service.getById(user.getId()));
		}
		return R.<User>fail("创建用户失败");
	}

	@Override
	@PostMapping(API_PREFIX + "/save-user-oauth")
	public R<Boolean> saveUserOauth(UserOauth userOauth) {
		return R.data(service.saveUserOauth(userOauth));
	}

	@Override
	@GetMapping(API_PREFIX + "/user-by-account")
	public R<User> getUserByAccount(String tenantId, String account) {
		User user = service.getOne(Wrappers.<User>query().lambda()
			.eq(User::getTenantId, tenantId)
			.eq(User::getAccount, account));
		return R.data(user);
	}

}
