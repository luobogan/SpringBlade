package org.springblade.system.service;

import org.springblade.core.mp.base.BaseService;
import org.springblade.system.dto.ScopeFieldBatch;
import org.springblade.system.entity.ScopeField;

import java.util.List;

/**
 * 数据权限可见字段配置服务
 */
public interface IScopeFieldService extends BaseService<ScopeField> {

	/**
	 * 按菜单/数据权限查询列配置（升序，含不可见字段，由前端决定是否过滤）
	 */
	List<ScopeField> listByScope(Long scopeId, Long menuId, String resourceCode);

	/**
	 * 批量保存（按 scopeId + menuId 做差集合并），返回是否成功
	 */
	boolean saveBatchConfig(ScopeFieldBatch batch);
}
