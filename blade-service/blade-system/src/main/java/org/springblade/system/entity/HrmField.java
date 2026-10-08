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
package org.springblade.system.entity;

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
 * 人员自定义字段配置（对齐 ecology HrmCustomFieldByInfoType）
 * <p>仅承载"自由字段"；内置字段（account/password/realName/deptId…）不进本表，等价于系统级不可停用。</p>
 *
 * @author Chill
 */
@Data
@TableName("blade_hrm_field")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "人员自定义字段配置")
public class HrmField extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "主键")
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	@Schema(description = "所属分组id")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long groupId;

	@Schema(description = "字段属性名(表单字段名)")
	private String propName;

	@Schema(description = "字段显示名")
	private String label;

	@Schema(description = "控件类型:input/textarea/number/date/select")
	private String eleType;

	@Schema(description = "是否必填:0否 1是")
	private Integer required;

	@Schema(description = "排序")
	private Integer sort;

	@Schema(description = "控件扩展配置JSON")
	private String extJson;

	@Schema(description = "备注")
	private String remark;

}
