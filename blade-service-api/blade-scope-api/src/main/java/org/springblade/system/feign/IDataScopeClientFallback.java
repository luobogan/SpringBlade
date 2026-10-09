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
package org.springblade.system.feign;

import org.springblade.core.datascope.model.DataScopeModel;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * IDataScopeClientFallback
 *
 * <p><b>为什么不能返回 null</b>：调用方 {@code DataScopeCache#getDataScopeByMapper} 拿到返回值后会
 * 立即读取 {@code getResourceCode()} 判断是否配置了数据权限，null 会当场 NPE，
 * 使整条业务查询以 500 失败（实测：服务发现解析不到实例 → 走降级 → 顶部菜单等接口全部 500）。
 * 降级语义应当是「没有数据权限限制」，故返回与服务端 {@code DataScopeClient} 未命中时一致的
 * 哨兵对象（searched=true、resourceCode 为空）／空集合，让调用方安全地按「无配置」处理。</p>
 *
 * @author Chill
 */
@Component
public class IDataScopeClientFallback implements IDataScopeClient {
	@Override
	public DataScopeModel getDataScopeByMapper(String mapperId, String roleId) {
		return new DataScopeModel(Boolean.TRUE);
	}

	@Override
	public DataScopeModel getDataScopeByCode(String code) {
		return new DataScopeModel(Boolean.TRUE);
	}

	@Override
	public List<Long> getDeptAncestors(Long deptId) {
		return Collections.emptyList();
	}
}
