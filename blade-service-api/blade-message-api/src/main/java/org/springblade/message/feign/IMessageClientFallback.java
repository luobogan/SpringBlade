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

import org.springblade.core.tool.api.R;
import org.springblade.message.dto.MessageSendDTO;
import org.springblade.message.dto.SessionCreateDTO;
import org.springblade.message.vo.SessionVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 消息中心 Feign 降级类
 *
 * @author Chill
 */
@Component
public class IMessageClientFallback implements IMessageClient {

	@Override
	public R<Boolean> send(MessageSendDTO dto) {
		return R.fail("消息中心服务暂不可用");
	}

	@Override
	public R<SessionVO> createSession(SessionCreateDTO dto) {
		return R.fail("消息中心服务暂不可用");
	}

	@Override
	public R<Long> unreadCount(Long userId) {
		return R.fail("消息中心服务暂不可用，无法查询未读数");
	}

	@Override
	public R<List<Map<String, Object>>> recent(Long userId, Integer limit) {
		// 降级返回空列表：门户元素不应因消息服务不可用而整页失败
		return R.data(new ArrayList<>());
	}

}
