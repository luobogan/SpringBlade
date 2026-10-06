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
import org.springblade.message.vo.MessageVO;
import org.springblade.message.vo.UnreadCountVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于 Redis 发布订阅的消息实时推送发布器
 *
 * @author Chill
 */
@Component
@AllArgsConstructor
@Slf4j
public class RedisMessageRealtimePublisher implements MessageRealtimePublisher {

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper messageObjectMapper;

	private static final String CHANNEL_PREFIX = "message:";

	@Override
	public void publishNewMessage(Long sessionId, String tenantId, MessageVO message, List<Long> userIds) {
		MessageRealtimeEvent event = new MessageRealtimeEvent();
		event.setType("NEW_MESSAGE");
		event.setSessionId(sessionId);
		event.setTenantId(tenantId);
		event.setMessage(message);
		event.setUserIds(userIds);
		publish(CHANNEL_PREFIX + tenantId + ":" + sessionId, event);
	}

	@Override
	public void publishUnread(Long userId, UnreadCountVO unread) {
		MessageRealtimeEvent event = new MessageRealtimeEvent();
		event.setType("UNREAD");
		event.setUserId(userId);
		event.setUnread(unread);
		publish(CHANNEL_PREFIX + "unread:" + userId, event);
	}

	@Override
	public void publishRead(Long sessionId, String tenantId, List<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return;
		}
		MessageRealtimeEvent event = new MessageRealtimeEvent();
		event.setType("READ");
		event.setSessionId(sessionId);
		event.setTenantId(tenantId);
		event.setUserIds(userIds);
		publish(CHANNEL_PREFIX + "read:" + tenantId + ":" + sessionId, event);
	}

	private void publish(String channel, MessageRealtimeEvent event) {
		try {
			stringRedisTemplate.convertAndSend(channel, messageObjectMapper.writeValueAsString(event));
		} catch (Exception e) {
			log.error("消息实时事件发布失败 channel={}", channel, e);
		}
	}

}
