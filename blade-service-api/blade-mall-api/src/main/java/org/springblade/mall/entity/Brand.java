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

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 品牌实体类
 *
 * @author Chill
 */
@Data
@TableName("blade_mall_brand")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "品牌")
public class Brand extends MallTenantEntity {

	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	/**
	 * 品牌名称
	 */
	@Schema(description = "品牌名称")
	private String name;

	/**
	 * 品牌编码
	 */
	@Schema(description = "品牌编码")
	private String code;

	/**
	 * 品牌 logo
	 */
	@Schema(description = "品牌 logo")
	private String logo;

	/**
	 * 品牌描述
	 */
	@Schema(description = "品牌描述")
	private String description;

	/**
	 * 品牌状态
	 */
	@Schema(description = "品牌状态")
	private Integer status;

	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createTime;

	/**
	 * 更新时间
	 */
	@Schema(description = "更新时间")
	private Date updateTime;

}
