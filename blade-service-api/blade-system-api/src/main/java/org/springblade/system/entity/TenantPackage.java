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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 租户产品包实体类
 *
 * @author Chill
 */
@Data
@TableName("blade_tenant_package")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "租户产品包")
public class TenantPackage extends TenantEntity {

	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	/**
	 * 产品包名称
	 */
	@Schema(description = "产品包名称")
	private String packageName;

	/**
	 * 产品包编码
	 */
	@Schema(description = "产品包编码")
	private String packageCode;

	/**
	 * 产品包描述
	 */
	@Schema(description = "产品包描述")
	private String description;

	/**
	 * 状态
	 */
	@Schema(description = "状态")
	private Integer status;
}
