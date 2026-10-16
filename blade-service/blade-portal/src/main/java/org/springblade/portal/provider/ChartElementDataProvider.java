package org.springblade.portal.provider;

import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.vo.ElementDataVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 图表元素 Provider
 * <p>
 * 零外部取数：直接把元素私有配置（chartType / dataKey / staticData 等）
 * 原样回传给前端，由前端用 @ant-design/plots 渲染。
 * 后续若接入统计服务，只需在此替换为真实数据查询，引擎与前端无需改动。
 * </p>
 */
@Component
public class ChartElementDataProvider implements IPortalElementDataProvider {

	@Override
	public String type() {
		return PortalConstant.ELEMENT_CHART;
	}

	@Override
	public ElementDataVO load(ElementDataContext ctx) {
		ElementDataVO vo = new ElementDataVO();
		vo.setType(type());

		Map<String, Object> row = new LinkedHashMap<>();
		String chartType = ctx.getStringConfig("chartType");
		row.put("chartType", chartType == null || chartType.isBlank() ? "column" : chartType);
		row.put("data", ctx.getConfig().get("data"));
		row.put("xField", ctx.getStringConfig("xField"));
		row.put("yField", ctx.getStringConfig("yField"));
		// 组织上下文透传：业务图表 Provider 接入真实数据时，可据此按组织过滤（§22 数据型元素按组织过滤）
		row.put("orgScope", ctx.getOrgScope());
		row.put("tenantId", ctx.getTenantId());
		row.put("deptId", ctx.getDeptId());
		row.put("branchId", ctx.getBranchId());

		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(row);
		vo.setData(rows);
		return vo;
	}
}