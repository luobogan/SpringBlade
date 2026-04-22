package org.springblade.mall.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.mall.entity.Category;
import org.springblade.mall.vo.CategoryVO;

/**
 * 分类包装类,返回视图层所需的字段
 *
 * @author Chill
 */
public class CategoryWrapper extends BaseEntityWrapper<Category, CategoryVO> {

	public static CategoryWrapper build() {
		return new CategoryWrapper();
	}

	@Override
	public CategoryVO entityVO(Category category) {
		CategoryVO categoryVO = BeanUtil.copyProperties(category, CategoryVO.class);
		return categoryVO;
	}

}
