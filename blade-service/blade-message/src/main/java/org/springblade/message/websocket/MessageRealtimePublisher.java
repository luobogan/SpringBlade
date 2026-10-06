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

import org.springblade.message.vo.MessageVO;
import org.springblade.message.vo.UnreadCountVO;

import java.util.List;

/**
 * 消息实时推送发布接口
 *
 * @author Chill
 */
public interface MessageRealtimePublisher {

	/**
	 * 发布新消息事件（各实例订阅后推送给在线成员）
	 *
	 * @param sessionId 会话ID
	 * @param tenantId  租户ID（用于隔离频道，防串租户）
	 * @param message   消息视图
	 * @param userIds   需接收的成员ID（含发送人）
	 */
	void publishNewMessage(Long sessionId, String tenantId, MessageVO message, List<Long> userIds);

	/**
	 * 发布未读红点更新
	 *
	 * @param userId 用户ID
	 * @param unread 未读聚合
	 */
	void publishUnread(Long userId, UnreadCountVO unread);

	/**
	 * 发布已读回执事件（会话内其他成员已读后，通知发送方刷新「已读/未读」）
	 *
	 * @param sessionId 会话ID
	 * @param tenantId  租户ID
	 * @param userIds   需要收到回执刷新的用户ID（互动的另一方）
	 */
	void publishRead(Long sessionId, String tenantId, List<Long> userIds);

}
