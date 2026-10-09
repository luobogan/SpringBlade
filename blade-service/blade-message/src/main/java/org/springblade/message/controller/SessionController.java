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
package org.springblade.message.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.swagger.annotation.ApiOrder;
import org.springblade.core.tool.api.R;
import org.springblade.message.dto.SessionCreateDTO;
import org.springblade.message.service.ISessionService;
import org.springblade.message.vo.SessionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息会话控制器
 *
 * @author Chill
 */
@RestController
@RequestMapping("message/session")
@AllArgsConstructor
@ApiOrder
@Tag(name = "消息会话", description = "消息会话接口")
public class SessionController extends BladeController {

	private final ISessionService sessionService;

	/**
	 * 我的会话分页
	 */
	@GetMapping("/page")
	@Operation(summary = "我的会话分页",
		description = "返回当前用户参与的会话，含未读数量与成员信息。"
			+ "hasMessage=true 只返回「已有消息」的会话（首屏优先加载）；"
			+ "false 只返回「还没有消息」的会话（滚动到可视区域时再按需加载）；不传则不过滤。")
	public R<IPage<SessionVO>> page(Query query, BladeUser user,
									@RequestParam(value = "hasMessage", required = false) Boolean hasMessage) {
		return R.data(sessionService.pageSessions(query, user, hasMessage));
	}

	/**
	 * 创建/复用会话
	 */
	@PostMapping("/create")
	@Operation(summary = "创建/复用会话", description = "两人会话按成员幂等复用")
	public R<SessionVO> create(@RequestBody SessionCreateDTO dto, BladeUser user) {
		return R.data(sessionService.createSession(dto, user));
	}

}
