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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.net.URI;
import java.security.Principal;
import java.util.Base64;
import java.util.Map;

/**
 * 握手处理器：从 URL 查询参数 ?token= 解析 JWT 负载中的 user_id / tenant_id，
 * 其中 user_id 作为 STOMP 用户目的地（/user/{userId}/queue/...）的路由依据，
 * tenant_id 写入会话属性，供在线状态按租户隔离。
 *
 * @author Chill
 */
@Slf4j
public class MessageHandshakeHandler extends DefaultHandshakeHandler {

	/**
	 * 会话属性键：租户ID（由 JWT 解析，供在线状态监听器读取）
	 */
	public static final String ATTR_TENANT_ID = "tenantId";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
		String token = extractToken(request.getURI());
		if (token == null) {
			return null;
		}
		JsonNode payload = parsePayload(token);
		if (payload == null) {
			return null;
		}
		String tenantId = text(payload, "tenant_id", "tenantId");
		if (tenantId != null) {
			attributes.put(ATTR_TENANT_ID, tenantId);
		}
		String userId = text(payload, "user_id", "userId");
		if (userId == null) {
			return null;
		}
		final String uid = userId;
		log.debug("[WS-HANDSHAKE] 设置 STOMP 主体 principal={} tenant={}", uid, tenantId);
		return () -> uid;
	}

	private String extractToken(URI uri) {
		String query = uri.getQuery();
		if (query == null) {
			return null;
		}
		for (String pair : query.split("&")) {
			int idx = pair.indexOf('=');
			if (idx < 0) {
				continue;
			}
			String key = pair.substring(0, idx);
			if ("token".equals(key) || "access_token".equals(key)) {
				return pair.substring(idx + 1);
			}
		}
		return null;
	}

	private JsonNode parsePayload(String token) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length < 2) {
				return null;
			}
			String payload = new String(Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
			return MAPPER.readTree(payload);
		} catch (Exception e) {
			return null;
		}
	}

	private String text(JsonNode node, String... keys) {
		for (String key : keys) {
			JsonNode value = node.get(key);
			if (value != null && !value.isNull()) {
				String text = value.asText();
				if (text != null && !text.isEmpty()) {
					return text;
				}
			}
		}
		return null;
	}

}
