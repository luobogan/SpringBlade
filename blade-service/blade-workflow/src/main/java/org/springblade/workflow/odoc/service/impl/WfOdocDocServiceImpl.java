package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.common.wps.WpsEditorConfig;
import org.springblade.common.wps.WpsFileId;
import org.springblade.common.wps.WpsFileMeta;
import org.springblade.common.wps.WpsSettings;
import org.springblade.workflow.odoc.config.WpsSettingStore;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.workflow.odoc.OdocUrlSigner;
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

import java.util.ArrayList;
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
	private final OdocUrlSigner signer;
	private final WpsSettingStore settings;

	private static final Integer DOC_STATUS_DRAFT = 9;

	@Override
	public OdocDocInitVO initDoc(Long instanceId, Long defId, String title, boolean edit) {
		// 历史 4 参入口默认 editor=auto（= 当前 OnlyOffice 行为），保持存量前端零变化
		return initDoc(instanceId, defId, title, edit, "auto");
	}

	@Override
	public OdocDocInitVO initDoc(Long instanceId, Long defId, String title, boolean edit, String editor) {
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

		String eff = (editor == null || editor.isBlank()) ? "auto" : editor;
		// auto 兜底：WPS 已配置（单一配置源 doc_engine_setting）时优先走 WPS 方案，
		// 保证前端「在线编辑」按钮能真正弹出 WPS 编辑器；未配置才回落 OnlyOffice。
		boolean useWps = ("wps".equals(eff) || "auto".equals(eff)) && isWpsConfigured();
		if (useWps) {
			// WPS WebOffice 方案（WPS 统一改造 P2）
			boolean editable = edit && current != null && isEditableType(current.getDocFileType());
			vo.setEditorType("wps");
			vo.setCanEdit(isWpsConfigured() && current != null
				&& isEditableType(current.getDocFileType()));
			vo.setWpsEditorConfig(buildWpsEditorConfig(doc.getId(), current.getId(), editable));
			vo.setEditorConfig(null);
		} else {
			// 现有 OnlyOffice 路径（auto 默认 = OnlyOffice）
			boolean canEdit = isDocServerConfigured() && current != null
				&& isEditableType(current.getDocFileType());
			vo.setEditorType("onlyoffice");
			vo.setCanEdit(canEdit);
			vo.setEditorConfig(canEdit && edit ? buildEditorConfig(doc.getId(), current.getId(), true) : null);
			vo.setWpsEditorConfig(null);
		}
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
			+ "/odoc/doc/version/" + versionId + "/content?token=" + signer.sign(String.valueOf(versionId)));
		document.getPermissions().setEdit(editable);
		vo.setDocument(document);

		OdocEditorConfigVO.EditorConfig ec = new OdocEditorConfigVO.EditorConfig();
		ec.setCallbackUrl(stripTrailingSlash(properties.getPublicBaseUrl())
			+ "/odoc/doc/callback?docId=" + docId + "&versionId=" + versionId
			+ "&token=" + signer.sign(docId + ":" + versionId));
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

	// ───────────── WPS WebOffice（WPS 统一改造 P2） ─────────────

	@Override
	public boolean isWpsConfigured() {
		return settings.current().isConfigured();
	}

	@Override
	public WpsEditorConfig buildWpsEditorConfig(Long docId, Long versionId, boolean edit) {
		if (!isWpsConfigured()) {
			return null;
		}
		WfOdocDocVersion version = versionMapper.selectById(versionId);
		if (version == null) {
			return null;
		}
		boolean editable = edit && isEditableType(version.getDocFileType());
		final WpsSettings wps = settings.current();
		WpsEditorConfig wc = new WpsEditorConfig();
		wc.setDomain(WpsFileId.DOMAIN_ODOC);
		wc.setMode(editable ? "edit" : "view");
		wc.setReadOnly(!editable);
		wc.setFileName(version.getFileName());
		wc.setFileExt(extOf(version.getDocFileType()));
		wc.setAppId(wps.getAppId());
		// 下发给 WPS 中台的 fileId 必须满足中台字符集（数字/字母/下划线），故用 wire 形态
		// odoc_{docId}；token 的 resource（wps:{wireFileId}）与回调/upload 验签口径随之自动一致
		String wire = WpsFileId.wireValue(WpsFileId.DOMAIN_ODOC, String.valueOf(docId));
		wc.setFileId(wire);
		wc.setToken(signer.sign("wps:" + wire));
		wc.setTimestamp(System.currentTimeMillis() / 1000);
		wc.setPermission(editable ? "write" : "read");
		wc.setUserId(String.valueOf(SecureUtil.getUserId()));
		try {
			wc.setUserName(SecureUtil.getUser().getUserName());
		} catch (Exception ignored) {
			wc.setUserName("");
		}
		wc.setJssdkUrl(wps.getJssdkUrl());
		// contentUrl 供前端/SDK 参考（实际下载由 WPS 中台经 v3 协议拉取），指向 workflow 直连端点
		String base = stripTrailingSlash(wps.getPublicBase());
		wc.setContentUrl(base + "/blade-workflow/odoc/wps/content?docId=" + docId
			+ "&version=" + version.getVersionId() + "&token=" + signer.sign(docId + ":" + version.getVersionId()));
		return wc;
	}

	@Override
	public WpsFileMeta wpsMeta(Long docId) {
		WfOdocDoc doc = docMapper.selectById(docId);
		if (doc == null) {
			return null;
		}
		WfOdocDocVersion cur = doc.getDocEditionId() == null ? null
			: versionMapper.selectById(doc.getDocEditionId());
		return cur == null ? null : toWpsMeta(docId, cur);
	}

	@Override
	public List<WpsFileMeta> wpsVersions(Long docId) {
		List<WfOdocDocVersion> all = versionMapper.selectList(
			Wrappers.<WfOdocDocVersion>lambdaQuery().eq(WfOdocDocVersion::getDocId, docId)
				.orderByAsc(WfOdocDocVersion::getVersionId));
		List<WpsFileMeta> out = new ArrayList<>();
		for (WfOdocDocVersion v : all) {
			out.add(toWpsMeta(docId, v));
		}
		return out;
	}

	@Override
	public WpsFileMeta wpsVersionMeta(Long versionId) {
		WfOdocDocVersion v = versionMapper.selectById(versionId);
		return v == null ? null : toWpsMeta(v.getDocId(), v);
	}

	@Override
	public WpsFileMeta saveWpsVersion(Long docId, byte[] content, String name) {
		WfOdocDoc doc = docMapper.selectById(docId);
		if (doc == null) {
			throw new IllegalArgumentException("正文不存在: " + docId);
		}
		Long fileId = fileStore.store(content, name);
		Integer next = nextVersionId(docId);
		WfOdocDocVersion v = new WfOdocDocVersion();
		v.setDocId(docId);
		v.setVersionId(next);
		v.setFileId(fileId);
		v.setFileName(name);
		v.setDocFileType(extToType(name));
		// R6：保留文档级套红态（hasUsedTemplet），WPS 保存新版本不重置（待产品确认）
		v.setHasUsedTemplet(doc.getHasUsedTemplet());
		v.setIsExtFile(0);
		v.setVersionDetail("WPS在线编辑保存");
		try {
			v.setCreateUser(SecureUtil.getUserId());
		} catch (Exception ignored) {
			v.setCreateUser(0L);
		}
		versionMapper.insert(v);
		doc.setDocEditionId(v.getId());
		docMapper.updateById(doc);
		WpsFileMeta m = toWpsMeta(docId, v);
		m.setSize((long) content.length);
		return m;
	}

	@Override
	public byte[] wpsVersionContent(Long docId, int version) {
		WfOdocDocVersion v = versionMapper.selectOne(
			Wrappers.<WfOdocDocVersion>lambdaQuery()
				.eq(WfOdocDocVersion::getDocId, docId)
				.eq(WfOdocDocVersion::getVersionId, version));
		if (v == null) {
			return null;
		}
		return fileStore.download(v.getFileId());
	}

	private WpsFileMeta toWpsMeta(Long docId, WfOdocDocVersion v) {
		WpsFileMeta m = new WpsFileMeta();
		m.setId(WpsFileId.wireValue(WpsFileId.DOMAIN_ODOC, String.valueOf(docId)));
		m.setName(v.getFileName());
		m.setVersion(v.getVersionId());
		m.setSize(0L);
		long now = System.currentTimeMillis() / 1000;
		m.setCreateTime(now);
		m.setModifyTime(now);
		return m;
	}

	private Integer extToType(String name) {
		if (name == null) {
			return 7;
		}
		String lower = name.toLowerCase();
		if (lower.endsWith(".doc")) {
			return 3;
		}
		if (lower.endsWith(".pdf")) {
			return 12;
		}
		if (lower.endsWith(".ofd")) {
			return 13;
		}
		return 7;
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
