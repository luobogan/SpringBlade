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
 * 用户信息完善度（对齐 ecology HrmInfoStatus）
 *
 * @author Chill
 */
@Data
@TableName("blade_user_complete_status")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户信息完善度")
public class UserCompleteStatus extends TenantEntity {

	/** 完善项：基本信息 */
	public static final String ITEM_BASE = "BASE";
	/** 完善项：个人信息 */
	public static final String ITEM_PERSON = "PERSON";
	/** 完善项：工作信息 */
	public static final String ITEM_WORK = "WORK";
	/** 完善项：系统信息 */
	public static final String ITEM_SYSTEM = "SYSTEM";

	/** 建用户时初始化的四类完善项 */
	public static final String[] DEFAULT_ITEMS = {ITEM_BASE, ITEM_PERSON, ITEM_WORK, ITEM_SYSTEM};

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "主键")
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	@Schema(description = "用户id")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long userId;

	@Schema(description = "完善项:BASE基本信息 PERSON个人信息 WORK工作信息 SYSTEM系统信息")
	private String item;

	@Schema(description = "是否完善:0否 1是")
	private Integer done;

}
