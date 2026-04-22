package org.springblade.mall.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.mall.entity.Product;
import org.springblade.mall.vo.ProductVO;

/**
 * 商品包装类,返回视图层所需的字段
 *
 * @author Chill
 */
public class ProductWrapper extends BaseEntityWrapper<Product, ProductVO> {

	public static ProductWrapper build() {
		return new ProductWrapper();
	}

	@Override
	public ProductVO entityVO(Product product) {
		ProductVO productVO = BeanUtil.copyProperties(product, ProductVO.class);
		return productVO;
	}

}
