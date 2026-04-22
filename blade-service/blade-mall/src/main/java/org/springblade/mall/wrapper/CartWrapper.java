package org.springblade.mall.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.mall.entity.Cart;
import org.springblade.mall.vo.CartVO;

/**
 * 购物车包装类,返回视图层所需的字段
 *
 * @author Chill
 */
public class CartWrapper extends BaseEntityWrapper<Cart, CartVO> {

	public static CartWrapper build() {
		return new CartWrapper();
	}

	@Override
	public CartVO entityVO(Cart cart) {
		CartVO cartVO = BeanUtil.copyProperties(cart, CartVO.class);
		return cartVO;
	}

}
