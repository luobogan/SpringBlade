package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.workflow.odoc.config.OdocProperties;
import org.springblade.workflow.odoc.entity.WfOdocDoc;
import org.springblade.workflow.odoc.entity.WfOdocDocVersion;
import org.springblade.workflow.odoc.mapper.WfOdocDocMapper;
import org.springblade.workflow.odoc.mapper.WfOdocDocVersionMapper;
import org.springblade.workflow.odoc.service.IWfOdocDocService;
import org.springblade.workflow.odoc.store.IOdocFileStore;
import org.springblade.workflow.odoc.vo.OdocDocInitVO;
import org.springblade.workflow.odoc.vo.OdocEditorConfigVO;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 公文正文文档服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfOdocDocServiceImpl implements IWfOdocDocService {

	private final WfOdocDocMapper docMapper;
	private final WfOdocDocVersionMapper versionMapper;
	private final IOdocFileStore fileStore;
	private final OdocProperties properties;

	private static final Integer DOC_STATUS_DRAFT = 9;

	@Override
	public OdocDocInitVO initDoc(Long instanceId, Long defId, String title, boolean edit) {
		WfOdocDoc doc = docMapper.selectOne(
			Wrappers.<WfOdocDoc>lambdaQuery().eq(WfOdocDoc::getInstanceId, instanceId));
		if (doc == null) {
			doc = new WfOdocDoc();
			doc.setDefId(defId);
			doc.setInstanceId(instanceId);
			doc.setTitle(title);
			doc.setDocStatus(DOC_STATUS_DRAFT);
			doc.setHasUsedTemplet(0);
			doc.setIsTest(0);
			docMapper.insert(doc);
		}
		OdocDocInitVO vo = new OdocDocInitVO();
		vo.setDocId(doc.getId());
		vo.setTitle(doc.getTitle());
		vo.setDocStatus(doc.getDocStatus());

		WfOdocDocVersion current = doc.getDocEditionId() == null ? null
			: versionMapper.selectById(doc.getDocEditionId());
		vo.setCurrentVersion(current);

		List<WfOdocDocVersion> versions = versionMapper.selectList(
			Wrappers.<WfOdocDocVersion>lambdaQuery().eq(WfOdocDocVersion::getDocId, doc.getId())
				.orderByDesc(WfOdocDocVersion::getVersionId));
		vo.setVersions(versions);

		boolean canEdit = isDocServerConfigured() && current != null && isEditableType(current.getDocFileType());
		vo.setCanEdit(canEdit);
		vo.setEditorConfig(canEdit && edit ? buildEditorConfig(doc.getId(), current.getId(), true) : null);
		return vo;
	}

	@Override
	public WfOdocDoc getDoc(Long docId) {
		return docMapper.selectById(docId);
	}

	@Override
	public IPage<WfOdocDoc> picker(Long current, Long pageSize, String keyword, Long excludeInstanceId) {
		Page<WfOdocDoc> page = new Page<>(
			(current == null || current < 1) ? 1 : current,
			(pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 200));
		return docMapper.selectPage(page, Wrappers.<WfOdocDoc>lambdaQuery()
			// 测试流程产生的文档不进选择器
			.eq(WfOdocDoc::getIsTest, 0)
			// 排除当前实例自身的文档：避免把本文档关联给自己
			.ne(excludeInstanceId != null, WfOdocDoc::getInstanceId, excludeInstanceId)
			.like(keyword != null && !keyword.isBlank(), WfOdocDoc::getTitle, keyword)
			.orderByDesc(WfOdocDoc::getCreateTime));
	}

	@Override
	public List<WfOdocDocVersion> listVersions(Long docId) {
		return versionMapper.selectList(
			Wrappers.<WfOdocDocVersion>lambdaQuery().eq(WfOdocDocVersion::getDocId, docId)
				.orderByDesc(WfOdocDocVersion::getVersionId));
	}

	@Override
	public WfOdocDocVersion saveVersion(Long docId, Long fileId, String fileName,
										Integer docFileType, String versionDetail) {
		WfOdocDoc doc = docMapper.selectById(docId);
		if (doc == null) {
			throw new IllegalArgumentException("正文不存在: " + docId);
		}
		Integer next = nextVersionId(docId);
		WfOdocDocVersion v = new WfOdocDocVersion();
		v.setDocId(docId);
		v.setVersionId(next);
		v.setFileId(fileId);
		v.setFileName(fileName);
		v.setDocFileType(docFileType);
		v.setHasUsedTemplet(doc.getHasUsedTemplet());
		v.setIsExtFile(0);
		v.setVersionDetail(versionDetail);
		// 回调路径（permitAll）无登录态，meta 填充可能为 null，这里显式兜底避免空操作用户
		try {
			v.setCreateUser(SecureUtil.getUserId());
		} catch (Exception ignored) {
			v.setCreateUser(0L);
		}
		versionMapper.insert(v);

		doc.setDocEditionId(v.getId());
		docMapper.updateById(doc);
		return v;
	}

	@Override
	public WfOdocDocVersion rollback(Long docId, Long toVersionId) {
		WfOdocDocVersion target = versionMapper.selectById(toVersionId);
		if (target == null || !Objects.equals(target.getDocId(), docId)) {
			throw new IllegalArgumentException("回滚目标版本不存在或不属于该正文");
		}
		return saveVersion(docId, target.getFileId(), target.getFileName(),
			target.getDocFileType(), "回滚至 v" + target.getVersionId());
	}

	@Override
	public OdocEditorConfigVO buildEditorConfig(Long docId, Long versionId, boolean edit) {
		if (!isDocServerConfigured()) {
			return null;
		}
		WfOdocDocVersion version = versionMapper.selectById(versionId);
		if (version == null) {
			return null;
		}
		boolean editable = edit && isEditableType(version.getDocFileType());

		OdocEditorConfigVO vo = new OdocEditorConfigVO();
		vo.setDocServerUrl(stripTrailingSlash(properties.getDocServerUrl()));
		vo.setDocumentType("word");

		OdocEditorConfigVO.Document document = new OdocEditorConfigVO.Document();
		document.setFileType(extOf(version.getDocFileType()));
		document.setKey(docId + "_" + versionId);
		document.setTitle(version.getFileName());
		document.setUrl(stripTrailingSlash(properties.getPublicBaseUrl())
			+ "/odoc/doc/version/" + versionId + "/content");
		document.getPermissions().setEdit(editable);
		vo.setDocument(document);

		OdocEditorConfigVO.EditorConfig ec = new OdocEditorConfigVO.EditorConfig();
		ec.setCallbackUrl(stripTrailingSlash(properties.getPublicBaseUrl())
			+ "/odoc/doc/callback?docId=" + docId + "&versionId=" + versionId);
		ec.setLang(properties.getLang());
		ec.setMode(editable ? "edit" : "view");
		ec.getUser().setId(String.valueOf(SecureUtil.getUserId()));
		ec.getUser().setName(String.valueOf(SecureUtil.getUser().getUserName()));
		vo.setEditorConfig(ec);
		return vo;
	}

	@Override
	public byte[] content(Long versionId) {
		WfOdocDocVersion version = versionMapper.selectById(versionId);
		if (version == null) {
			throw new IllegalArgumentException("版本不存在: " + versionId);
		}
		return fileStore.download(version.getFileId());
	}

	@Override
	public String contentFileName(Long versionId) {
		WfOdocDocVersion version = versionMapper.selectById(versionId);
		if (version == null) {
			return "";
		}
		if (version.getFileName() != null && !version.getFileName().isBlank()) {
			return version.getFileName();
		}
		return fileStore.resolveFileName(version.getFileId());
	}

	@Override
	public void handleCallback(Long docId, Long versionId, int status, String url) {
		// OnlyOffice 状态：2=文档已编辑并可供下载保存；6=无变化关闭；其余忽略
		if (status != 2 || url == null || url.isBlank()) {
			return;
		}
		WfOdocDocVersion edited = versionMapper.selectById(versionId);
		if (edited == null) {
			log.warn("OnlyOffice 回调指向的版本不存在 docId={} versionId={}", docId, versionId);
			return;
		}
		Long fileId = fileStore.storeFromUrl(url, edited.getFileName());
		saveVersion(docId, fileId, edited.getFileName(), edited.getDocFileType(), "在线编辑保存");
		log.info("OnlyOffice 回调落库完成 docId={} versionId={} newFileId={}", docId, versionId, fileId);
	}

	@Override
	public WfOdocDoc getDocByInstanceId(Long instanceId) {
		return docMapper.selectOne(
			Wrappers.<WfOdocDoc>lambdaQuery().eq(WfOdocDoc::getInstanceId, instanceId));
	}

	@Override
	public WfOdocDocVersion getCurrentVersion(Long docId) {
		WfOdocDoc doc = docMapper.selectById(docId);
		if (doc == null || doc.getDocEditionId() == null) {
			return null;
		}
		return versionMapper.selectById(doc.getDocEditionId());
	}

	@Override
	public WfOdocDocVersion saveTaohongVersion(Long docId, byte[] bytes, String fileName,
											   Integer docFileType, String versionDetail) {
		WfOdocDoc doc = docMapper.selectById(docId);
		if (doc == null) {
			throw new IllegalArgumentException("正文不存在: " + docId);
		}
		Long fileId = fileStore.store(bytes, fileName);
		Integer next = nextVersionId(docId);
		WfOdocDocVersion v = new WfOdocDocVersion();
		v.setDocId(docId);
		v.setVersionId(next);
		v.setFileId(fileId);
		v.setFileName(fileName);
		v.setDocFileType(docFileType);
		v.setHasUsedTemplet(1);
		v.setIsExtFile(0);
		v.setVersionDetail(versionDetail);
		try {
			v.setCreateUser(SecureUtil.getUserId());
		} catch (Exception ignored) {
			v.setCreateUser(0L);
		}
		versionMapper.insert(v);

		doc.setDocEditionId(v.getId());
		doc.setHasUsedTemplet(1);
		docMapper.updateById(doc);
		return v;
	}

	@Override
	public WfOdocDocVersion clearTaohong(Long docId) {
		WfOdocDoc doc = docMapper.selectById(docId);
		if (doc == null) {
			throw new IllegalArgumentException("正文不存在: " + docId);
		}
		List<WfOdocDocVersion> all = versionMapper.selectList(
			Wrappers.<WfOdocDocVersion>lambdaQuery().eq(WfOdocDocVersion::getDocId, docId)
				.orderByDesc(WfOdocDocVersion::getVersionId));
		WfOdocDocVersion target = all.stream()
			.filter(v -> v.getHasUsedTemplet() == null || v.getHasUsedTemplet() != 1)
			.findFirst()
			.orElse(null);
		if (target == null) {
			throw new IllegalArgumentException("没有可回退的未套红版本");
		}
		Integer next = nextVersionId(docId);
		WfOdocDocVersion v = new WfOdocDocVersion();
		v.setDocId(docId);
		v.setVersionId(next);
		v.setFileId(target.getFileId());
		v.setFileName(target.getFileName());
		v.setDocFileType(target.getDocFileType());
		v.setHasUsedTemplet(0);
		v.setIsExtFile(0);
		v.setVersionDetail("取消套红，回退至 v" + target.getVersionId());
		try {
			v.setCreateUser(SecureUtil.getUserId());
		} catch (Exception ignored) {
			v.setCreateUser(0L);
		}
		versionMapper.insert(v);

		doc.setDocEditionId(v.getId());
		doc.setHasUsedTemplet(0);
		docMapper.updateById(doc);
		return v;
	}

	private Integer nextVersionId(Long docId) {
		List<WfOdocDocVersion> all = versionMapper.selectList(
			Wrappers.<WfOdocDocVersion>lambdaQuery().eq(WfOdocDocVersion::getDocId, docId)
				.select(WfOdocDocVersion::getVersionId));
		return all.stream().map(WfOdocDocVersion::getVersionId)
			.max(Comparator.naturalOrder()).orElse(0) + 1;
	}

	private boolean isDocServerConfigured() {
		return properties.getDocServerUrl() != null && !properties.getDocServerUrl().isBlank();
	}

	private boolean isEditableType(Integer docFileType) {
		// OnlyOffice 仅可编辑 doc/docx；pdf/ofd 只能预览
		return docFileType != null && (docFileType == 3 || docFileType == 7);
	}

	private String extOf(Integer docFileType) {
		if (docFileType == null) {
			return "docx";
		}
		switch (docFileType) {
			case 3:
				return "doc";
			case 7:
				return "docx";
			case 12:
				return "pdf";
			case 13:
				return "ofd";
			default:
				return "docx";
		}
	}

	private String stripTrailingSlash(String s) {
		if (s == null) {
			return "";
		}
		return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
	}
}
