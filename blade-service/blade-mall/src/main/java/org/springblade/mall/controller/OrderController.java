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
import org.springblade.mall.entity.Order;
import org.springblade.mall.service.IOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 *
 * @author Chill
 */
@RestController
@RequestMapping("/mall/order")
@AllArgsConstructor
@Tag(name = "订单管理")
public class OrderController {

	private final IOrderService orderService;

	/**
	 * 订单列表
	 */
	@GetMapping("/list")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "订单列表", description = "订单列表")
	public R<IPage<Order>> list(Query query, Order order) {
		IPage<Order> pages = orderService.page(Condition.getPage(query), order);
		return R.data(pages);
	}

	/**
	 * 订单详情
	 */
	@GetMapping("/detail")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "订单详情", description = "订单详情")
	public R<Order> detail(Long id) {
		Order order = orderService.getById(id);
		return R.data(order);
	}

	/**
	 * 更新订单状态
	 */
	@PostMapping("/update-status")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "更新订单状态", description = "更新订单状态")
	public R updateStatus(@RequestBody Order order) {
		boolean update = orderService.updateById(order);
		return R.status(update);
	}

	/**
	 * 取消订单
	 */
	@PostMapping("/cancel")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "取消订单", description = "取消订单")
	public R cancel(Long id) {
		Order order = orderService.getById(id);
		if (order == null) {
			return R.fail("订单不存在");
		}
		order.setStatus(6);
		boolean update = orderService.updateById(order);
		return R.status(update);
	}

	/**
	 * 删除订单
	 */
	@PostMapping("/remove")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "删除订单", description = "删除订单")
	public R remove(Long id) {
		boolean remove = orderService.removeById(id);
		return R.status(remove);
	}

	/**
	 * 批量删除订单
	 */
	@PostMapping("/batch-remove")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "批量删除订单", description = "批量删除订单")
	public R batchRemove(@RequestBody List<Long> ids) {
		boolean remove = orderService.removeByIds(ids);
		return R.status(remove);
	}

}
