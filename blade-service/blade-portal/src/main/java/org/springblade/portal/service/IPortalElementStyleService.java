package org.springblade.portal.service;

import org.springblade.portal.dto.PortalElementStyleSaveDTO;
import org.springblade.portal.entity.PortalElementStyle;

import java.util.List;

/**
 * 元素样式库/头条样式库服务（对标泛微 §15.3 B3 elementstylelib / B4 headlinestyle）
 */
public interface IPortalElementStyleService {

	/** 样式列表（styleKind 可空：element 元素样式 / headline 头条样式） */
	List<PortalElementStyle> list(String styleKind);

	/** 新增/编辑（同租户 分类+名称 冲突返回 false） */
	boolean save(PortalElementStyleSaveDTO dto);

	/** 删除 */
	boolean remove(Long id);
}
