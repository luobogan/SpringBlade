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
package org.springblade.auth.granter;

import org.springblade.auth.enums.BladeUserEnum;
import org.springblade.auth.utils.PersonStatusGuard;
import org.springblade.auth.utils.TokenUtil;
import org.springblade.common.cache.CacheNames;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.redis.cache.BladeRedis;
import org.springblade.core.secure.props.BladeAuthProperties;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DigestUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.core.tool.utils.WebUtil;
import org.springblade.system.feign.IParamClient;
import org.springblade.system.user.entity.UserInfo;
import org.springblade.system.user.feign.IUserClient;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 验证码TokenGranter
 *
 * @author Chill
 */
@Slf4j
@Component
@AllArgsConstructor
public class CaptchaTokenGranter implements ITokenGranter {

	public static final String GRANT_TYPE = "captcha";

	private IUserClient userClient;
	private IParamClient paramClient;
	private BladeRedis bladeRedis;

	private BladeAuthProperties authProperties;

	@Override
	public UserInfo grant(TokenParameter tokenParameter) {
		HttpServletRequest request = WebUtil.getRequest();

		// 按租户读取「是否开启登录验证码」(captcha_mode)，关闭则该租户免验证码。
		// 经 Feign(IParamClient) 走 Nacos 服务发现调用 blade-system 公开接口 /param/public-value，
		// 替代原 RestTemplate 写死 host:port 的 HTTP 自调用（网关/回环路径变化会导致 404）。
		// 读取失败（fallback 返回 null 或抛异常）时保持要求验证码，即 fail-closed。
		String tenantId = tokenParameter.getArgs().getStr("tenantId");
		boolean captchaRequired = true;
		try {
			R<String> result = paramClient.publicValue("captcha_mode", tenantId);
			if (result != null && result.isSuccess() && "false".equals(result.getData())) {
				captchaRequired = false;
			}
		} catch (Exception ex) {
			log.warn("读取租户验证码开关失败，默认要求验证码: {}", tenantId, ex);
		}

		String key = request.getHeader(TokenUtil.CAPTCHA_HEADER_KEY);
		String code = request.getHeader(TokenUtil.CAPTCHA_HEADER_CODE);
		// 获取验证码
		String redisCode = Func.toStr(bladeRedis.getAndDel(CacheNames.CAPTCHA_KEY + key));
		// 判断验证码（仅当该租户开启验证码时校验）
		if (captchaRequired) {
			if (code == null || !StringUtil.equalsIgnoreCase(redisCode, code)) {
				throw new ServiceException(TokenUtil.CAPTCHA_NOT_CORRECT);
			}
		}

		String account = tokenParameter.getArgs().getStr("account");
		String password = tokenParameter.getArgs().getStr("password");

		// 判断账号和IP是否锁定
		TokenUtil.checkAccountAndIpLock(bladeRedis, tenantId, account);

		UserInfo userInfo = null;
		if (Func.isNoneBlank(account, password)) {
			// 获取用户类型
			String userType = tokenParameter.getArgs().getStr("userType");
			// 解密密码
			String decryptPassword = TokenUtil.decryptPassword(password, authProperties.getPublicKey(), authProperties.getPrivateKey());
			// 定义返回结果
			R<UserInfo> result;
			// 根据不同用户类型调用对应的接口返回数据，用户可自行拓展
			if (userType.equals(BladeUserEnum.WEB.getName())) {
				result = userClient.userInfo(tenantId, account, DigestUtil.encrypt(decryptPassword));
			} else if (userType.equals(BladeUserEnum.APP.getName())) {
				result = userClient.userInfo(tenantId, account, DigestUtil.encrypt(decryptPassword));
			} else {
				result = userClient.userInfo(tenantId, account, DigestUtil.encrypt(decryptPassword));
			}
			userInfo = result.isSuccess() ? result.getData() : null;
		}

		if (userInfo == null || userInfo.getUser() == null) {
			// 处理登录失败
			TokenUtil.handleLoginFailure(bladeRedis, tenantId, account);
			log.error("用户登录失败, 账号:{}, IP:{}", account, WebUtil.getIP());
			throw new ServiceException(TokenUtil.USER_NOT_FOUND);
		} else {
			// P3-5 在职性校验：解聘(4) 拒绝登录（对齐 ecology ResourceComInfo 在职判定）
			PersonStatusGuard.check(userInfo.getUser().getPersonStatus());
			// 处理登录成功
			TokenUtil.handleLoginSuccess(bladeRedis, tenantId, account);
		}
		return userInfo;
	}

}
