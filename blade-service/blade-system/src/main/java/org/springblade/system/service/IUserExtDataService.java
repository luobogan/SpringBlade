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
import org.springblade.system.entity.UserExtData;

import java.util.Map;

/**
 * 用户自定义字段值服务类（对齐 ecology cus_fielddata）
 *
 * @author Chill
 */
public interface IUserExtDataService extends BaseService<UserExtData> {

	/**
	 * 读取用户自定义字段值
	 *
	 * @param userId 用户主键
	 * @return fieldId -> fieldValue
	 */
	Map<Long, String> getExtData(Long userId);

	/**
	 * 保存用户自定义字段值（存在则更新，不存在则插入；不物理删除，避免唯一键与逻辑删除冲突）
	 *
	 * @param userId 用户主键
	 * @param values fieldId -> fieldValue
	 */
	void saveExtData(Long userId, Map<Long, String> values);

}
