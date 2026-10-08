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
package org.springblade.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.api.R;
import org.springblade.system.dto.StatusFlowStartDTO;
import org.springblade.system.entity.PersonStatusFlow;
import org.springblade.system.entity.PersonStatusFlowRecord;
import org.springblade.system.service.IPersonStatusFlowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 人员状态流转控制器（P3-2，对齐 ecology HrmResourceTryAction/FireAction）
 *
 * @author Chill
 */
@RestController
@AllArgsConstructor
@RequestMapping("/user/status-flow")
@Tag(name = "人员状态流转", description = "状态变更发起与流转记录")
public class UserStatusFlowController extends BladeController {

	private final IPersonStatusFlowService personStatusFlowService;

	/**
	 * 发起状态变更
	 */
	@PostMapping("/start")
	@Operation(summary = "发起人员状态变更", description = "命中配置则发起审批流程，未配置流程则直接变更")
	public R<String> start(@RequestBody StatusFlowStartDTO dto) {
		return personStatusFlowService.start(dto);
	}

	/**
	 * 某用户可用的流转配置
	 */
	@GetMapping("/available")
	@Operation(summary = "可用状态流转", description = "传入userId")
	public R<List<PersonStatusFlow>> available(@Parameter(description = "用户主键", required = true) @RequestParam Long userId) {
		return R.data(personStatusFlowService.availableFlows(userId));
	}

	/**
	 * 某用户的流转记录（流转进度）
	 */
	@GetMapping("/records")
	@Operation(summary = "人员状态流转记录", description = "传入userId")
	public R<List<PersonStatusFlowRecord>> records(@Parameter(description = "用户主键", required = true) @RequestParam Long userId) {
		return R.data(personStatusFlowService.records(userId));
	}

}
