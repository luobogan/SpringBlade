package org.springblade.mall.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.mall.entity.Order;
import org.springblade.mall.vo.OrderVO;

/**
 * 订单包装类,返回视图层所需的字段
 *
 * @author Chill
 */
public class OrderWrapper extends BaseEntityWrapper<Order, OrderVO> {

	public static OrderWrapper build() {
		return new OrderWrapper();
	}

	@Override
	public OrderVO entityVO(Order order) {
		OrderVO orderVO = BeanUtil.copyProperties(order, OrderVO.class);
		return orderVO;
	}

}
