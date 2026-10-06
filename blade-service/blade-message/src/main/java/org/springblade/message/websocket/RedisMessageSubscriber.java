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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.message.service.IMessageService;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 订阅者：收到集群广播后，经本实例 WS Broker 推送给在线成员
 *
 * @author Chill
 */
@Component
@AllArgsConstructor
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

	private final SimpMessagingTemplate messagingTemplate;
	private final ObjectMapper messageObjectMapper;
	private final IMessageService messageService;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		try {
			MessageRealtimeEvent event = messageObjectMapper.readValue(message.getBody(), MessageRealtimeEvent.class);
			log.debug("[REALTIME-RX] 收到集群广播 type={} userIds={} pattern={}", event.getType(), event.getUserIds(), pattern == null ? null : new String(pattern));
			if ("NEW_MESSAGE".equals(event.getType())) {
				for (Long userId : event.getUserIds()) {
					String uid = String.valueOf(userId);
					log.debug("[REALTIME-TX] 准备 convertAndSendToUser uid={} dest=/queue/message", uid);
					messagingTemplate.convertAndSendToUser(uid, "/queue/message", event.getMessage());
					messagingTemplate.convertAndSendToUser(uid, "/queue/unread", messageService.unreadCount(userId));
				}
			} else if ("UNREAD".equals(event.getType())) {
				messagingTemplate.convertAndSendToUser(String.valueOf(event.getUserId()), "/queue/unread", event.getUnread());
			} else if ("READ".equals(event.getType())) {
				// 已读回执：通知互动的另一方刷新会话内消息的「已读/未读」标记
				if (event.getUserIds() != null) {
					for (Long userId : event.getUserIds()) {
						messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/read", event);
					}
				}
			}
		} catch (Exception e) {
			log.error("消息实时推送处理失败", e);
		}
	}

}
