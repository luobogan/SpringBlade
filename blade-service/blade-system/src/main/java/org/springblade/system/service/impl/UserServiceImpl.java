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
package org.springblade.system.service.impl;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springblade.common.cache.CacheNames;
import org.springblade.common.constant.CommonConstant;
import org.springblade.core.cache.constant.CacheConstant;
import org.springblade.core.cache.utils.CacheUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.redis.cache.BladeRedis;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tenant.TenantGuard;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.utils.*;
import org.springblade.message.dto.MessageSendDTO;
import org.springblade.message.dto.SessionCreateDTO;
import org.springblade.message.feign.IMessageClient;
import org.springblade.message.vo.SessionVO;
import lombok.extern.slf4j.Slf4j;
import org.springblade.system.entity.Tenant;
import org.springblade.system.feign.ISysClient;
import org.springblade.system.service.IUserCompleteStatusService;
import org.springblade.system.user.entity.User;
import org.springblade.system.user.entity.UserInfo;
import org.springblade.system.user.entity.UserOauth;
import org.springblade.system.user.vo.UserVO;
import org.springblade.system.excel.UserExcel;
import org.springblade.system.mapper.UserMapper;
import org.springblade.system.service.IDeptService;
import org.springblade.system.service.IPostService;
import org.springblade.system.service.IRoleService;
import org.springblade.system.service.ITenantService;
import org.springblade.system.service.IUserOauthService;
import org.springblade.system.service.IUserService;
import org.springblade.system.service.IUserExtDataService;
import org.springblade.system.service.IHrmFieldService;
import org.springblade.system.service.IWorkCodeRuleService;
import org.springblade.system.vo.UserFormSchemaVO;
import org.springblade.system.wrapper.UserWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.springblade.core.tenant.TenantGuard.EntityType.ROLE;
import static org.springblade.core.tenant.TenantGuard.EntityType.USER;

/**
 * 服务实现类
 *
 * @author Chill
 */
@Slf4j
@Service
public class UserServiceImpl extends BaseServiceImpl<UserMapper, User> implements IUserService {
	private static final String GUEST_NAME = "guest";
	private static final String MINUS_ONE = "-1";
	/** 人员状态默认值：正式（对齐 ecology AddResourceBaseCmd 新建默认 status=1） */
	private static final Integer DEFAULT_PERSON_STATUS = 1;
	/** SM2 密文前缀（与 blade-auth TokenUtil.ENCRYPT_PREFIX 一致），用于识别前端加密密码 */
	private static final String SM2_PREFIX = "04";

	private final IDeptService deptService;
	private final IPostService postService;
	private final IRoleService roleService;
	private final ISysClient sysClient;
	private final IUserOauthService userOauthService;
	private final BladeRedis bladeRedis;
	private final ITenantService tenantService;
	private final IWorkCodeRuleService workCodeRuleService;
	private final IUserExtDataService userExtDataService;
	private final IHrmFieldService hrmFieldService;
	private final IUserCompleteStatusService userCompleteStatusService;
	private final IMessageClient messageClient;

	/** SM2 私钥（Nacos blade.auth.private-key，与前端 defaultSettings.auth.publicKey 配对），用于解密创建/改密接口的密码 */
	@Value("${blade.auth.private-key:}")
	private String sm2PrivateKey;

	/** 默认初始密码（可配置，对齐 ecology ChgPasswdReminder；Nacos blade.default-password，缺省 123456） */
	@Value("${blade.default-password:123456}")
	private String defaultPassword;

	@Autowired
	public UserServiceImpl(IDeptService deptService, IPostService postService, IRoleService roleService,
		ISysClient sysClient, IUserOauthService userOauthService, BladeRedis bladeRedis,
		ITenantService tenantService, IWorkCodeRuleService workCodeRuleService,
		IUserExtDataService userExtDataService, IHrmFieldService hrmFieldService,
		IUserCompleteStatusService userCompleteStatusService, IMessageClient messageClient) {
		this.deptService = deptService;
		this.postService = postService;
		this.roleService = roleService;
		this.sysClient = sysClient;
		this.userOauthService = userOauthService;
		this.bladeRedis = bladeRedis;
		this.tenantService = tenantService;
		this.workCodeRuleService = workCodeRuleService;
		this.userExtDataService = userExtDataService;
		this.hrmFieldService = hrmFieldService;
		this.userCompleteStatusService = userCompleteStatusService;
		this.messageClient = messageClient;
	}

