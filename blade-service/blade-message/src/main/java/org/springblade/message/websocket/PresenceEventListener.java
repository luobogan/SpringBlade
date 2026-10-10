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
package org.springblade.message.websocket;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.message.service.IPresenceService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.Map;

/**
 * WS 会话事件监听：把 STOMP 连接的建立/断开映射为在线状态。
 * <p>
 * 租户ID 由 {@link MessageHandshakeHandler} 解析 JWT 后写入会话属性，
 * 此处从会话属性读取，避免依赖业务线程上下文。
 *
 * @author Chill
 */
@Component
@AllArgsConstructor
@Slf4j
public class PresenceEventListener {

	private final IPresenceService presenceService;

	/**
	 * 连接建立 → 注册在线会话。
	 * 注意：SessionConnectedEvent 此时原生会话属性尚未挂载到 STOMP 头，
	 * getSessionAttributes() 常为 null，故实际注册放到 onSubscribe（首帧订阅时属性已就绪）。
	 * 这里仅做兜底：若能取到属性则注册，取不到也不影响（订阅事件会补全）。
	 */
	@EventListener
	public void onConnected(SessionConnectedEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		register(accessor, accessor.getSessionId());
	}

	/**
	 * 连接断开 → 按会话ID注销。
	 * <p>
	 * <b>关键</b>：{@code SessionDisconnectEvent} 只带 sessionId，<b>不携带会话属性</b>，
	 * 因此不能像 onConnected/onSubscribe 那样从 {@code getSessionAttributes()} 取 tenantId
	 * （那样会直接早退，注销永不执行，用户只能等活跃窗口过期才下线）。
	 * 注册时已写入 {@code sessionId -> tenantId:userId} 索引，这里反查后精确注销。
	 */
	@EventListener
	public void onDisconnect(SessionDisconnectEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		String sessionId = event.getSessionId() != null ? event.getSessionId() : accessor.getSessionId();
		if (sessionId == null) {
			return;
		}
		presenceService.unregisterSessionById(sessionId);
		log.debug("[PRESENCE] disconnect session={}", sessionId);
	}

	/**
	 * 订阅动作视为活跃信号 → 注册/续期。
	 * 首帧订阅时原生会话属性已就绪，在此完成在线注册（onConnected 因时序可能读不到属性）。
	 * registerSession 幂等（ZADD 同成员覆盖分数），重复调用仅刷新。
	 */
	@EventListener
	public void onSubscribe(SessionSubscribeEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		log.debug("[WS-SUBSCRIBE] dest={} principal={} session={}", accessor.getDestination(), accessor.getUser(), accessor.getSessionId());
		register(accessor, accessor.getSessionId());
	}

	/**
	 * 从会话头解析出 tenantId/userId 后注册在线会话。
	 * <p>
	 * 仅用于「会话属性可读」的两个时机（首帧订阅、连接建立兜底）；断开事件无属性，
	 * 走 {@link #unregisterSessionById(String)} 的索引反查路径。
	 *
	 * @return 是否成功注册（false 表示会话属性/主体缺失，已跳过）
	 */
	private boolean register(StompHeaderAccessor accessor, String sessionId) {
		Principal principal = accessor.getUser();
		if (principal == null) {
			return false;
		}
		Map<String, Object> attributes = accessor.getSessionAttributes();
		if (attributes == null) {
			return false;
		}
		Object tenantId = attributes.get(MessageHandshakeHandler.ATTR_TENANT_ID);
		if (tenantId == null) {
			return false;
		}
		Long userId;
		try {
			userId = Long.valueOf(principal.getName());
		} catch (NumberFormatException e) {
			return false;
		}
		presenceService.registerSession(tenantId.toString(), userId, sessionId);
		return true;
	}

}
