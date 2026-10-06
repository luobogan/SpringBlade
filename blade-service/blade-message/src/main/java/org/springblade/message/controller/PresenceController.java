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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.swagger.annotation.ApiOrder;
import org.springblade.core.tool.api.R;
import org.springblade.message.service.IPresenceService;
import org.springblade.message.websocket.MessageHandshakeHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 在线状态控制器：提供租户在线用户查询，以及 WS 心跳续期入口。
 *
 * @author Chill
 */
@RestController
@RequestMapping("message/presence")
@AllArgsConstructor
@ApiOrder
@Slf4j
@Tag(name = "在线状态", description = "消息中心在线状态接口")
public class PresenceController extends BladeController {

	private final IPresenceService presenceService;

	/**
	 * 本租户在线用户ID列表
	 */
	@GetMapping("/online")
	@Operation(summary = "本租户在线用户", description = "返回当前租户下处于在线状态的用户ID列表")
	public R<List<Long>> online(BladeUser user) {
		if (user == null) {
			return R.data(Collections.emptyList());
		}
		return R.data(presenceService.onlineUserIds(user.getTenantId()));
	}

	/**
	 * WS 心跳：客户端定时发送（/app/presence/ping），仅续期活跃时间戳，防止长连接被误判离线。
	 */
	@MessageMapping("presence/ping")
	public void ping(SimpMessageHeaderAccessor accessor) {
		Principal principal = accessor.getUser();
		if (principal == null) {
			return;
		}
		Map<String, Object> attributes = accessor.getSessionAttributes();
		if (attributes == null) {
			return;
		}
		Object tenantId = attributes.get(MessageHandshakeHandler.ATTR_TENANT_ID);
		if (tenantId == null) {
			return;
		}
		try {
			presenceService.refresh(tenantId.toString(), Long.valueOf(principal.getName()));
		} catch (Exception e) {
			log.warn("在线状态心跳续期失败 user={}", principal.getName());
		}
	}

}
