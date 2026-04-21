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
 * 订单实体类
 *
 * @author Chill
 */
@Data
@TableName("blade_mall_order")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单")
public class Order extends MallTenantEntity {

	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	/**
	 * 订单号
	 */
	@Schema(description = "订单号")
	private String orderNo;

	/**
	 * 用户ID
	 */
	@Schema(description = "用户ID")
	private Long userId;

	/**
	 * 用户名
	 */
	@Schema(description = "用户名")
	private String userName;

	/**
	 * 收货地址ID
	 */
	@Schema(description = "收货地址ID")
	private Long addressId;

	/**
	 * 订单总金额
	 */
	@Schema(description = "订单总金额")
	private BigDecimal totalAmount;

	/**
	 * 实际支付金额
	 */
	@Schema(description = "实际支付金额")
	private BigDecimal payAmount;

	/**
	 * 支付方式
	 */
	@Schema(description = "支付方式")
	private Integer payType;

	/**
	 * 订单状态
	 */
	@Schema(description = "订单状态")
	private Integer status;

	/**
	 * 订单备注
	 */
	@Schema(description = "订单备注")
	private String remark;

	/**
	 * 支付时间
	 */
	@Schema(description = "支付时间")
	private Date payTime;

	/**
	 * 发货时间
	 */
	@Schema(description = "发货时间")
	private Date shipTime;

	/**
	 * 完成时间
	 */
	@Schema(description = "完成时间")
	private Date finishTime;

	/**
	 * 取消时间
	 */
	@Schema(description = "取消时间")
	private Date cancelTime;

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
