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
package org.springblade.message.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 消息附件提交 DTO（前端直传 blade-resource 后回传的元数据）
 *
 * @author Chill
 */
@Data
public class MessageAttachmentDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

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
