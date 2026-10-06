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
package org.springblade.message.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.message.entity.Message;
import org.springblade.message.vo.MessageVO;

/**
 * 消息包装类
 *
 * <p>发送人姓名/附件/已读标记由 Service 在查询时附加，本类仅做基础属性拷贝。</p>
 *
 * @author Chill
 */
public class MessageWrapper extends BaseEntityWrapper<Message, MessageVO> {

	public static MessageWrapper build() {
		return new MessageWrapper();
	}

	@Override
	public MessageVO entityVO(Message message) {
		return BeanUtil.copyProperties(message, MessageVO.class);
	}

}
