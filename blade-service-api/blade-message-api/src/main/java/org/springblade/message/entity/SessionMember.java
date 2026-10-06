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
package org.springblade.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;

/**
 * 会话成员实体类
 *
 * @author Chill
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_message_session_member")
public class SessionMember extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@Schema(description = "主键")
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/**
	 * 会话ID
	 */
	@Schema(description = "会话ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long sessionId;

	/**
	 * 参与人（blade_user.id）
	 */
	@Schema(description = "参与人（blade_user.id）")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long userId;

	/**
	 * 未读数量
	 */
	@Schema(description = "未读数量")
	private Integer unreadCount;

	/**
	 * 置顶 0否 1是
	 */
	@Schema(description = "置顶 0否 1是")
	private Integer pinned;

	/**
	 * 免打扰 0否 1是
	 */
	@Schema(description = "免打扰 0否 1是")
	private Integer mute;

}
