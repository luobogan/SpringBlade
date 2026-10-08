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
package org.springblade.system.feign;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springblade.system.service.IPersonStatusFlowService;
import org.springblade.system.user.feign.IUserStatusFlowClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 人员状态流转回调 Feign 实现类（P3-2）
 * <p>
 * 供 blade-workflow 在流程审批完成时回调；沿用 {@link UserClient} 的既有约定
 * （内部 Feign 端点不加 {@code @PreAuth}，只在服务间调用，不对外暴露入口）。
 *
 * @author Chill
 */
@Slf4j
@Hidden
@RestController
@AllArgsConstructor
public class UserStatusFlowClient implements IUserStatusFlowClient {

	private final IPersonStatusFlowService personStatusFlowService;

	@Override
	@PostMapping(API_PREFIX + "/status-flow/callback")
	public R<Boolean> statusFlowCallback(@RequestParam("instanceId") String instanceId,
		@RequestParam("approved") Boolean approved,
		@RequestParam("opinion") String opinion) {
		log.info("[UserStatusFlowClient] 收到审批回调. instanceId={}, approved={}", instanceId, approved);
		return personStatusFlowService.callback(instanceId, approved, opinion);
	}

}
