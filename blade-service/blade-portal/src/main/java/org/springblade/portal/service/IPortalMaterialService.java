package org.springblade.portal.service;

import org.springblade.portal.dto.PortalMaterialSaveDTO;
import org.springblade.portal.entity.PortalMaterial;

import java.util.List;

/**
 * 素材库服务（对标泛微 materiallib §15.4 C2；文件走资源服务，本表记元数据）
 */
public interface IPortalMaterialService {

	/** 素材列表（dirName 可空） */
	List<PortalMaterial> list(String dirName);

	/** 全部目录名（对标 getDirs） */
	List<String> dirs();

	/** 新增/编辑 */
	boolean save(PortalMaterialSaveDTO dto);

	/** 删除 */
	boolean remove(Long id);
}
