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
import java.util.List;

/**
 * 流程通知发送请求 DTO（系统代发，无登录态）
 *
 * <p>供 blade-workflow 等服务经内部 Feign（{@code /feign/client/notice/send-to-users}）调用：
 * 服务端按 userId 自动建/复用「系统通知会话」（type=3，每人一条），消息
 * {@code category=2}（流程通知），发送者固定为系统（senderId=0）。</p>
 *
 * @author Chill
 */
@Data
public class NoticeSendDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 租户ID（内部调用无登录态，必须显式传入）
	 */
	@Schema(description = "租户ID（内部调用无登录态，必传）")
	private String tenantId;

	/**
	 * 接收人列表（blade_user.id）
	 */
	@Schema(description = "接收人列表（blade_user.id）")
	private List<Long> userIds;

	/**
	 * 通知文案（调用方拼好模板标题，如「您有一条流程待办需要处理：xxx」）
	 */
	@Schema(description = "通知文案（调用方拼好模板标题）")
	private String content;

	/**
	 * 内容类型，默认 4=流程引用
	 */
	@Schema(description = "内容类型，默认 4=流程引用")
	private Integer contentType = 4;

	/**
	 * 业务引用类型（WF_INSTANCE/WF_TASK）
	 */
	@Schema(description = "业务引用类型（WF_INSTANCE/WF_TASK）")
	private String bizRefType;

	/**
	 * 业务引用ID（字符串防精度丢失）
	 */
	@Schema(description = "业务引用ID（字符串防精度丢失）")
	private String bizRefId;

	/**
	 * 通知业务状态（可选）：NULL=待处理 1=已处理 2=已办结（办结通知自带 2）。
	 * 常量取值见 {@link org.springblade.message.constant.MessageConstant}。
	 */
	@Schema(description = "通知业务状态 NULL=待处理 1=已处理 2=已办结")
	private Integer bizState;

	/**
	 * 流程定义 key（proc_key，三期 T11.1）：发送侧按用户级提醒配置过滤的依据，
	 * 缺省视为不过滤（全部送达）
	 */
	@Schema(description = "流程定义key（proc_key，用户级提醒过滤依据）")
	private String flowKey;

}
