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
package org.springblade.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springblade.message.entity.Message;

import java.util.List;
import java.util.Map;

/**
 * 消息主体 Mapper
 *
 * @author Chill
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

	/**
	 * 查询某个用户的「最近消息」（跨会话，取最新 N 条）
	 *
	 * <p>供门户「消息提醒」元素使用（经内部 Feign 端点暴露，userId 由调用方显式传入）。
	 * 取我参与的所有会话里最新的消息，按 id 倒序。</p>
	 *
	 * @param userId 用户ID
	 * @param limit  条数
	 * @return 消息摘要列表（id/sessionId/senderId/category/content/createTime）
	 */
	@Select("SELECT m.id, m.session_id AS sessionId, m.sender_id AS senderId, m.category, "
		+ " m.content_type AS contentType, m.content, m.create_time AS createTime "
		+ " FROM blade_message m "
		+ " JOIN blade_message_session_member sm ON sm.session_id = m.session_id AND sm.user_id = #{userId} "
		+ " WHERE m.is_deleted = 0 AND sm.is_deleted = 0 "
		+ " ORDER BY m.id DESC LIMIT #{limit}")
	List<Map<String, Object>> selectRecentByUser(@Param("userId") Long userId, @Param("limit") int limit);

}
