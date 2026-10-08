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
import com.baomidou.mybatisplus.annotation.TableField;
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
 * 组织编码规则（对齐 ecology CodeRuleManager / hrm_coderulereserved 简化版）
 * <p>编码格式：prefix + date_fmt 日期段 + 左补零序列。</p>
 *
 * @author Chill
 */
@Data
@TableName("blade_code_rule")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "组织编码规则")
public class WorkCodeRule extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "主键")
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	@Schema(description = "规则编码:USER=工号 / DEPT=部门编号")
	private String ruleCode;

	@Schema(description = "前缀")
	private String prefix;

	@Schema(description = "日期段格式(如 yyyyMMdd),NULL 表示无日期段")
	@TableField("date_fmt")
	private String dateFormat;

	@Schema(description = "序列位数(左补零)")
	private Integer seqLen;

	@Schema(description = "当前序列值")
	private Long currentSeq;

	@Schema(description = "备注")
	private String remark;

}
