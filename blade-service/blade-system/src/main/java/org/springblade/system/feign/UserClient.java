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
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.system.mapper.UserMapper;
import org.springblade.system.service.IUserService;
import org.springblade.system.user.entity.User;
import org.springblade.system.user.entity.UserInfo;
import org.springblade.system.user.entity.UserOauth;
import org.springblade.system.user.feign.IUserClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用户服务Feign实现类
 *
 * @author Chill
 */
@Slf4j
@Hidden
@RestController
@AllArgsConstructor
public class UserClient implements IUserClient {

	private IUserService service;

	/** 组织维度查询（按部门/角色/岗位/全部）直接走 Mapper，避免 select * 后内存过滤 */
	private UserMapper userMapper;

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
	public R<UserInfo> saveUser(UserInfo userInfo) {
		User user = userInfo.getUser();
		User existingUser = service.getOne(Wrappers.<User>query().lambda()
			.eq(User::getTenantId, user.getTenantId())
			.eq(User::getAccount, user.getAccount()));
		if (existingUser != null) {
			// 用户已存在，执行更新操作
			user.setId(existingUser.getId());
			// 忽略不应更新的字段
			user.setRoleId(null);
			user.setDeptId(null);
			user.setAccount(null);
			user.setPassword(null);
			// 保护关键字段：个人资料更新不应修改 wx_openid
			user.setWxOpenid(null);
			user.setOpenId(null);
			user.setUpdateTime(DateUtil.now());
			service.updateById(user);
			UserInfo resultInfo = new UserInfo();
			resultInfo.setUser(service.getById(existingUser.getId()));
			return R.data(resultInfo);
		}
		boolean saved = service.save(user);
		if (saved) {
			UserInfo resultInfo = new UserInfo();
			resultInfo.setUser(service.getById(user.getId()));
			return R.data(resultInfo);
		}
		return R.<UserInfo>fail("创建用户失败");
	}

	@Override
	@PostMapping(API_PREFIX + "/save-user-oauth")
	public R<Boolean> saveUserOauth(UserOauth userOauth) {
		return R.data(service.saveUserOauth(userOauth));
	}

	@Override
	@GetMapping(API_PREFIX + "/user-by-account")
	public R<UserInfo> getUserByAccount(String tenantId, String account) {
		User user = service.getOne(Wrappers.<User>query().lambda()
			.eq(User::getTenantId, tenantId)
			.eq(User::getAccount, account));
		UserInfo resultInfo = new UserInfo();
		resultInfo.setUser(user);
		return R.data(resultInfo);
	}

	@Override
	@GetMapping(API_PREFIX + "/user-by-phone")
	public R<UserInfo> getUserByPhone(String tenantId, String phone) {
		log.info("根据手机号查询用户 - tenantId: {}, phone: {}", tenantId, phone);
		// 打印 SQL 查询条件
		log.info("执行 SQL: SELECT * FROM blade_user WHERE tenant_id = '{}' AND phone = '{}'", tenantId, phone);
		// 使用 list() 避免 TooManyResultsException，取第一条记录
		java.util.List<User> users = service.list(Wrappers.<User>query().lambda()
			.eq(User::getTenantId, tenantId)
			.eq(User::getPhone, phone));
		log.info("查询结果 - list 数量: {}, list: {}", users != null ? users.size() : 0, users);

		// 只有当查询到有效用户（ID 不为 null）时才返回成功
		if (users == null || users.isEmpty()) {
			log.info("未找到用户，返回失败");
			return R.<UserInfo>fail("用户不存在");
		}

		User user = users.get(0);
		if (user == null || user.getId() == null) {
			log.info("用户对象为空或ID为null，返回失败");
			return R.<UserInfo>fail("用户不存在");
		}

		log.info("最终返回 - 用户存在: true, userId: {}", user.getId());
		UserInfo resultInfo = new UserInfo();
		resultInfo.setUser(user);
		return R.data(resultInfo);
	}

	// ==================== 组织维度查用户ID（供流程节点操作者解析） ====================

	/** 「所有人」类操作者默认返回上限 */
	private static final int DEFAULT_ALL_USER_LIMIT = 2000;
	/** 「所有人」类操作者最大返回上限 */
	private static final int MAX_ALL_USER_LIMIT = 5000;

