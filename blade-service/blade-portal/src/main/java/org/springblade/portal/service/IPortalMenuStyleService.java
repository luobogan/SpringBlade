package org.springblade.portal.service;

import org.springblade.portal.dto.PortalMenuStyleSaveDTO;
import org.springblade.portal.entity.PortalMenuStyle;

import java.util.List;

/**
 * 菜单样式库服务（对标泛微 menustylelib §14.2 E，hpMenuStyle menuh/menuv）
 */
public interface IPortalMenuStyleService {

	/** 样式列表（styleType 可空；按 sort/id 排序） */
	List<PortalMenuStyle> list(String styleType);

	/** 新增/编辑（同租户 名称+类型 冲突返回 false） */
	boolean save(PortalMenuStyleSaveDTO dto);

	/** 删除 */
	boolean remove(Long id);
}
