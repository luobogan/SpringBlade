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
package org.springblade.gateway.provider;

import org.springblade.core.launch.constant.TokenConstant;

import java.util.ArrayList;
import java.util.List;

/**
 * 鉴权配置
 *
 * @author Chill
 */
public class AuthProvider {

	public static String AUTH_KEY = TokenConstant.HEADER;
	private static final List<String> DEFAULT_SKIP_URL = new ArrayList<>();

	static {
		DEFAULT_SKIP_URL.add("/example");
		DEFAULT_SKIP_URL.add("/token/**");
		DEFAULT_SKIP_URL.add("/captcha/**");
		DEFAULT_SKIP_URL.add("/actuator/health/**");
		DEFAULT_SKIP_URL.add("/v3/api-docs/**");
		DEFAULT_SKIP_URL.add("/auth/**");
		DEFAULT_SKIP_URL.add("/oauth/**");
		DEFAULT_SKIP_URL.add("/log/**");
		DEFAULT_SKIP_URL.add("/menu/routes");
		DEFAULT_SKIP_URL.add("/menu/auth-routes");
		DEFAULT_SKIP_URL.add("/tenant/info");
		// 登录页等场景读取的「公开参数值」接口：网关实际收到的路径带服务前缀
		// （如 /blade-system/param/public-value，/api 前缀由前端代理剥离），
		// 故放行必须带前缀，否则 AntPathMatcher 匹配不到 → 401 → 前端读不到开关、回落默认 true。
		DEFAULT_SKIP_URL.add("/param/public-value");
		DEFAULT_SKIP_URL.add("/blade-system/param/public-value");
		DEFAULT_SKIP_URL.add("/api/blade-system/param/public-value");
		DEFAULT_SKIP_URL.add("/order/create/**");
		DEFAULT_SKIP_URL.add("/storage/deduct/**");
		DEFAULT_SKIP_URL.add("/error/**");
		DEFAULT_SKIP_URL.add("/assets/**");
		DEFAULT_SKIP_URL.add("/blade-mall/front/**");
		DEFAULT_SKIP_URL.add("/blade-mall/upload/**");
		DEFAULT_SKIP_URL.add("/blade-mall/auth/**");
		DEFAULT_SKIP_URL.add("/blade-mall/file/download/**");
		DEFAULT_SKIP_URL.add("/file/download/**");
		DEFAULT_SKIP_URL.add("/v3/api-docs/**");
		DEFAULT_SKIP_URL.add("/*/v3/api-docs/**");
		DEFAULT_SKIP_URL.add("/swagger-ui/**");
		DEFAULT_SKIP_URL.add("/swagger-ui.html");
	}

	/**
	 * 默认无需鉴权的API
	 */
	public static List<String> getDefaultSkipUrl() {
		return DEFAULT_SKIP_URL;
	}

}
