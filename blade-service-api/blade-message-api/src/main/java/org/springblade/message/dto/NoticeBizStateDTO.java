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
package org.springblade.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程通知业务状态回写请求 DTO（对齐 ecology {@code Util_Message.updateBizState}）
 *
 * <p>审批完成后由 blade-workflow 经内部 Feign 调用，把该业务引用关联的历史通知
 * 批量标记业务状态，让通知列表不再是永久的「待办」。幂等：重复调用结果一致。</p>
 *
 * @author Chill
 */
@Data
public class NoticeBizStateDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 租户ID（内部调用无登录态，必须显式传入）
	 */
	@Schema(description = "租户ID（内部调用无登录态，必传）")
	private String tenantId;

	/**
	 * 业务引用类型（WF_INSTANCE/WF_TASK）
	 */
	@Schema(description = "业务引用类型（WF_INSTANCE/WF_TASK）")
	private String bizRefType;

	/**
	 * 业务引用ID（WF_TASK=引擎任务ID；WF_INSTANCE=实例ID，字符串防精度丢失）
	 */
	@Schema(description = "业务引用ID（字符串防精度丢失）")
	private String bizRefId;

	/**
	 * 目标业务状态：1=已处理 2=已办结
	 */
	@Schema(description = "目标业务状态 1=已处理 2=已办结")
	private Integer bizState;

}
