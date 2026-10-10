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
import org.springblade.message.dto.NoticeBizStateDTO;
import org.springblade.message.dto.NoticeSendDTO;
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
	 * 会话消息分页（默认按时间正序，含发送人/附件/已读）
	 *
	 * @param sessionId 会话ID
	 * @param query     分页
	 * @param user      当前用户
	 * @return 消息分页
	 */
	IPage<MessageVO> pageMessages(Long sessionId, Query query, BladeUser user);

	/**
	 * 会话消息分页（可指定排序方向）
	 *
	 * @param sessionId 会话ID
	 * @param query     分页
	 * @param user      当前用户
	 * @param desc      true=按 id 倒序（流程通知列表取最新一页）；false=正序（聊天流）
	 * @return 消息分页
	 */
	IPage<MessageVO> pageMessages(Long sessionId, Query query, BladeUser user, boolean desc);

	/**
	 * 单条消息已读（二期 T8）：写 read_log（幂等）+ 该会话未读 -1 + 推送最新未读数
	 *
	 * <p>供流程通知卡片「点击即单条已读」使用（对齐 ecology 点击消息置已读）。</p>
	 *
	 * @param messageId 消息ID
	 * @param user      当前用户
	 * @return 是否成功（消息不存在返回 false）
	 */
	Boolean markOneRead(Long messageId, BladeUser user);

	/**
	 * 回写通知业务状态（二期 T9，对齐 ecology updateBizState）：把同业务引用的
	 * 流程通知（category=2）批量标记目标状态，幂等
	 *
	 * @param dto 回写参数（tenantId/bizRefType/bizRefId/bizState 必传）
	 * @return 是否更新成功（无匹配行视为成功）
	 */
	Boolean markBizState(NoticeBizStateDTO dto);

	/**
	 * 发送消息（落库 + 增量未读 + 实时推送）
	 *
	 * @param dto  发送参数
	 * @param user 当前用户
	 * @return 是否成功
	 */
	Boolean send(MessageSendDTO dto, BladeUser user);

	/**
	 * 批量发送流程通知（系统代发，无登录态；每人一条 type=3 系统通知会话，自动建/复用）
	 *
	 * <p>供 {@code INoticeClient}（/feign/client/notice 前缀，网关 InnerFilter 隔离）调用，
	 * 消息 {@code category=2}，发送者固定为系统（senderId=0），复用既有落库/未读/WS 推送链路。</p>
	 *
	 * @param dto 通知参数（tenantId/userIds 必传）
	 * @return 是否全部发送成功
	 */
	Boolean sendNoticeToUsers(NoticeSendDTO dto);

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
