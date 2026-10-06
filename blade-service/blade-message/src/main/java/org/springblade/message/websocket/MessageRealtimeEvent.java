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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.springblade.message.vo.MessageVO;
import org.springblade.message.vo.UnreadCountVO;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 实时推送事件（经 Redis 发布订阅在集群各实例间广播）
 *
 * @author Chill
 */
@Data
public class MessageRealtimeEvent implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 事件类型 NEW_MESSAGE / UNREAD / READ
	 */
	private String type;

	/**
	 * 会话ID
	 */
	@JsonSerialize(using = ToStringSerializer.class)
	private Long sessionId;

	/**
	 * 租户ID
	 */
	private String tenantId;

	/**
	 * 用户ID（UNREAD 事件）
	 */
	@JsonSerialize(using = ToStringSerializer.class)
	private Long userId;

	/**
	 * 新消息（NEW_MESSAGE 事件）
	 */
	private MessageVO message;

	/**
	 * 未读聚合（UNREAD 事件）
	 */
	private UnreadCountVO unread;

	/**
	 * 接收成员ID列表（NEW_MESSAGE 事件）
	 */
	private List<Long> userIds;

}
