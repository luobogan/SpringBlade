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
package org.springblade.system;

import org.springblade.core.launch.BladeApplication;
import org.springblade.core.launch.constant.AppConstant;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 系统服务启动类
 *
 * @author Chill
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {
	AppConstant.BASE_PACKAGES,
	"org.springblade"
})
// 预防性修复：自定义 BladeFeignSentinel 要求 Feign fallback 必须是 Spring Bean；
// 默认只扫本包(org.springblade.system)，消费跨包 fallback(如 org.springblade.pay.feign.*)
// 时会报 No fallback instance found，故显式把 org.springblade 纳入组件扫描。
@ComponentScan(basePackages = "org.springblade")
public class SystemApplication {

	public static void main(String[] args) {
		BladeApplication.run(AppConstant.APPLICATION_SYSTEM_NAME, SystemApplication.class, args);
	}

}
