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
 * 消息主体实体类
 *
 * @author Chill
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_message")
public class Message extends TenantEntity {

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
	 * 发送人
	 */
	@Schema(description = "发送人（blade_user.id）")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long senderId;

	/**
	 * 内容类型 1文本 2富文本 3附件 4流程引用
	 */
	@Schema(description = "内容类型 1文本 2富文本 3附件 4流程引用")
	private Integer contentType;

	/**
	 * 消息分类 1=聊天 2=流程通知
	 */
	@Schema(description = "消息分类 1=聊天 2=流程通知")
	private Integer category;

	/**
	 * 内容
	 */
	@Schema(description = "内容")
	private String content;

	/**
	 * 引用消息ID
	 */
	@Schema(description = "引用消息ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long quoteMessageId;

	/**
	 * 业务引用类型（WF_INSTANCE/WF_TASK）
	 */
	@Schema(description = "业务引用类型（WF_INSTANCE/WF_TASK）")
	private String bizRefType;

	/**
	 * 业务引用ID（字符串防精度丢失）
	 */
	@Schema(description = "业务引用ID（字符串防精度丢失）")
	private String bizRefId;

	/**
	 * 状态 1=SENT
	 */
	@Schema(description = "状态 1=SENT")
	private Integer status;

}
