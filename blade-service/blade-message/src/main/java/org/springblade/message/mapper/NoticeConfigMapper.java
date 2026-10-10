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
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springblade.message.entity.NoticeConfig;

/**
 * 流程通知用户级接收配置 Mapper
 *
 * @author Chill
 */
@Mapper
public interface NoticeConfigMapper extends BaseMapper<NoticeConfig> {

	/**
	 * 忽略逻辑删除，按 (user_id, flow_key) 查唯一行。
	 * 用于 upsert 收敛：即便上一轮 reset 把行置为 is_deleted=1（逻辑删除），
	 * 也能找到并重新激活，避免与唯一索引 uk_..._user_flow 冲突导致 INSERT 抛 500。
	 */
	@Select("SELECT * FROM blade_message_notice_config WHERE user_id = #{userId} AND flow_key = #{flowKey} LIMIT 1")
	NoticeConfig selectByUserAndFlow(@Param("userId") Long userId, @Param("flowKey") String flowKey);

	/**
	 * 重新激活已有行（绕过 @TableLogic 的 is_deleted=0 过滤）：置 enabled 并清除逻辑删除标记。
	 */
	@Update("UPDATE blade_message_notice_config SET enabled = #{enabled}, is_deleted = 0, update_user = #{updateUser}, update_time = NOW() WHERE id = #{id}")
	int reactivate(@Param("id") Long id, @Param("enabled") Integer enabled, @Param("updateUser") Long updateUser);

	/**
	 * 物理删除（绕过 @TableLogic）："恢复接收"语义为真正删行（回到默认接收），不留幽灵行。
	 */
	@Delete("DELETE FROM blade_message_notice_config WHERE user_id = #{userId} AND flow_key = #{flowKey}")
	int physicalDelete(@Param("userId") Long userId, @Param("flowKey") String flowKey);

}
