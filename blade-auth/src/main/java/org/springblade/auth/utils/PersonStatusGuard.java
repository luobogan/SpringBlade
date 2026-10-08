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
package org.springblade.auth.utils;

import org.springblade.core.log.exception.ServiceException;

/**
 * 人员状态（在职性）登录守卫（P3-5）
 * <p>
 * 对齐 Ecology {@code ResourceComInfo} 的在职判定：在职集合 = {@code 0,1,2,3,5}，
 * <b>仅 4（解聘）</b> 拒绝登录；退休(5) 仍属在职，可正常登录。
 * 存量数据 {@code person_status} 为 NULL 时按在职放行（向后兼容）。
 * </p>
 * <p>
 * 三个 Granter（password / captcha / refresh_token）统一在此判定：
 * 少了 refresh_token 一环，已登录的离职账号可无限续期，拦截形同虚设。
 * </p>
 *
 * @author Chill
 */
public final class PersonStatusGuard {

	/** 解聘（对齐 ecology hrmresource.status=4，即离职） */
	public static final int DISMISS = 4;
	/** 拒绝登录提示语 */
	public static final String ACCOUNT_DISABLED = "账号已停用";

	private PersonStatusGuard() {
	}

	/**
	 * 是否允许登录
	 *
	 * @param personStatus 人员状态
	 * @return true 允许
	 */
	public static boolean loginAllowed(Integer personStatus) {
		if (personStatus == null) {
			return true;
		}
		return !Integer.valueOf(DISMISS).equals(personStatus);
	}

	/**
	 * 校验在职性，非在职直接抛业务异常（经 {@code BladeRestExceptionTranslator} 转为 400 + 明确提示）
	 *
	 * @param personStatus 人员状态
	 */
	public static void check(Integer personStatus) {
		if (!loginAllowed(personStatus)) {
			throw new ServiceException(ACCOUNT_DISABLED);
		}
	}

}
