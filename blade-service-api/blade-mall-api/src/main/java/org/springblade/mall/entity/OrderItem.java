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
 * 订单明细实体类
 *
 * @author Chill
 */
@Data
@TableName("blade_mall_order_item")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单明细")
public class OrderItem extends MallTenantEntity {

	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	/**
	 * 订单ID
	 */
	@Schema(description = "订单ID")
	private Long orderId;

	/**
	 * 商品ID
	 */
	@Schema(description = "商品ID")
	private Long productId;

	/**
	 * 商品名称
	 */
	@Schema(description = "商品名称")
	private String productName;

	/**
	 * 商品图片
	 */
	@Schema(description = "商品图片")
	private String productImage;

	/**
	 * 商品价格
	 */
	@Schema(description = "商品价格")
	private BigDecimal price;

	/**
	 * 商品数量
	 */
	@Schema(description = "商品数量")
	private Integer quantity;

	/**
	 * 商品规格
	 */
	@Schema(description = "商品规格")
	private String spec;

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