	/**
	 * 密码解析：前端（UserAdd/UserEdit）提交时用 SM2 公钥加密，此处用 blade.auth.private-key 解密
	 * 还原明文再交给 DigestUtil 摘要存储（与登录链路 TokenUtil.decryptPassword 完全一致）。
	 * <p>
	 * 前端 sm-crypto 产出的密文不带 "04" 前缀，故解密前按登录链路规则补齐前缀；若解密失败
	 * （如 Excel 导入 / 旧链路传入的明文密码），则原样返回，保持向后兼容。
	 */
	private String resolvePassword(String raw) {
		if (Func.isEmpty(raw)) {
			return raw;
		}
		// 与登录链路保持一致：SM2 密文可能不带 04 前缀，解密前补齐
		String toDecrypt = SM2_PREFIX.equals(raw.length() > 2 ? raw.substring(0, 2) : "") ? raw : SM2_PREFIX + raw;
		try {
			String decrypted = SM2Util.decrypt(toDecrypt, sm2PrivateKey);
			if (Func.isNotEmpty(decrypted)) {
				return decrypted;
			}
		} catch (Exception e) {
			// 解密失败：视为非加密明文（Excel 导入 / 旧链路），回退原值
		}
		return raw;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submit(User user) {
		TenantGuard.bindTenant(this, user, USER);
		if (Func.isEmpty(user.getTenantId())) {
			throw new ServiceException("租户ID不能为空");
		}
		TenantGuard.verifyBatch(roleService, Func.toLongList(user.getRoleId()), ROLE);
		// 工号缺省（新增）时按编码规则自动生成（对齐 ecology CodeRuleManager；规则未配置则保持空）
		if (Func.isEmpty(user.getId()) && Func.isEmpty(user.getWorkCode())) {
			user.setWorkCode(workCodeRuleService.generate(user.getTenantId(), "USER"));
		}
		return doSubmit(user);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean update(User user) {
		TenantGuard.bindTenant(this, user, USER);
		TenantGuard.verifyBatch(roleService, Func.toLongList(user.getRoleId()), ROLE);
		return doSubmit(user);
	}

	/**
	 * ⚠ INTERNAL ONLY ⚠ 跳过 TenantGuard 的内部入口
	 * <p>
	 * 仅做密码加密 + 账号唯一性校验，<strong>不做任何租户归属校验</strong>。
	 * 调用方必须在外层确保以下两点：
	 * <ol>
	 *   <li>{@code user.tenantId} 来自可信源（OAuth 上下文 / 当前会话），
	 *       <strong>绝不允许前端入参直接通过此方法落库</strong>。</li>
	 *   <li>已经完成对应业务的鉴权检查。</li>
	 * </ol>
	 * 错误调用会导致跨租户数据泄漏。新增调用点请提交 PR 时 @ 安全负责人 review。
	 */
	private boolean doSubmit(User user) {
		CacheUtil.clear(CacheConstant.USER_CACHE);
		if (Func.isNotEmpty(user.getPassword())) {
			user.setPassword(DigestUtil.encrypt(resolvePassword(user.getPassword())));
		}
		if (Func.isNotEmpty(user.getAccount())) {
			Long cnt = baseMapper.selectCount(Wrappers.<User>query().lambda()
				.eq(User::getTenantId, user.getTenantId())
				.eq(User::getAccount, user.getAccount())
				.ne(Func.isNotEmpty(user.getId()), User::getId, user.getId()));
			if (cnt > 0) {
				throw new ServiceException("当前账号已被使用!");
			}
		}
		// 工号租户内唯一（DB 唯一键 uk_blade_user_tenant_workcode 兜底；对齐 ecology hrmResourceCheck）
		if (Func.isNotEmpty(user.getWorkCode())) {
			Long workCodeCnt = baseMapper.selectCount(Wrappers.<User>query().lambda()
				.eq(User::getTenantId, user.getTenantId())
				.eq(User::getWorkCode, user.getWorkCode())
				.ne(Func.isNotEmpty(user.getId()), User::getId, user.getId()));
			if (workCodeCnt > 0) {
				throw new ServiceException("当前工号已被使用!");
			}
		}
		// 证件号唯一（对齐 ecology HrmResourceAddService#save 的证件号校验）
		if (Func.isNotEmpty(user.getCertificateNum())) {
			Long certCnt = baseMapper.selectCount(Wrappers.<User>query().lambda()
				.eq(User::getTenantId, user.getTenantId())
				.eq(User::getCertificateNum, user.getCertificateNum())
				.ne(Func.isNotEmpty(user.getId()), User::getId, user.getId()));
			if (certCnt > 0) {
				throw new ServiceException("当前证件号已被使用!");
			}
		}
		boolean isCreate = Func.isEmpty(user.getId());
		boolean saved = isCreate ? save(user) : updateById(user);
		// P2 伴生写入：自定义字段值落扩展表（对齐 ecology cus_fielddata；与主表同事务）
		if (saved && Func.isNotEmpty(user.getExtData())) {
			userExtDataService.saveExtData(user.getId(), user.getExtData());
		}
		// P3-3 伴生初始化：新用户信息完善度（对齐 ecology HrmInfoStatus；与主表同事务）
		if (saved && isCreate) {
			userCompleteStatusService.initDefault(user.getId());
			// P3-4 消息通知：事务提交成功后才发，失败仅记日志不回滚建档
			notifyManagerOnCreate(user);
		}
		return saved;
	}

	/**
	 * P3-4：建档成功后向直属主管（managerId）发送待办/欢迎消息（对齐 Ecology 人员创建后的组织联动）
	 * <p>注册为事务提交后回调：消息中心抖动不得导致建档失败。</p>
	 */
	private void notifyManagerOnCreate(User user) {
		Long managerId = user.getManagerId();
		if (managerId == null || managerId <= 0) {
			return;
		}
		Long newUserId = user.getId();
		String newUserName = Func.isNotBlank(user.getRealName()) ? user.getRealName() : user.getName();
		String account = user.getAccount();
		try {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				return;
			}
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					sendManagerMessage(managerId, newUserId, newUserName, account);
				}
			});
		} catch (Exception e) {
			log.warn("[P3-4] 注册建档消息回调失败. userId={}, {}", newUserId, e.getMessage());
		}
	}

	/** 实际发送消息：取/建两人会话 → 发送；任一步失败仅记日志 */
	private void sendManagerMessage(Long managerId, Long newUserId, String newUserName, String account) {
		try {
			SessionCreateDTO sessionDto = new SessionCreateDTO();
			sessionDto.setMemberIds(List.of(managerId));
			R<SessionVO> sessionRes = messageClient.createSession(sessionDto);
			if (sessionRes == null || !sessionRes.isSuccess() || sessionRes.getData() == null
				|| sessionRes.getData().getId() == null) {
				log.warn("[P3-4] 创建会话失败，跳过消息通知. managerId={}, msg={}",
					managerId, sessionRes == null ? "null" : sessionRes.getMsg());
				return;
			}
			MessageSendDTO sendDto = new MessageSendDTO();
			sendDto.setSessionId(sessionRes.getData().getId());
			sendDto.setContentType(1);
			sendDto.setContent("新成员[" + newUserName + "]（账号：" + account + "）已建档，请及时完善其信息并分配工作。");
			R<Boolean> sendRes = messageClient.send(sendDto);
			if (sendRes == null || !Boolean.TRUE.equals(sendRes.getData())) {
				log.warn("[P3-4] 发送消息失败. managerId={}, newUserId={}, msg={}",
					managerId, newUserId, sendRes == null ? "null" : sendRes.getMsg());
			}
		} catch (Exception e) {
			log.warn("[P3-4] 建档消息通知异常. managerId={}, newUserId={}, {}", managerId, newUserId, e.getMessage());
		}
	}

	@Override
	public boolean remove(List<Long> userIds) {
		CacheUtil.clear(CacheConstant.USER_CACHE);
		TenantGuard.verifyBatch(this, userIds, USER);
		// 逻辑删除 × 唯一键对策：置空工号，保证原工号可被复用（见改造文档 §6 风险表）
		this.update(Wrappers.<User>update().lambda()
			.set(User::getWorkCode, null)
			.in(User::getId, userIds));
		return deleteLogic(userIds);
	}

	@Override
	public boolean unlock(List<Long> userIds) {
		List<User> userList = TenantGuard.verifyBatch(this, userIds, USER);
		userList.forEach(user -> bladeRedis.del(CacheNames.tenantKey(user.getTenantId(), CacheNames.USER_FAIL_KEY, user.getAccount())));
		return true;
	}

	@Override
	public boolean updateUserInfo(User user) {
		CacheUtil.clear(CacheConstant.USER_CACHE);
		// 用户修改自身信息强制指定当前请求账号的ID
		user.setId(SecureUtil.getUserId());
		User currentUser = getById(user.getId());
		if (currentUser == null) {
			throw new ServiceException("用户不存在!");
		}
		// 用户修改自身信息强制忽略角色、部门、账号等字段
		user.setRoleId(null);
		user.setDeptId(null);
		user.setAccount(null);
		user.setPassword(null);
		user.setUpdateTime(DateUtil.now());
		return updateById(user);
	}

	@Override
	public IPage<User> selectUserPage(IPage<User> page, User user) {
		return page.setRecords(baseMapper.selectUserPage(page, user));
	}

	@Override
	public IPage<UserVO> selectPage(Map<String, Object> user, Query query) {
		QueryWrapper<User> queryWrapper = Condition.getQueryWrapper(user, User.class);
		if (!SecureUtil.isAdministrator()) {
			queryWrapper.lambda().eq(User::getTenantId, SecureUtil.getTenantId());
		}
		return UserWrapper.build().pageVO(page(Condition.getPage(query), queryWrapper));
	}

	@Override
	public UserInfo userInfo(Long userId) {
		UserInfo userInfo = new UserInfo();
		User user = baseMapper.selectById(userId);
		userInfo.setUser(user);
		if (Func.isNotEmpty(user)) {
			List<String> roleAlias = baseMapper.getRoleAlias(Func.toStrArray(user.getRoleId()));
			userInfo.setRoles(roleAlias);
		}
		return userInfo;
	}

	@Override
	public UserInfo userInfo(String tenantId, String account, String password) {
		UserInfo userInfo = new UserInfo();
		User user = baseMapper.getUser(tenantId, account, password);
		userInfo.setUser(user);
		if (Func.isNotEmpty(user)) {
			List<String> roleAlias = baseMapper.getRoleAlias(Func.toStrArray(user.getRoleId()));
			userInfo.setRoles(roleAlias);
		}
		return userInfo;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public UserInfo userInfo(UserOauth userOauth) {
		UserOauth uo = userOauthService.getOne(Wrappers.<UserOauth>query().lambda()
			.eq(UserOauth::getTenantId, userOauth.getTenantId())
			.eq(UserOauth::getUuid, userOauth.getUuid())
			.eq(UserOauth::getSource, userOauth.getSource()));
		UserInfo userInfo;
		if (Func.isNotEmpty(uo) && Func.isNotEmpty(uo.getUserId())) {
			userInfo = this.userInfo(uo.getUserId());
			userInfo.setOauthId(Func.toStr(uo.getId()));
		} else {
			userInfo = new UserInfo();
			if (Func.isEmpty(uo)) {
				userOauthService.save(userOauth);
				userInfo.setOauthId(Func.toStr(userOauth.getId()));
			} else {
				userInfo.setOauthId(Func.toStr(uo.getId()));
			}
			User user = new User();
			user.setAccount(userOauth.getUsername());
			userInfo.setUser(user);
			userInfo.setRoles(Collections.singletonList(GUEST_NAME));
		}
		return userInfo;
	}

	@Override
	public boolean grant(String userIds, String roleIds) {
		CacheUtil.clear(CacheConstant.USER_CACHE);
		List<Long> idList = Func.toLongList(userIds);
		TenantGuard.verifyBatch(this, idList, USER);
		TenantGuard.verifyBatch(roleService, Func.toLongList(roleIds), ROLE);
		User user = new User();
		user.setRoleId(roleIds);
		return this.update(user, Wrappers.<User>update().lambda().in(User::getId, idList));
	}

	@Override
	public boolean resetPassword(String userIds) {
		CacheUtil.clear(CacheConstant.USER_CACHE);
		List<Long> idList = Func.toLongList(userIds);
		TenantGuard.verifyBatch(this, idList, USER);
		User user = new User();
		user.setPassword(DigestUtil.encrypt(defaultPassword));
		user.setUpdateTime(DateUtil.now());
		return this.update(user, Wrappers.<User>update().lambda().in(User::getId, idList));
	}

	@Override
	public boolean updatePassword(Long userId, String oldPassword, String newPassword, String newPassword1) {
		CacheUtil.clear(CacheConstant.USER_CACHE);
		User user = getById(userId);
		if (!newPassword.equals(newPassword1)) {
			throw new ServiceException("请输入正确的确认密码!");
		}
		if (!user.getPassword().equals(DigestUtil.encrypt(resolvePassword(oldPassword)))) {
			throw new ServiceException("原密码不正确!");
		}
		return this.update(Wrappers.<User>update().lambda().set(User::getPassword, DigestUtil.encrypt(resolvePassword(newPassword))).eq(User::getId, userId));
	}

	@Override
	public List<String> getRoleName(String roleIds) {
		if (Func.isEmpty(roleIds)) {
			return Collections.emptyList();
		}
		String[] ids = Func.toStrArray(roleIds);
		if (ids == null || ids.length == 0) {
			return Collections.emptyList();
		}
		return baseMapper.getRoleName(ids);
	}

	@Override
	public List<String> getDeptName(String deptIds) {
		return baseMapper.getDeptName(Func.toStrArray(deptIds));
	}

	@Override
	public List<String> importUser(List<UserExcel> data) {
		List<String> errors = new ArrayList<>();
		if (Func.isEmpty(data)) {
			return errors;
		}
		// 强制使用当前会话租户，禁止 Excel 内容决定租户归属
		String currentTenantId = SecureUtil.getTenantId();
		for (int i = 0; i < data.size(); i++) {
			UserExcel userExcel = data.get(i);
			if (userExcel == null) {
				continue;
			}
			int rowNum = userExcel.getRowNum() == null ? i + 1 : userExcel.getRowNum();
			String rowTip = "第" + rowNum + "行" + (Func.isNotBlank(userExcel.getAccount()) ? "（账号：" + userExcel.getAccount() + "）" : "");
			try {
				userExcel.setTenantId(currentTenantId);
				User user = Objects.requireNonNull(BeanUtil.copyProperties(userExcel, User.class));
				user.setTenantId(currentTenantId);
				// 人员状态缺省取"正式"（对齐 ecology AddResourceBaseCmd 默认 status=1）
				if (user.getPersonStatus() == null) {
					user.setPersonStatus(DEFAULT_PERSON_STATUS);
				}
				// 设置部门ID
				user.setDeptId(sysClient.getDeptIds(currentTenantId, userExcel.getDeptName()));
				// 设置岗位ID
				user.setPostId(sysClient.getPostIds(currentTenantId, userExcel.getPostName()));
				// 设置角色ID
				user.setRoleId(sysClient.getRoleIds(currentTenantId, userExcel.getRoleName()));
				// 设置默认密码（可配置，对齐 resetPassword）
				user.setPassword(defaultPassword);
				this.submit(user);
			} catch (Exception e) {
				// 单行失败不影响其余行：记录到行级明细（工号/账号/证件号重复等均在 submit 校验链中抛出）
				errors.add(rowTip + "：" + Func.toStr(e.getMessage(), "导入失败"));
			}
		}
		return errors;
	}

	@Override
	public List<UserExcel> exportUser(Wrapper<User> queryWrapper) {
		List<UserExcel> userList = baseMapper.exportUser(queryWrapper);
		userList.forEach(user -> {
			user.setRoleName(StringUtil.join(sysClient.getRoleNames(user.getRoleId())));
			user.setDeptName(StringUtil.join(sysClient.getDeptNames(user.getDeptId())));
			user.setPostName(StringUtil.join(sysClient.getPostNames(user.getPostId())));
		});
		return userList;
	}

	@Override
	public List<UserExcel> exportUser(Map<String, Object> user) {
		QueryWrapper<User> queryWrapper = Condition.getQueryWrapper(user, User.class);
		if (!SecureUtil.isAdministrator()) {
			queryWrapper.lambda().eq(User::getTenantId, SecureUtil.getTenantId());
		}
		queryWrapper.lambda().eq(User::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		return exportUser(queryWrapper);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean registerGuest(User user, Long oauthId) {
		Tenant tenant = tenantService.getOne(Wrappers.<Tenant>lambdaQuery().eq(Tenant::getTenantId, user.getTenantId()));
		if (tenant == null || tenant.getId() == null) {
			throw new ServiceException("租户信息错误!");
		}
		// 第一步：先取出 OAuth 上下文。oauthId 由 OAuth 第一步授权回调写回前端，
		// 是攻击者难以伪造的可信关联——以此为锚点反推 tenantId。
		UserOauth userOauth = userOauthService.getById(oauthId);
		if (userOauth == null || userOauth.getId() == null) {
			throw new ServiceException("第三方登陆信息错误!");
		}
		// ⚡ 优先采用 OAuth 上下文中已绑定的 tenantId（来自 OAuth 第一步可信源），
		// 强制覆盖前端传入的 user.tenantId，防止匿名接口被用于跨租户植入账户。
		// 仅当 OAuth 流程未绑定 tenantId 时才回退到入参（向后兼容历史 OAuth 流程）。
		if (Func.isNotEmpty(userOauth.getTenantId())) {
			user.setTenantId(userOauth.getTenantId());
		}
		R<Tenant> result = sysClient.getTenant(user.getTenantId());
		Tenant tenantInfo = result.getData();
		if (!result.isSuccess() || tenantInfo == null || tenantInfo.getId() == null) {
			throw new ServiceException("租户信息错误!");
		}
		user.setRealName(user.getName());
		user.setAvatar(userOauth.getAvatar());
		user.setRoleId(MINUS_ONE);
		user.setDeptId(MINUS_ONE);
		user.setPostId(MINUS_ONE);
		// 第三方注册为匿名上下文，绕过 TenantGuard 直接走 doSubmit；
		// tenantId 已在上方强制锚定到 OAuth 上下文，可信源已确定。
		boolean userTemp = doSubmit(user);
		userOauth.setUserId(user.getId());
		userOauth.setTenantId(user.getTenantId());
		boolean oauthTemp = userOauthService.updateById(userOauth);
		return (userTemp && oauthTemp);
	}

	@Override
	public boolean saveUserOauth(UserOauth userOauth) {
		return userOauthService.save(userOauth);
	}

	@Override
	public boolean checkUserFieldUnique(String field, String value, String tenantId, Long excludeId) {
		if (Func.isBlank(field) || Func.isBlank(value)) {
			return false;
		}
		// 超管可指定任意租户预检；其他用户强制当前会话租户（对齐 selectPage 的租户口径）
		String resolvedTenant = SecureUtil.isAdministrator()
			? Func.toStr(tenantId, SecureUtil.getTenantId())
			: SecureUtil.getTenantId();
		Long cnt;
		switch (field) {
			case "account":
				cnt = baseMapper.selectCount(Wrappers.<User>query().lambda()
					.eq(User::getTenantId, resolvedTenant)
					.eq(User::getAccount, value)
					.ne(Func.isNotEmpty(excludeId), User::getId, excludeId));
				break;
			case "workCode":
				cnt = baseMapper.selectCount(Wrappers.<User>query().lambda()
					.eq(User::getTenantId, resolvedTenant)
					.eq(User::getWorkCode, value)
					.ne(Func.isNotEmpty(excludeId), User::getId, excludeId));
				break;
			case "certificateNum":
				cnt = baseMapper.selectCount(Wrappers.<User>query().lambda()
					.eq(User::getTenantId, resolvedTenant)
					.eq(User::getCertificateNum, value)
					.ne(Func.isNotEmpty(excludeId), User::getId, excludeId));
				break;
			default:
				throw new ServiceException("不支持预检的字段: " + field);
		}
		return cnt == null || cnt == 0;
	}

	@Override
	public String nextWorkCode(String tenantId) {
		String resolvedTenant = SecureUtil.isAdministrator()
			? Func.toStr(tenantId, SecureUtil.getTenantId())
			: SecureUtil.getTenantId();
		// 生成后循环校验唯一性（撞库自动重取，最多 10 次；对齐 ecology 编码规则预留号段思想）
		for (int i = 0; i < 10; i++) {
			String candidate = workCodeRuleService.generate(resolvedTenant, "USER");
			if (Func.isBlank(candidate)) {
				return null;
			}
			Long cnt = baseMapper.selectCount(Wrappers.<User>query().lambda()
				.eq(User::getTenantId, resolvedTenant)
				.eq(User::getWorkCode, candidate));
			if (cnt == null || cnt == 0) {
				return candidate;
			}
		}
		throw new ServiceException("工号生成失败: 序列冲突次数过多，请检查编码规则");
	}

	@Override
	public Map<Long, String> getExtData(Long userId) {
		return userExtDataService.getExtData(userId);
	}

	@Override
	public List<UserFormSchemaVO> formSchema(String tenantId) {
		return hrmFieldService.formSchema(tenantId);
	}

	@Override
	public Map<String, Integer> getCompleteStatus(Long userId) {
		return userCompleteStatusService.getByUserId(userId);
	}

}
