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

import java.util.List;

/**
 * 租户菜单服务接口
 *
 * @author SpringBlade
 */
public interface ITenantMenuService {

	/**
	 * 为租户复制菜单结构
	 * 从超级管理员的菜单中复制产品包对应的菜单，为租户创建新的菜单记录
	 *
	 * @param tenantId       租户ID
	 * @param packageId      产品包ID
	 * @param menuIds        产品包中的菜单ID列表
	 * @return 复制的菜单数量
	 */
	int copyMenusForTenant(String tenantId, Long packageId, List<Long> menuIds);

	/**
	 * 为租户复制角色权限
	 *
	 * @param tenantId  租户ID
	 * @param roleIds   角色ID列表
	 * @param menuIds   菜单ID列表
	 */
	void copyRoleMenusForTenant(String tenantId, List<Long> roleIds, List<Long> menuIds);

	/**
	 * 获取租户的菜单ID列表
	 *
	 * @param tenantId  租户ID
	 * @param menuCodes 菜单编码列表（用于匹配）
	 * @return 租户的菜单ID列表
	 */
	List<Long> getTenantMenuIds(String tenantId, List<Long> menuCodes);
}
