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
package org.springblade.mall.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.node.ForestNodeMerger;
import org.springblade.core.tool.utils.Func;
import org.springblade.mall.entity.Category;
import org.springblade.mall.service.ICategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器
 *
 * @author Chill
 */
@RestController
@RequestMapping("/mall/category")
@AllArgsConstructor
@Tag(name = "分类管理")
public class CategoryController {

	private final ICategoryService categoryService;

	/**
	 * 分类列表
	 */
	@GetMapping("/list")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "分类列表", description = "分类列表")
	public R<IPage<Category>> list(Query query, Category category) {
		IPage<Category> pages = categoryService.page(Condition.getPage(query), category);
		return R.data(pages);
	}

	/**
	 * 分类树
	 */
	@GetMapping("/tree")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "分类树", description = "分类树")
	public R<List<Category>> tree() {
		List<Category> tree = categoryService.tree();
		return R.data(tree);
	}

	/**
	 * 分类详情
	 */
	@GetMapping("/detail")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "分类详情", description = "分类详情")
	public R<Category> detail(Long id) {
		Category category = categoryService.getById(id);
		return R.data(category);
	}

	/**
	 * 新增分类
	 */
	@PostMapping("/submit")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "新增分类", description = "新增分类")
	public R submit(@RequestBody Category category) {
		boolean save = categoryService.save(category);
		return save ? R.data(category) : R.fail("创建失败");
	}

	/**
	 * 更新分类
	 */
	@PostMapping("/update")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "更新分类", description = "更新分类")
	public R update(@RequestBody Category category) {
		boolean update = categoryService.updateById(category);
		return R.status(update);
	}

	/**
	 * 删除分类
	 */
	@PostMapping("/remove")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "删除分类", description = "删除分类")
	public R remove(Long id) {
		boolean remove = categoryService.removeById(id);
		return R.status(remove);
	}

}
