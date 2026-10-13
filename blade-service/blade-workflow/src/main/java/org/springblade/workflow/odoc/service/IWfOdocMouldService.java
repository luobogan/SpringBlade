package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.entity.WfOdocMould;
import org.springblade.workflow.odoc.entity.WfOdocMouldBookmark;
import org.springblade.workflow.odoc.entity.WfOdocMouldFieldMap;

import java.util.List;

/**
 * 红头模板/书签/字段映射管理。
 *
 * <p>对应 E9 docmould / MouldBookMark / DocMouldBookMark。套红模板是含红头与书签的
 * .docx；上传后扫描书签，建立「书签 → 表单字段」映射，供套红合成取值。</p>
 */
public interface IWfOdocMouldService {

	/** 新建模板（fileId 为已上传到文件服务的 .docx） */
	WfOdocMould createMould(String mouldName, Integer mouldType, Long fileId, Long defId, String docTypeCode);

	WfOdocMould getMould(Long mouldId);

	/** 可用模板列表（启用；defId 为空=全局 或 命中当前流程） */
	List<WfOdocMould> listMoulds(Long defId, Integer mouldType);

	void removeMould(Long mouldId);

	/** 扫描模板 docx 书签并重存（覆盖式），返回书签清单 */
	List<WfOdocMouldBookmark> extractBookmarks(Long mouldId);

	List<WfOdocMouldBookmark> listBookmarks(Long mouldId);

	/** 更新书签属性（isContent / descript / elementType），按 id 逐条更新 */
	void updateBookmarks(Long mouldId, List<WfOdocMouldBookmark> bookmarks);

	/** 覆盖保存「书签 → 表单字段」映射 */
	void saveFieldMaps(Long mouldId, Long defId, List<WfOdocMouldFieldMap> maps);

	/** 查询模板的字段映射（编辑回显用） */
	List<WfOdocMouldFieldMap> listFieldMaps(Long mouldId);
}
