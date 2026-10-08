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
package org.springblade.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户新增表单 schema（对齐 ecology getHrmResourceAddForm：分组 + 字段元数据）
 *
 * @author Chill
 */
@Data
@Schema(description = "用户表单 schema 分组")
public class UserFormSchemaVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "分组id")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long groupId;

	@Schema(description = "分组编码")
	private String groupCode;

	@Schema(description = "分组名称")
	private String groupName;

	@Schema(description = "分组类型:1基本信息 2个人信息 3工作信息")
	private Integer groupType;

	@Schema(description = "排序")
	private Integer sort;

	@Schema(description = "字段列表")
	private List<FieldItem> fields;

	/**
	 * 字段元数据（对齐 ecology HrmFieldManager：isUse / isMand / eleclazzname）
	 */
	@Data
	@Schema(description = "用户表单 schema 字段")
	public static class FieldItem implements Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

		@Schema(description = "字段id（提交 extData 时作为 key）")
		@JsonSerialize(using = ToStringSerializer.class)
		private Long fieldId;

		@Schema(description = "字段属性名")
		private String propName;

		@Schema(description = "字段显示名")
		private String label;

		@Schema(description = "控件类型:input/textarea/number/date/select")
		private String eleType;

		@Schema(description = "是否必填:0否 1是")
		private Integer required;

		@Schema(description = "排序")
		private Integer sort;

		@Schema(description = "控件扩展配置JSON（如 select 的 options）")
		private String extJson;

	}

}
