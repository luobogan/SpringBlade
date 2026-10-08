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
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.system.entity.UserCompleteStatus;
import org.springblade.system.mapper.UserCompleteStatusMapper;
import org.springblade.system.service.IUserCompleteStatusService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户信息完善度服务实现类（P3-3，对齐 ecology HrmInfoStatus）
 *
 * @author Chill
 */
@Slf4j
@Service
public class UserCompleteStatusServiceImpl
	extends BaseServiceImpl<UserCompleteStatusMapper, UserCompleteStatus> implements IUserCompleteStatusService {

	@Override
	public void initDefault(Long userId) {
		if (Func.isEmpty(userId)) {
			return;
		}
		String tenantId = Func.toStr(SecureUtil.getTenantId(), "000000");
		for (String item : UserCompleteStatus.DEFAULT_ITEMS) {
			UserCompleteStatus exist = this.getOne(Wrappers.<UserCompleteStatus>lambdaQuery()
				.eq(UserCompleteStatus::getUserId, userId)
				.eq(UserCompleteStatus::getItem, item));
			if (exist != null) {
				continue;
			}
			UserCompleteStatus row = new UserCompleteStatus();
			row.setTenantId(tenantId);
			row.setUserId(userId);
			row.setItem(item);
			row.setDone(0);
			this.save(row);
		}
	}

	@Override
	public Map<String, Integer> getByUserId(Long userId) {
		Map<String, Integer> result = new HashMap<>();
		if (Func.isEmpty(userId)) {
			return result;
		}
		List<UserCompleteStatus> list = this.list(Wrappers.<UserCompleteStatus>lambdaQuery()
			.eq(UserCompleteStatus::getUserId, userId));
		if (Func.isNotEmpty(list)) {
			list.forEach(row -> result.put(row.getItem(), Func.toInt(row.getDone(), 0)));
		}
		return result;
	}

	@Override
	public void markDone(Long userId, String item) {
		if (Func.isEmpty(userId) || Func.isBlank(item)) {
			return;
		}
		UserCompleteStatus exist = this.getOne(Wrappers.<UserCompleteStatus>lambdaQuery()
			.eq(UserCompleteStatus::getUserId, userId)
			.eq(UserCompleteStatus::getItem, item));
		if (exist != null) {
			this.update(Wrappers.<UserCompleteStatus>lambdaUpdate()
				.set(UserCompleteStatus::getDone, 1)
				.eq(UserCompleteStatus::getId, exist.getId()));
			return;
		}
		UserCompleteStatus row = new UserCompleteStatus();
		row.setTenantId(Func.toStr(SecureUtil.getTenantId(), "000000"));
		row.setUserId(userId);
		row.setItem(item);
		row.setDone(1);
		this.save(row);
	}

}
