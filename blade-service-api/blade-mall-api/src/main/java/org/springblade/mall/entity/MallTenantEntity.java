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
package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.time.LocalDateTime;

/**
 * 商城租户实体类
 *
 * @author Chill
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MallTenantEntity extends TenantEntity {

	private static final long serialVersionUID = 1L;

	/**
	 * 创建时间
	 */
	@TableField("created_at")
	@Schema(description = "创建时间")
	private LocalDateTime createdAt;

	/**
	 * 更新时间
	 */
	@TableField("updated_at")
	@Schema(description = "更新时间")
	private LocalDateTime updatedAt;

	/**
	 * 软删除标记
	 */
	@TableField("is_deleted")
	@TableLogic
	@Schema(description = "软删除标记")
	private Integer isDeleted;

}
