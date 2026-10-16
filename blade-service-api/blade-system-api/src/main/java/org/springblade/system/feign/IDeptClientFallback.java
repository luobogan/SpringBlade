package org.springblade.system.feign;

import org.springblade.core.tool.api.R;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 部门 Feign失败配置
 *
 * <p>返回 null（调用方据此走 fail-closed 逻辑：矩阵 -13 条件取值失败 → 该条件不参与匹配，
 * 绝不因组织服务不可用而错误命中或阻塞流转）。</p>
 *
 * @author Blade
 */
@Component
public class IDeptClientFallback implements IDeptClient {

	@Override
	public R<Long> divisionIdByDept(Long deptId) {
		return null;
	}

	@Override
	public R<List<Long>> ancestorIds(Long deptId) {
		return null;
	}

	@Override
	public R<List<Long>> childIds(Long deptId) {
		return null;
	}

}
