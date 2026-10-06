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
 * 消息附件实体类（文件实体存于 blade-resource，本表仅引用）
 *
 * @author Chill
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_message_attachment")
public class MessageAttachment extends TenantEntity {

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
	 * 消息ID
	 */
	@Schema(description = "消息ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long messageId;

	/**
	 * blade-resource 文件ID
	 */
	@Schema(description = "blade-resource 文件ID")
	private String fileId;

	/**
	 * 文件名
	 */
	@Schema(description = "文件名")
	private String fileName;

	/**
	 * 文件访问地址
	 */
	@Schema(description = "文件访问地址")
	private String fileUrl;

	/**
	 * 文件大小（字节）
	 */
	@Schema(description = "文件大小（字节）")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long fileSize;

	/**
	 * 文件类型
	 */
	@Schema(description = "文件类型")
	private String fileType;

}
