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
import org.springblade.system.entity.HrmField;
import org.springblade.system.vo.UserFormSchemaVO;

import java.util.List;

/**
 * 人员自定义字段配置服务类（对齐 ecology HrmFieldManager）
 *
 * @author Chill
 */
public interface IHrmFieldService extends BaseService<HrmField> {

	/**
	 * 获取用户新增表单 schema（分组 + 启用中的字段元数据）
	 * <p>
	 * 对齐 ecology {@code getHrmResourceAddForm}：前端据此动态渲染表单，
	 * 改配置（增/停/必填）即时生效，无需改前端代码。
	 *
	 * @param tenantId 租户编号（超管可指定，其他用户强制当前会话租户）
	 * @return 分组 + 字段列表；无配置时返回空集合
	 */
	List<UserFormSchemaVO> formSchema(String tenantId);

}
