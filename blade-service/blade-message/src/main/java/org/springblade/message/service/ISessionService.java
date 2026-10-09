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
package org.springblade.message.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.message.dto.SessionCreateDTO;
import org.springblade.message.entity.Session;
import org.springblade.message.vo.SessionVO;

/**
 * 消息会话服务类
 *
 * @author Chill
 */
public interface ISessionService extends BaseService<Session> {

	/**
	 * 我的会话分页（含当前用户未读、成员信息）
	 *
	 * <p><b>按需加载</b>：全公司会话里绝大多数是「还没有消息」的空会话，一并加载会显著拖慢首屏。
	 * 调用方按 {@code hasMessage} 分段取数——首屏只取「有消息」的，空会话等滚动到可视区域再取。</p>
	 *
	 * @param query      分页
	 * @param user       当前用户
	 * @param hasMessage 过滤条件：true=只要已有消息的；false=只要还没有消息的；null=不过滤
	 * @return 会话分页
	 */
	IPage<SessionVO> pageSessions(Query query, BladeUser user, Boolean hasMessage);

	/**
	 * 创建（或复用）会话；两人会话幂等
	 *
	 * @param dto  创建参数
	 * @param user 当前用户
	 * @return 会话视图
	 */
	SessionVO createSession(SessionCreateDTO dto, BladeUser user);

}
