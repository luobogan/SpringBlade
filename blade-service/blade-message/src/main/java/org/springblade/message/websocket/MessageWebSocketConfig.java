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
import org.springframework.context.annotation.Bean;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 消息中心 WebSocket 配置（STOMP over SockJS）
 *
 * @author Chill
 */
@Configuration
@EnableWebSocketMessageBroker
public class MessageWebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final ObjectMapper objectMapper;

	public MessageWebSocketConfig(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Bean
	public MessageHandshakeHandler messageHandshakeHandler() {
		return new MessageHandshakeHandler();
	}

	/**
	 * 注册消息转换器：默认 broker 仅含 String/ByteArray，无法序列化 VO，
	 * 补充 MappingJackson2MessageConverter 以支持实时消息/未读红点的 JSON 推送。
	 */
	@Override
	public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
		messageConverters.add(new StringMessageConverter());
		messageConverters.add(new ByteArrayMessageConverter());
		MappingJackson2MessageConverter jackson = new MappingJackson2MessageConverter();
		jackson.setObjectMapper(objectMapper);
		messageConverters.add(jackson);
		return true;
	}

	/**
	 * Redis 订阅容器：按 message:* 模式监听集群广播
	 */
	@Bean
	public RedisMessageListenerContainer messageRedisListenerContainer(RedisConnectionFactory factory, RedisMessageSubscriber subscriber) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(factory);
		container.addMessageListener(subscriber, new PatternTopic("message:*"));
		return container;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws/message")
			.setAllowedOriginPatterns("*")
			.setHandshakeHandler(messageHandshakeHandler())
			.withSockJS();
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// 注意：简单 broker 不能包含用户目的地前缀 "/user"，否则会与 setUserDestinationPrefix("/user")
		// 冲突，导致 convertAndSendToUser 的私信消息被简单 broker 在用户目的地翻译前/后被错误消费，最终投不出去。
		// 用户目的地（/user/...）由 setUserDestinationPrefix 单独处理。
		registry.enableSimpleBroker("/topic", "/queue");
		registry.setApplicationDestinationPrefixes("/app");
		registry.setUserDestinationPrefix("/user");
	}

}
