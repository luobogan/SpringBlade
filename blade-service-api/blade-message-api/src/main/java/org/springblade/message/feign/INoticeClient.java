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
import org.springblade.message.dto.NoticeSendDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 流程通知 Feign 客户端接口（系统代发，无登录态）
 *
 * <p>路径使用 {@code /feign/client} 前缀：网关 InnerFilter 会拒绝一切经网关的
 * {@code /feign/client/**} 外部请求，仅允许服务间直连（Nacos 服务发现）访问，
 * 对齐 {@code IWorkflowClient}（blade-workflow）既有约定。</p>
 *
 * <p>与 {@link IMessageClient}（资源路径 {@code /message/**}，需登录态）的区别：
 * 本接口专为「无登录态的内部通知」设计——按 userId 自动建/复用系统通知会话，
 * 发送者固定为系统（senderId=0），消息 {@code category=2}。</p>
 *
 * @author Chill
 */
@FeignClient(
	value = MessageConstant.APPLICATION_MESSAGE_NAME,
	fallback = INoticeClientFallback.class
)
public interface INoticeClient {

	/**
	 * 流程通知 API 前缀（网关 InnerFilter 隔离）
	 */
	String API_PREFIX = "/feign/client/notice";

	/**
	 * 批量发送流程通知（每人一条系统通知会话，自动建/复用）
	 *
	 * @param dto 通知参数（tenantId 必传，userIds/content 必传）
	 * @return 是否全部发送成功
	 */
	@PostMapping(API_PREFIX + "/send-to-users")
	R<Boolean> sendToUsers(@RequestBody NoticeSendDTO dto);

}
