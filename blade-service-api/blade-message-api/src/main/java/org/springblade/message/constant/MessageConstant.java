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
package org.springblade.message.constant;

/**
 * 消息中心模块常量
 *
 * @author Chill
 */
public interface MessageConstant {

	/**
	 * 消息中心服务名称（Nacos 注册名）
	 */
	String APPLICATION_MESSAGE_NAME = "blade-message";

	/**
	 * 消息中心前端 API 前缀（Umi dev proxy 剥离 /api 后，网关路由 /blade-message/**）
	 */
	String API_PREFIX = "/api/blade-message";

}
