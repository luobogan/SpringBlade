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

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品实体类
 *
 * @author Chill
 */
@Data
@TableName("blade_mall_product")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品")
public class Product extends MallTenantEntity {

	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	/**
	 * 商品名称
	 */
	@Schema(description = "商品名称")
	private String name;

	/**
	 * 商品编码
	 */
	@Schema(description = "商品编码")
	private String code;

	/**
	 * 分类ID
	 */
	@Schema(description = "分类ID")
	private Long categoryId;

	/**
	 * 品牌ID
	 */
	@Schema(description = "品牌ID")
	private Long brandId;

	/**
	 * 商品价格
	 */
	@Schema(description = "商品价格")
	private BigDecimal price;

	/**
	 * 商品库存
	 */
	@Schema(description = "商品库存")
	private Integer stock;

	/**
	 * 商品销量
	 */
	@Schema(description = "商品销量")
	private Integer sales;

	/**
	 * 商品图片
	 */
	@Schema(description = "商品图片")
	private String image;

	/**
	 * 商品描述
	 */
	@Schema(description = "商品描述")
	private String description;

	/**
	 * 商品状态
	 */
	@Schema(description = "商品状态")
	private Integer status;

	/**
	 * 是否上架
	 */
	@Schema(description = "是否上架")
	private Integer isPublish;

	/**
	 * 是否推荐
	 */
	@Schema(description = "是否推荐")
	private Integer isRecommend;

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
