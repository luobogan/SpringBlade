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

import org.springblade.system.entity.TenantPackage;
import org.springblade.system.vo.CheckedTreeVO;
import org.springblade.system.vo.GrantTreeVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 租户产品包服务接口
 *
 * @author Chill
 */
public interface ITenantPackageService extends IService<TenantPackage> {

	/**
	 * 自定义分页
	 */
	IPage<TenantPackage> page(IPage<TenantPackage> page, TenantPackage tenantPackage);

	/**
	 * 获取菜单权限分配树形结构
	 */
	GrantTreeVO grantTree();

	/**
	 * 获取产品包已分配的菜单权限
	 */
	CheckedTreeVO packageMenuKeys(Long packageId);

	/**
	 * 为产品包分配菜单权限
	 */
	boolean grantMenu(Long packageId, List<Long> menuIds);

	/**
	 * 移除产品包的菜单权限
	 */
	boolean removeMenu(Long packageId);

	/**
	 * 根据产品包ID获取菜单列表
	 */
	List<Long> getMenuIdsByPackageId(Long packageId);

	/**
	 * 根据产品包ID获取菜单编码列表
	 */
	List<String> getMenuCodesByPackageId(Long packageId);
}
