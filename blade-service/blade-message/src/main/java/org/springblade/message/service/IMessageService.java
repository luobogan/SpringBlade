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

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.message.dto.MessageSendDTO;
import org.springblade.message.entity.Message;
import org.springblade.message.vo.MessageVO;
import org.springblade.message.vo.UnreadCountVO;

/**
 * 消息主体服务类
 *
 * @author Chill
 */
public interface IMessageService extends BaseService<Message> {

	/**
	 * 会话消息分页（按时间正序，含发送人/附件/已读）
	 *
	 * @param sessionId 会话ID
	 * @param query     分页
	 * @param user      当前用户
	 * @return 消息分页
	 */
	IPage<MessageVO> pageMessages(Long sessionId, Query query, BladeUser user);

	/**
	 * 发送消息（落库 + 增量未读 + 实时推送）
	 *
	 * @param dto  发送参数
	 * @param user 当前用户
	 * @return 是否成功
	 */
	Boolean send(MessageSendDTO dto, BladeUser user);

	/**
	 * 标记会话已读（补回执 + 未读归零）
	 *
	 * @param sessionId 会话ID
	 * @param user      当前用户
	 * @return 是否成功
	 */
	Boolean markRead(Long sessionId, BladeUser user);

	/**
	 * 当前用户未读红点聚合
	 *
	 * @param user 当前用户
	 * @return 未读聚合
	 */
	UnreadCountVO unreadCount(BladeUser user);

	/**
	 * 指定用户未读红点聚合（实时推送订阅者内部使用，无需登录态）
	 *
	 * @param userId 用户ID
	 * @return 未读聚合
	 */
	UnreadCountVO unreadCount(Long userId);

}
