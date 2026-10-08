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
package org.springblade.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 人员状态流转回调参数（P3-2：由 blade-workflow 审批完成时调用）
 *
 * @author Chill
 */
@Data
@Schema(description = "人员状态流转回调参数")
public class StatusFlowCallbackDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "流程实例id(wf_instance.id)", requiredMode = Schema.RequiredMode.REQUIRED)
	private String instanceId;

	@Schema(description = "是否审批通过：true 通过（落库目标状态）/ false 驳回")
	private Boolean approved;

	@Schema(description = "审批意见")
	private String opinion;

}
