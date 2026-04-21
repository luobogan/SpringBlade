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
import org.springblade.mall.entity.Brand;
import org.springblade.mall.service.IBrandService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 品牌控制器
 *
 * @author Chill
 */
@RestController
@RequestMapping("/mall/brand")
@AllArgsConstructor
@Tag(name = "品牌管理")
public class BrandController {

	private final IBrandService brandService;

	/**
	 * 品牌列表
	 */
	@GetMapping("/list")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "品牌列表", description = "品牌列表")
	public R<IPage<Brand>> list(Query query, Brand brand) {
		IPage<Brand> pages = brandService.page(Condition.getPage(query), brand);
		return R.data(pages);
	}

	/**
	 * 品牌详情
	 */
	@GetMapping("/detail")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "品牌详情", description = "品牌详情")
	public R<Brand> detail(Long id) {
		Brand brand = brandService.getById(id);
		return R.data(brand);
	}

	/**
	 * 新增品牌
	 */
	@PostMapping("/submit")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "新增品牌", description = "新增品牌")
	public R submit(@RequestBody Brand brand) {
		boolean save = brandService.save(brand);
		return save ? R.data(brand) : R.fail("创建失败");
	}

	/**
	 * 更新品牌
	 */
	@PostMapping("/update")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "更新品牌", description = "更新品牌")
	public R update(@RequestBody Brand brand) {
		boolean update = brandService.updateById(brand);
		return R.status(update);
	}

	/**
	 * 删除品牌
	 */
	@PostMapping("/remove")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "删除品牌", description = "删除品牌")
	public R remove(Long id) {
		boolean remove = brandService.removeById(id);
		return R.status(remove);
	}

	/**
	 * 批量删除品牌
	 */
	@PostMapping("/batch-remove")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "批量删除品牌", description = "批量删除品牌")
	public R batchRemove(@RequestBody List<Long> ids) {
		boolean remove = brandService.removeByIds(ids);
		return R.status(remove);
	}

}