	/**
	 * 按部门查询用户ID（含下级部门时先取子孙部门，再一次性按集合匹配）
	 */
	@Override
	@GetMapping(API_PREFIX + "/user-ids-by-dept")
	public R<List<Long>> userIdsByDept(Long deptId, Boolean containChild) {
		if (deptId == null) {
			return R.data(Collections.emptyList());
		}
		List<Long> deptIds = new ArrayList<>(8);
		deptIds.add(deptId);
		if (Boolean.TRUE.equals(containChild)) {
			List<Long> children = userMapper.selectChildDeptIds(deptId);
			if (children != null && !children.isEmpty()) {
				deptIds.addAll(children);
			}
		}
		List<Long> ids = userMapper.selectUserIdsByDeptIds(deptIds);
		log.info("按部门查询用户 - deptId: {}, containChild: {}, deptCount: {}, userCount: {}",
			deptId, containChild, deptIds.size(), ids == null ? 0 : ids.size());
		return R.data(ids == null ? Collections.emptyList() : ids);
	}

	/**
	 * 按角色查询用户ID
	 */
	@Override
	@GetMapping(API_PREFIX + "/user-ids-by-role")
	public R<List<Long>> userIdsByRole(Long roleId) {
		if (roleId == null) {
			return R.data(Collections.emptyList());
		}
		List<Long> ids = userMapper.selectUserIdsByRoleId(roleId);
		log.info("按角色查询用户 - roleId: {}, userCount: {}", roleId, ids == null ? 0 : ids.size());
		return R.data(ids == null ? Collections.emptyList() : ids);
	}

	/**
	 * 按岗位查询用户ID
	 */
	@Override
	@GetMapping(API_PREFIX + "/user-ids-by-post")
	public R<List<Long>> userIdsByPost(Long postId) {
		if (postId == null) {
			return R.data(Collections.emptyList());
		}
		List<Long> ids = userMapper.selectUserIdsByPostId(postId);
		log.info("按岗位查询用户 - postId: {}, userCount: {}", postId, ids == null ? 0 : ids.size());
		return R.data(ids == null ? Collections.emptyList() : ids);
	}

	/**
	 * 查询全部有效用户ID（「所有人」类操作者；limit 兜底，避免万级用户一次拉爆）
	 */
	@Override
	@GetMapping(API_PREFIX + "/user-ids-all")
	public R<List<Long>> allUserIds(Integer limit) {
		int size = (limit == null || limit <= 0) ? DEFAULT_ALL_USER_LIMIT : Math.min(limit, MAX_ALL_USER_LIMIT);
		List<Long> ids = userMapper.selectAllUserIds(size);
		log.info("查询全部用户 - limit: {}, userCount: {}", size, ids == null ? 0 : ids.size());
		return R.data(ids == null ? Collections.emptyList() : ids);
	}

	/**
	 * 取指定用户的「主管」用户ID（上级 / 直线经理）
	 */
	@Override
	@GetMapping(API_PREFIX + "/leader-id")
	public R<Long> leaderId(Long userId) {
		if (userId == null) {
			return R.data(null);
		}
		Long managerId = userMapper.selectManagerId(userId);
		log.info("查询用户主管 - userId: {}, managerId: {}", userId, managerId);
		return R.data(managerId);
	}

	/**
	 * 取指定用户「所在部门」的成员用户ID（含下级部门时一并纳入子孙部门成员）
	 */
	@Override
	@GetMapping(API_PREFIX + "/user-ids-by-dept-of-user")
	public R<List<Long>> userIdsByDeptOfUser(Long userId, Boolean containChild) {
		if (userId == null) {
			return R.data(Collections.emptyList());
		}
		List<Long> ids = userMapper.selectUserIdsByDeptOfUser(userId, containChild);
		log.info("按用户部门查询用户 - userId: {}, containChild: {}, userCount: {}",
			userId, containChild, ids == null ? 0 : ids.size());
		return R.data(ids == null ? Collections.emptyList() : ids);
	}

	/**
	 * 校验用户密码（流程节点「二次认证」）
	 */
	@Override
	@GetMapping(API_PREFIX + "/verify-password")
	public R<Boolean> verifyPassword(Long userId, String password) {
		if (userId == null || password == null || password.isEmpty()) {
			return R.data(false);
		}
		User user = service.getById(userId);
		if (user == null || user.getPassword() == null) {
			return R.data(false);
		}
		boolean ok = user.getPassword().equals(org.springblade.core.tool.utils.DigestUtil.encrypt(password));
		log.info("校验用户密码 - userId: {}, matched: {}", userId, ok);
		return R.data(ok);
	}

}
