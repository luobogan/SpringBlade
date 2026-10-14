package org.springblade.system.feign;

import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.tool.api.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 部门 Feign接口类
 *
 * <p>映射 blade-system 的 DeptController，供 blade-workflow 等模块经服务发现解析组织层级
 * （如矩阵操作者 -13「创建人分部」条件：先取创建人部门，再解析其所属分部）。</p>
 *
 * @author Blade
 */
@FeignClient(
	value = AppConstant.APPLICATION_SYSTEM_NAME,
	fallback = IDeptClientFallback.class
)
public interface IDeptClient {

	String API_PREFIX = "/dept";

	/**
	 * 取某组织节点所属「分部(division)」节点ID。
	 *
	 * <p>解析规则（对齐 ecology hrmdepartment.subcompanyid1）：</p>
	 * <ul>
	 *   <li>传入节点本身是分部(deptType=1) → 返回自身；</li>
	 *   <li>传入部门(deptType=2) → 返回其所属分部 subcompanyId；</li>
	 *   <li>subcompanyId 为空时沿 parent 向上找最近的 deptType=1；</li>
	 *   <li>节点不存在 / 无分部归属 → data 为 null。</li>
	 * </ul>
	 *
	 * @param deptId 组织节点ID（公司/分部/部门均可）
	 * @return 分部节点ID；无归属或节点不存在时 data 为 null
	 */
	@GetMapping(API_PREFIX + "/division-id-by-dept")
	R<Long> divisionIdByDept(@RequestParam("deptId") Long deptId);

}
