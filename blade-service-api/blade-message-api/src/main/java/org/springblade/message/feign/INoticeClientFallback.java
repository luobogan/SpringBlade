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
import org.springblade.message.dto.NoticeBizStateDTO;
import org.springblade.message.dto.NoticeSendDTO;
import org.springframework.stereotype.Component;

/**
 * 流程通知 Feign 降级类
 *
 * <p>调用方（blade-workflow 监听器）对失败只记日志不影响引擎事务，
 * 降级返回 fail 即可。</p>
 *
 * @author Chill
 */
@Component
public class INoticeClientFallback implements INoticeClient {

	@Override
	public R<Boolean> sendToUsers(NoticeSendDTO dto) {
		return R.fail("消息中心服务暂不可用");
	}

	@Override
	public R<Boolean> markBizState(NoticeBizStateDTO dto) {
		return R.fail("消息中心服务暂不可用");
	}

}
