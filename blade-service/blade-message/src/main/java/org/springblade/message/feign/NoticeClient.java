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
package org.springblade.message.feign;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springblade.message.dto.NoticeSendDTO;
import org.springblade.message.service.IMessageService;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程通知内部端点（{@code /feign/client/notice/**}）
 *
 * <p>仅供服务间直连（Nacos 服务发现）调用：网关 InnerFilter 会拒绝一切经网关的
 * {@code /feign/client} 连续段请求，外部无法触达。无 {@code @PreAuth}，
 * 鉴权边界由网关隔离保证（对齐 blade-workflow 的 {@code WfInstanceClient}）。</p>
 *
 * <p>不 {@code implements} 到既有 Controller：避免与
 * {@code MessageController#send}（POST /message/message/send）形成跨类重复映射
 * （历史教训见 MessageController 类注释）。</p>
 *
 * @author Chill
 */
@Slf4j
@RestController
@AllArgsConstructor
public class NoticeClient implements INoticeClient {

	private final IMessageService messageService;

	@Override
	public R<Boolean> sendToUsers(NoticeSendDTO dto) {
		return R.data(messageService.sendNoticeToUsers(dto));
	}

}
