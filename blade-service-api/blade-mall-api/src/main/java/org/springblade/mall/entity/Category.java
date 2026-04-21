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
 * 商品分类实体类
 *
 * @author Chill
 */
@Data
@TableName("blade_mall_category")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品分类")
public class Category extends MallTenantEntity {

	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	/**
	 * 分类名称
	 */
	@Schema(description = "分类名称")
	private String name;

	/**
	 * 分类编码
	 */
	@Schema(description = "分类编码")
	private String code;

	/**
	 * 父级分类ID
	 */
	@Schema(description = "父级分类ID")
	private Long parentId;

	/**
	 * 分类层级
	 */
	@Schema(description = "分类层级")
	private Integer level;

	/**
	 * 分类排序
	 */
	@Schema(description = "分类排序")
	private Integer sort;

	/**
	 * 分类图标
	 */
	@Schema(description = "分类图标")
	private String icon;

	/**
	 * 分类状态
	 */
	@Schema(description = "分类状态")
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
