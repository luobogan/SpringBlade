package org.springblade.portal.service;

import org.springblade.portal.dto.PortalPluginSaveDTO;
import org.springblade.portal.entity.PortalPlugin;

import java.util.List;

/**
 * 门户插件管理服务（对标泛微 pluginmanage §14.2 G）
 */
public interface IPortalPluginService {

	/** 插件列表（按 order_num 排序） */
	List<PortalPlugin> list();

	/** 新增/编辑（同租户 pid 冲突返回 false） */
	boolean save(PortalPluginSaveDTO dto);

	/** 删除 */
	boolean remove(Long id);
}
