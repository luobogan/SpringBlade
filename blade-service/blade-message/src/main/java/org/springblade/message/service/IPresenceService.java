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
package org.springblade.message.service;

import java.util.List;

/**
 * 在线状态服务：基于 Redis 记录租户下用户的在线情况。
 * <p>
 * 说明：用户在多个标签页/设备打开时存在多个 WS 会话，因此用「会话计数」判定，
 * 计数归零才判定离线；同时用时间戳分数做活跃度续期，实例异常退出后可按窗口自动过期。
 *
 * @author Chill
 */
public interface IPresenceService {

	/**
	 * 注册一个在线会话（WS 连接建立时调用）
	 *
	 * @param sessionId STOMP 会话ID，用于多标签页并存与断连幂等
	 */
	void registerSession(String tenantId, Long userId, String sessionId);

	/**
	 * 注销一个在线会话（WS 断开时调用）。按会话ID精确移除，
	 * 重复的断开事件不会误判离线；该用户无剩余会话时才判定离线。
	 */
	void unregisterSession(String tenantId, Long userId, String sessionId);

	/**
	 * 仅凭会话ID注销（WS 断开事件专用）。
	 * <p>
	 * {@code SessionDisconnectEvent} 只携带 sessionId，<b>不携带会话属性</b>，
	 * 因此拿不到 tenantId/userId，无法调用 {@link #unregisterSession}。
	 * 注册时已把 {@code sessionId -> tenantId:userId} 写入索引，这里反查后精确注销，
	 * 使「断开即下线」不再退化为等活跃窗口过期。
	 * <p>
	 * 幂等：索引不存在（未注册 / 已注销）时直接返回。
	 *
	 * @param sessionId STOMP 会话ID
	 */
	void unregisterSessionById(String sessionId);

	/**
	 * 续期在线状态（心跳/订阅等活跃信号，仅刷新时间戳，不改变会话计数）
	 */
	void refresh(String tenantId, Long userId);

	/**
	 * 查询租户下当前在线的用户ID列表
	 */
	List<Long> onlineUserIds(String tenantId);

	/**
	 * 判断指定用户是否在线
	 */
	boolean isOnline(String tenantId, Long userId);

}
