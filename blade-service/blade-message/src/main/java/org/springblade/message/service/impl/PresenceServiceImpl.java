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
package org.springblade.message.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.message.service.IPresenceService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 在线状态服务实现：Redis ZSET（活跃时间戳）+ 会话计数。
 *
 * @author Chill
 */
@Service
@AllArgsConstructor
@Slf4j
public class PresenceServiceImpl implements IPresenceService {

	private final StringRedisTemplate stringRedisTemplate;

	/**
	 * 活跃时间戳 ZSET 前缀，成员为 userId，分数为最后活跃毫秒
	 */
	private static final String KEY_PREFIX = "message:presence:";

	/**
	 * 在线会话集合键前缀（同一用户多标签页/多端并存时保存多个 sessionId）
	 */
	private static final String SESSION_PREFIX = "message:presence:sessions:";

	/**
	 * 活跃有效窗口（毫秒）：超过该时长未续期即视为离线。
	 * 客户端心跳间隔 60s，此处留 90s 冗余，避免心跳抖动误判离线。
	 */
	private static final long ACTIVE_WINDOW_MS = 90_000L;

	/**
	 * 会话集合兜底过期时间：防止实例崩溃导致会话集合长期残留而永久「在线」
	 */
	private static final Duration SESSION_TTL = Duration.ofHours(12);

	@Override
	public void registerSession(String tenantId, Long userId, String sessionId) {
		if (!valid(tenantId, userId) || sessionId == null) {
			return;
		}
		String sessionKey = sessionKey(tenantId, userId);
		stringRedisTemplate.opsForSet().add(sessionKey, sessionId);
		stringRedisTemplate.expire(sessionKey, SESSION_TTL);
		markActive(tenantId, userId);
		log.debug("[PRESENCE] register tenant={} user={} session={}", tenantId, userId, sessionId);
	}

	@Override
	public void unregisterSession(String tenantId, Long userId, String sessionId) {
		if (!valid(tenantId, userId) || sessionId == null) {
			return;
		}
		String sessionKey = sessionKey(tenantId, userId);
		stringRedisTemplate.opsForSet().remove(sessionKey, sessionId);
		Long remaining = stringRedisTemplate.opsForSet().size(sessionKey);
		if (remaining == null || remaining <= 0) {
			stringRedisTemplate.delete(sessionKey);
			stringRedisTemplate.opsForZSet().remove(zsetKey(tenantId), String.valueOf(userId));
			log.debug("[PRESENCE] offline tenant={} user={}", tenantId, userId);
		} else {
			log.debug("[PRESENCE] unregister tenant={} user={} remaining={}", tenantId, userId, remaining);
		}
	}

	@Override
	public void refresh(String tenantId, Long userId) {
		if (!valid(tenantId, userId)) {
			return;
		}
		markActive(tenantId, userId);
	}

	@Override
	public List<Long> onlineUserIds(String tenantId) {
		if (tenantId == null || tenantId.isEmpty()) {
			return Collections.emptyList();
		}
		String key = zsetKey(tenantId);
		// 先清理超出活跃窗口的成员，避免实例非正常退出后残留为「在线」
		stringRedisTemplate.opsForZSet().removeRangeByScore(key, 0D, (double) (System.currentTimeMillis() - ACTIVE_WINDOW_MS));
		Set<String> ids = stringRedisTemplate.opsForZSet().range(key, 0, -1);
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		List<Long> result = new ArrayList<>(ids.size());
		for (String id : ids) {
			try {
				result.add(Long.valueOf(id));
			} catch (NumberFormatException ignored) {
				// 忽略非法成员
			}
		}
		return result;
	}

	@Override
	public boolean isOnline(String tenantId, Long userId) {
		if (!valid(tenantId, userId)) {
			return false;
		}
		Double score = stringRedisTemplate.opsForZSet().score(zsetKey(tenantId), String.valueOf(userId));
		return score != null && score >= (double) (System.currentTimeMillis() - ACTIVE_WINDOW_MS);
	}

	private void markActive(String tenantId, Long userId) {
		stringRedisTemplate.opsForZSet().add(zsetKey(tenantId), String.valueOf(userId), System.currentTimeMillis());
	}

	private String zsetKey(String tenantId) {
		return KEY_PREFIX + tenantId;
	}

	private String sessionKey(String tenantId, Long userId) {
		return SESSION_PREFIX + tenantId + ":" + userId;
	}

	private boolean valid(String tenantId, Long userId) {
		return tenantId != null && !tenantId.isEmpty() && userId != null;
	}

}
