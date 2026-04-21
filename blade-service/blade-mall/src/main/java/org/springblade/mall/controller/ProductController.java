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
import org.springblade.core.tool.utils.Func;
import org.springblade.mall.entity.Product;
import org.springblade.mall.service.IProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 *
 * @author Chill
 */
@RestController
@RequestMapping("/mall/product")
@AllArgsConstructor
@Tag(name = "商品管理")
public class ProductController {

	private final IProductService productService;

	/**
	 * 商品列表
	 */
	@GetMapping("/list")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "商品列表", description = "商品列表")
	public R<IPage<Product>> list(Query query, Product product) {
		IPage<Product> pages = productService.page(Condition.getPage(query), product);
		return R.data(pages);
	}

	/**
	 * 商品详情
	 */
	@GetMapping("/detail")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "商品详情", description = "商品详情")
	public R<Product> detail(Long id) {
		Product product = productService.getById(id);
		return R.data(product);
	}

	/**
	 * 新增商品
	 */
	@PostMapping("/submit")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "新增商品", description = "新增商品")
	public R submit(@RequestBody Product product) {
		boolean save = productService.save(product);
		return save ? R.data(product) : R.fail("创建失败");
	}

	/**
	 * 更新商品
	 */
	@PostMapping("/update")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "更新商品", description = "更新商品")
	public R update(@RequestBody Product product) {
		boolean update = productService.updateById(product);
		return R.status(update);
	}

	/**
	 * 删除商品
	 */
	@PostMapping("/remove")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "删除商品", description = "删除商品")
	public R remove(Long id) {
		boolean remove = productService.removeById(id);
		return R.status(remove);
	}

	/**
	 * 批量删除商品
	 */
	@PostMapping("/batch-remove")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "批量删除商品", description = "批量删除商品")
	public R batchRemove(@RequestBody List<Long> ids) {
		boolean remove = productService.removeByIds(ids);
		return R.status(remove);
	}

}
