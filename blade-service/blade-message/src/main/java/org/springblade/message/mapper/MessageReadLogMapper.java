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
import org.apache.ibatis.annotations.Insert;
import org.springblade.message.entity.MessageReadLog;

/**
 * 消息已读回执 Mapper
 *
 * @author Chill
 */
public interface MessageReadLogMapper extends BaseMapper<MessageReadLog> {

	/**
	 * 幂等插入已读日志（MySQL INSERT IGNORE）。
	 * <p>
	 * markRead 是「先查已读集合、再补插缺失记录」，check-then-insert 在并发下不是原子操作：
	 * 前端打开会话 / WS 新消息自动已读 / 展开补已读 / 页签切回等多处几乎同时触发标记已读时，
	 * 两个并发事务都会查到同一批「未读」消息并各自插入，第二个事务撞唯一键
	 * uk_blade_message_read_log_msg_user（message_id + user_id）报 Duplicate entry。
	 * INSERT IGNORE 让冲突行静默跳过（保留首次 read_time），从存储层消除该竞态。
	 */
	@Insert("INSERT IGNORE INTO blade_message_read_log (id, message_id, user_id, read_time, tenant_id, create_user, create_dept, status, is_deleted) "
		+ "VALUES (#{id}, #{messageId}, #{userId}, #{readTime}, #{tenantId}, #{createUser}, #{createDept}, #{status}, #{isDeleted})")
	int insertIgnore(MessageReadLog readLog);

}
