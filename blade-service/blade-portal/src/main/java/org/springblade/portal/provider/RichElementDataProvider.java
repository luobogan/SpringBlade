package org.springblade.portal.provider;

import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.vo.ElementDataVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 富文本内容元素 Provider
 * <p>
 * 零外部取数：把元素私有配置中的 html 回传给前端渲染。
 * </p>
 */
@Component
public class RichElementDataProvider implements IPortalElementDataProvider {

	@Override
	public String type() {
		return PortalConstant.ELEMENT_RICH;
	}

	@Override
	public ElementDataVO load(ElementDataContext ctx) {
		ElementDataVO vo = new ElementDataVO();
		vo.setType(type());

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("html", ctx.getStringConfig("html"));
		row.put("align", ctx.getStringConfig("align"));

		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(row);
		vo.setData(rows);
		return vo;
	}
}