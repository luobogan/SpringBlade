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
package org.springblade.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.system.entity.UserExtData;
import org.springblade.system.mapper.UserExtDataMapper;
import org.springblade.system.service.IUserExtDataService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户自定义字段值服务实现类
 *
 * @author Chill
 */
@Service
public class UserExtDataServiceImpl extends BaseServiceImpl<UserExtDataMapper, UserExtData> implements IUserExtDataService {

	@Override
	public Map<Long, String> getExtData(Long userId) {
		Map<Long, String> result = new HashMap<>();
		if (Func.isEmpty(userId)) {
			return result;
		}
		List<UserExtData> list = this.list(Wrappers.<UserExtData>lambdaQuery()
			.eq(UserExtData::getUserId, userId));
		if (Func.isNotEmpty(list)) {
			list.forEach(row -> result.put(row.getFieldId(), row.getFieldValue()));
		}
		return result;
	}

	@Override
	public void saveExtData(Long userId, Map<Long, String> values) {
		if (Func.isEmpty(userId) || Func.isEmpty(values)) {
			return;
		}
		String tenantId = Func.toStr(SecureUtil.getTenantId(), "000000");
		values.forEach((fieldId, value) -> {
			if (fieldId == null) {
				return;
			}
			UserExtData exist = this.getOne(Wrappers.<UserExtData>lambdaQuery()
				.eq(UserExtData::getUserId, userId)
				.eq(UserExtData::getFieldId, fieldId));
			if (exist != null) {
				// 存在则更新（不物理删除，规避 UK(tenant_id,user_id,field_id) 与逻辑删除冲突）
				this.update(Wrappers.<UserExtData>lambdaUpdate()
					.set(UserExtData::getFieldValue, value)
					.eq(UserExtData::getId, exist.getId()));
			} else {
				UserExtData row = new UserExtData();
				row.setTenantId(tenantId);
				row.setUserId(userId);
				row.setFieldId(fieldId);
				row.setFieldValue(value);
				this.save(row);
			}
		});
	}

}
