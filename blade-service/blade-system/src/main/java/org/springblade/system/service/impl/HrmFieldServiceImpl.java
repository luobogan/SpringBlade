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
import org.springblade.system.entity.HrmField;
import org.springblade.system.entity.HrmFieldGroup;
import org.springblade.system.mapper.HrmFieldGroupMapper;
import org.springblade.system.mapper.HrmFieldMapper;
import org.springblade.system.service.IHrmFieldService;
import org.springblade.system.vo.UserFormSchemaVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 人员自定义字段配置服务实现类
 *
 * @author Chill
 */
@Service
public class HrmFieldServiceImpl extends BaseServiceImpl<HrmFieldMapper, HrmField> implements IHrmFieldService {

	private final HrmFieldGroupMapper fieldGroupMapper;

	public HrmFieldServiceImpl(HrmFieldGroupMapper fieldGroupMapper) {
		this.fieldGroupMapper = fieldGroupMapper;
	}

	@Override
	public List<UserFormSchemaVO> formSchema(String tenantId) {
		// 租户口径与 UserServiceImpl.selectPage 一致：仅超管可指定任意租户
		String resolvedTenant = SecureUtil.isAdministrator()
			? Func.toStr(tenantId, SecureUtil.getTenantId())
			: SecureUtil.getTenantId();
		List<HrmFieldGroup> groups = fieldGroupMapper.selectList(Wrappers.<HrmFieldGroup>lambdaQuery()
			.eq(HrmFieldGroup::getTenantId, resolvedTenant)
			.eq(HrmFieldGroup::getStatus, 1)
			.orderByAsc(HrmFieldGroup::getSort)
			.orderByAsc(HrmFieldGroup::getId));
		if (Func.isEmpty(groups)) {
			return new ArrayList<>();
		}
		List<UserFormSchemaVO> result = new ArrayList<>();
		for (HrmFieldGroup group : groups) {
			List<HrmField> fields = this.list(Wrappers.<HrmField>lambdaQuery()
				.eq(HrmField::getTenantId, resolvedTenant)
				.eq(HrmField::getGroupId, group.getId())
				.eq(HrmField::getStatus, 1)
				.orderByAsc(HrmField::getSort)
				.orderByAsc(HrmField::getId));
			if (Func.isEmpty(fields)) {
				continue;
			}
			UserFormSchemaVO vo = new UserFormSchemaVO();
			vo.setGroupId(group.getId());
			vo.setGroupCode(group.getGroupCode());
			vo.setGroupName(group.getGroupName());
			vo.setGroupType(group.getGroupType());
			vo.setSort(group.getSort());
			List<UserFormSchemaVO.FieldItem> items = new ArrayList<>();
			for (HrmField field : fields) {
				UserFormSchemaVO.FieldItem item = new UserFormSchemaVO.FieldItem();
				item.setFieldId(field.getId());
				item.setPropName(field.getPropName());
				item.setLabel(field.getLabel());
				item.setEleType(Func.toStr(field.getEleType(), "input"));
				item.setRequired(Func.toInt(field.getRequired(), 0));
				item.setSort(field.getSort());
				item.setExtJson(field.getExtJson());
				items.add(item);
			}
			vo.setFields(items);
			result.add(vo);
		}
		result.sort(Comparator.comparing(g -> Func.toInt(g.getSort(), 0)));
		return result;
	}

}
