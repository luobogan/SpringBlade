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
import java.util.List;

/**
 * 会话创建请求 DTO
 *
 * @author Chill
 */
@Data
public class SessionCreateDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 成员用户ID列表
	 */
	@Schema(description = "成员用户ID列表")
	@JsonSerialize(using = ToStringSerializer.class)
	private List<Long> memberIds;

	/**
	 * 群名称（两人会话可空）
	 */
	@Schema(description = "群名称（两人会话可空）")
	private String name;

}
