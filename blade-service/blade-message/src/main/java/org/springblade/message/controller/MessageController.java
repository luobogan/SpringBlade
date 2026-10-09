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
package org.springblade.message.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.swagger.annotation.ApiOrder;
import org.springblade.core.tool.api.R;
import org.springblade.message.dto.MessageSendDTO;
import org.springblade.message.service.IMessageService;
import org.springblade.message.vo.MessageVO;
import org.springblade.message.vo.UnreadCountVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息控制器
 *
 * <p><b>为什么不 {@code implements IMessageClient}</b>：该 Feign 契约把「消息」与「会话」两类
 * 资源路径聚合在一起 —— {@code /message/message/send}、{@code /message/message/read}、
 * {@code /message/message/unread-count} 由本类提供，而 {@code /message/session/create}
 * 已由 {@link SessionController#create} 提供。强行 implements 会要求本类再声明
 * {@code createSession}，从而与 SessionController 产生<b>同一路径的重复映射</b>，
 * Spring 启动阶段即因 ambiguous mapping 失败。Feign 客户端按路径调用，不依赖服务端的 implements 关系。</p>
 *
 * @author Chill
 */
@RestController
@RequestMapping("message")
@AllArgsConstructor
@ApiOrder
@Tag(name = "消息", description = "消息接口")
public class MessageController extends BladeController {

	private final IMessageService messageService;

	/**
	 * 会话消息分页
	 */
	@GetMapping("/session/{id}/messages")
	@Operation(summary = "会话消息分页", description = "按时间正序返回会话内消息")
	public R<IPage<MessageVO>> messages(@PathVariable("id") Long id, Query query, BladeUser user) {
		return R.data(messageService.pageMessages(id, query, user));
	}

	/**
	 * 发送消息
	 */
	@PostMapping("/message/send")
	@Operation(summary = "发送消息", description = "落库并实时推送新消息与未读红点")
	public R<Boolean> send(@RequestBody MessageSendDTO dto) {
		BladeUser user = AuthUtil.getUser();
		return R.status(messageService.send(dto, user));
	}

	/**
	 * 标记会话已读
	 */
	@PostMapping("/message/read")
	@Operation(summary = "标记会话已读", description = "补回执并清零会话未读")
	public R<Boolean> read(@RequestParam Long sessionId, BladeUser user) {
		return R.status(messageService.markRead(sessionId, user));
	}

	/**
	 * 未读红点聚合
	 */
	@GetMapping("/message/unread-count")
	@Operation(summary = "未读红点聚合", description = "顶部铃铛全局未读")
	public R<UnreadCountVO> unreadCount(BladeUser user) {
		return R.data(messageService.unreadCount(user));
	}

}
