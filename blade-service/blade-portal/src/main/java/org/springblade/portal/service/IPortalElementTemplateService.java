package org.springblade.portal.service;

import org.springblade.portal.dto.PortalElementTemplateSaveDTO;
import org.springblade.portal.entity.PortalElementTemplate;

import java.util.List;

/**
 * 元素模板库服务（对标泛微 elementtemplate §15.3 B2，HPELEMENTTEMPLATE）
 */
public interface IPortalElementTemplateService {

	/** 模板列表（elementType 可空） */
	List<PortalElementTemplate> list(String elementType);

	/** 新增/编辑（另存为模板；同租户名称冲突返回 false） */
	boolean save(PortalElementTemplateSaveDTO dto);

	/** 删除 */
	boolean remove(Long id);

	/**
	 * 模板实例化到页面：按模板快照在目标页追加一个元素，返回新元素ID
	 * （对标元素库 instantiate；封存语义不适用于模板，模板仅快照）
	 */
	Long instantiate(Long templateId, Long pageId, Integer rowNo, Integer colSpan);
}
