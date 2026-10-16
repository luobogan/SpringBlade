package org.springblade.portal.service;

import org.springblade.portal.dto.PortalNewsTemplateSaveDTO;
import org.springblade.portal.entity.PortalNewsTemplate;

import java.util.List;

/**
 * 新闻模板库服务（对标泛微 newstemplate §15.4 C1，PAGENEWSTEMPLATE）
 */
public interface IPortalNewsTemplateService {

	/** 模板列表（templateType 可空：inner/outer） */
	List<PortalNewsTemplate> list(String templateType);

	/** 新增/编辑（同租户名称冲突返回 false；is_default=1 时同类型内唯一） */
	boolean save(PortalNewsTemplateSaveDTO dto);

	/** 删除 */
	boolean remove(Long id);

	/** 设为默认模板（同类型内唯一） */
	boolean setDefault(Long id);
}
