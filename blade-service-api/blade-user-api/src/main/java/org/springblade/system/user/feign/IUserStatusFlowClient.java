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
package org.springblade.system.user.feign;

import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.tool.api.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 人员状态流转回调 Feign 客户端（P3-2）
 * <p>
 * 供 blade-workflow 在「人员状态变更」流程审批完成时回调 blade-system，
 * 由 blade-system 按 instanceId 反查流转记录后落库 {@code person_status}。
 * 与 {@link IUserClient} 同样使用资源路径，不经网关。
 *
 * @author Chill
 */
@FeignClient(
	value = AppConstant.APPLICATION_SYSTEM_NAME,
	fallback = IUserStatusFlowClientFallback.class
)
public interface IUserStatusFlowClient {

	/**
	 * 状态流转回调 API 前缀
	 */
	String API_PREFIX = "/user";

	/**
	 * 审批完成回调
	 *
	 * @param instanceId 流程实例id(wf_instance.id)
	 * @param approved   是否通过
	 * @param opinion    审批意见
	 * @return 是否处理成功
	 */
	@PostMapping(API_PREFIX + "/status-flow/callback")
	R<Boolean> statusFlowCallback(@RequestParam("instanceId") String instanceId,
		@RequestParam("approved") Boolean approved,
		@RequestParam("opinion") String opinion);

}
