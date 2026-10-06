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
import org.springblade.message.entity.Session;
import org.springblade.message.vo.SessionVO;

/**
 * 会话包装类
 *
 * <p>未读数量/成员信息由 Service 在分页时附加，本类仅做基础属性拷贝。</p>
 *
 * @author Chill
 */
public class SessionWrapper extends BaseEntityWrapper<Session, SessionVO> {

	public static SessionWrapper build() {
		return new SessionWrapper();
	}

	@Override
	public SessionVO entityVO(Session session) {
		return BeanUtil.copyProperties(session, SessionVO.class);
	}

}
