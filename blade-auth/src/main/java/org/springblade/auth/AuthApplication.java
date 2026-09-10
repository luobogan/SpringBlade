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
package org.springblade.auth;


import org.springblade.core.cloud.client.BladeCloudApplication;
import org.springblade.core.launch.BladeApplication;
import org.springblade.core.launch.constant.AppConstant;
import org.springframework.context.annotation.ComponentScan;

/**
 * 用户认证服务器
 *
 * @author Chill
 */
@BladeCloudApplication
// 自定义 BladeFeignSentinel 要求 Feign fallback 必须是 Spring Bean；
// @BladeCloudApplication 默认只扫本模块包(org.springblade.auth)，扫不到 api 模块里的
// fallback(@Component，如 org.springblade.system.user.feign.IUserClientFallback)，
// 故显式把 org.springblade 纳入组件扫描（与 @EnableFeignClients 的扫描范围对称）。
@ComponentScan(basePackages = "org.springblade")
public class AuthApplication {

	public static void main(String[] args) {
		BladeApplication.run(AppConstant.APPLICATION_AUTH_NAME, AuthApplication.class, args);
	}

}
