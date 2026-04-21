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
package org.springblade.mall.service.impl;

import org.springblade.core.mp.support.Condition;
import org.springblade.mall.entity.Category;
import org.springblade.mall.mapper.CategoryMapper;
import org.springblade.mall.service.ICategoryService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类服务实现类
 *
 * @author Chill
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {

	@Override
	public IPage<Category> page(IPage<Category> page, Category category) {
		return baseMapper.selectPage(page, Condition.getQueryWrapper(category));
	}

	@Override
	public List<Category> tree() {
		return list();
	}

}
