package org.springblade.portal.provider;

import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.vo.ElementDataVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 快捷链接元素 Provider
 * <p>
 * 零外部取数：把元素私有配置中的 links 数组（name/icon/url/color）回传给前端。
 * </p>
 */
@Component
public class LinksElementDataProvider implements IPortalElementDataProvider {

	@Override
	public String type() {
		return PortalConstant.ELEMENT_LINKS;
	}

	@Override
	public ElementDataVO load(ElementDataContext ctx) {
		ElementDataVO vo = new ElementDataVO();
		vo.setType(type());

		List<Map<String, Object>> rows = new ArrayList<>();
		Object links = ctx.getConfig().get("links");
		if (links instanceof List<?> list) {
			for (Object item : list) {
				if (!(item instanceof Map<?, ?> raw)) {
					continue;
				}
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("name", raw.get("name"));
				row.put("icon", raw.get("icon"));
				row.put("url", raw.get("url"));
				row.put("color", raw.get("color"));
				rows.add(row);
			}
		}
		vo.setData(rows);
		return vo;
	}
}