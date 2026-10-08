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
package org.springblade.system.service;

import org.springblade.core.mp.base.BaseService;
import org.springblade.system.entity.UserCompleteStatus;

import java.util.Map;

/**
 * 用户信息完善度服务类（P3-3，对齐 ecology HrmInfoStatus）
 *
 * @author Chill
 */
public interface IUserCompleteStatusService extends BaseService<UserCompleteStatus> {

	/**
	 * 新用户伴生初始化：补齐四类完善项（基本/个人/工作/系统），已存在则跳过（幂等）
	 *
	 * @param userId 用户主键
	 */
	void initDefault(Long userId);

	/**
	 * 读取用户完善度
	 *
	 * @param userId 用户主键
	 * @return item -> done(0/1)
	 */
	Map<String, Integer> getByUserId(Long userId);

	/**
	 * 标记某项已完善（已存在则更新，不存在则插入）
	 *
	 * @param userId 用户主键
	 * @param item   完善项
	 */
	void markDone(Long userId, String item);

}
