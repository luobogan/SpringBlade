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
import org.springblade.message.dto.SessionCreateDTO;
import org.springblade.message.vo.SessionVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

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
	 * 内部 Feign 端点前缀（网关 InnerFilter 拒绝经网关的外部访问）
	 */
	String INNER_API_PREFIX = "/feign/client/message";

	/**
	 * 发送消息
	 *
	 * @param dto 发送参数（会话ID + 内容 + 可选引用/附件）
	 * @return 是否发送成功
	 */
	@PostMapping(API_PREFIX + "/message/send")
	R<Boolean> send(@RequestBody MessageSendDTO dto);

	/**
	 * 创建会话（两人会话幂等复用：成员完全相同则直接返回既有会话）
	 * <p>P3-4：建档后向直属主管发消息前需先取得会话ID（{@code MessageSendDTO.sessionId} 为事实必填）。</p>
	 *
	 * @param dto 会话成员（当前登录用户会自动补齐为成员之一）
	 * @return 会话信息（含 id）
	 */
	@PostMapping(API_PREFIX + "/session/create")
	R<SessionVO> createSession(@RequestBody SessionCreateDTO dto);

	/**
	 * 查询指定用户的未读红点总数（门户消息元素专用内部端点）
	 *
	 * <p>门户的「消息提醒」卡片由 blade-portal 服务端聚合后随元素数据返回，
	 * 故需要一条服务间只读端点；用户由调用方显式传入（门户传当前登录用户ID）。</p>
	 *
	 * @param userId 用户ID
	 * @return 未读总数（无未读返回 0）
	 */
	@GetMapping(INNER_API_PREFIX + "/unread-count")
	R<Long> unreadCount(@RequestParam("userId") Long userId);

	/**
	 * 查询指定用户的最近消息（门户消息元素专用内部端点）
	 *
	 * <p>跨会话取最新 N 条消息摘要（id/sessionId/senderId/category/content/createTime），
	 * 供门户「消息提醒」卡片渲染列表。userId 由调用方显式传入。</p>
	 *
	 * @param userId 用户ID
	 * @param limit  条数（服务端默认 8，上限 50）
	 * @return 消息摘要列表（服务异常时返回空列表，不抛错）
	 */
	@GetMapping(INNER_API_PREFIX + "/recent")
	R<List<Map<String, Object>>> recent(
		@RequestParam("userId") Long userId,
		@RequestParam(value = "limit", required = false) Integer limit);

}
