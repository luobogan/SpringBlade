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
package org.springblade.message.feign;

import org.springblade.core.tool.api.R;
import org.springblade.message.constant.MessageConstant;
import org.springblade.message.dto.MessageSendDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 消息中心 Feign 客户端接口
 *
 * <p>供工作流等跨服务模块在业务事件发生时调用，向相关人员发送消息。
 * 路径使用资源路径（Cloud 约定，网关按服务名路由，不经网关时亦直连）。</p>
 *
 * @author Chill
 */
@FeignClient(
	value = MessageConstant.APPLICATION_MESSAGE_NAME,
	fallback = IMessageClientFallback.class
)
public interface IMessageClient {

	/**
	 * 消息发送 API 前缀
	 */
	String API_PREFIX = "/message";

	/**
	 * 发送消息
	 *
	 * @param dto 发送参数（会话ID + 内容 + 可选引用/附件）
	 * @return 是否发送成功
	 */
	@PostMapping(API_PREFIX + "/message/send")
	R<Boolean> send(@RequestBody MessageSendDTO dto);

}
