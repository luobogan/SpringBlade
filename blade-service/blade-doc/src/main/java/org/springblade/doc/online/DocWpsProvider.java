package org.springblade.doc.online;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.common.wps.WpsCallbackSupport;
import org.springblade.common.wps.WpsFileId;
import org.springblade.common.wps.WpsFileMeta;
import org.springblade.common.wps.WpsFileProvider;
import org.springblade.doc.config.WpsSettingStore;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.entity.DocImageFile;
import org.springblade.doc.mapper.DocDetailMapper;
import org.springblade.doc.mapper.DocImageFileMapper;
import org.springblade.doc.sign.DocUrlSigner;
import org.springblade.doc.store.IDocFileStore;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识管理（blade-doc）的 WPS 文件存储 SPI 实现。
 *
 * <p>WPS 统一改造 P1-1.5：把原 {@code WpsCallbackController} 中与 {@code DocImageFile}/
 * {@code DocDetail} 耦合的解析、版本链、保存落库全部迁入此处；控制器退化为
 * HTTP 协议编排（端点、鉴权、响应包装），不再依赖任何业务表。</p>
 *
 * <p>收发文（odoc）在 P2 提供 {@code domain()=odoc} 的另一个实现，同一个
 * {@code WpsCallbackController} 按 {@link WpsFileId#domain()} 路由即可复用整套 v3 协议。</p>
 *
 * <p><b>兼容</b>：知识管理既有 WPS 会话使用纯数字 fileId（{@link WpsFileId#legacy()}），
 * 本实现按 {@code bizId} 直接操作，不改变已下发给 WPS 中台的 fileId 形态。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocWpsProvider implements WpsFileProvider {

	private final DocDetailMapper detailMapper;
	private final DocImageFileMapper imageFileMapper;
	private final IDocFileStore fileStore;
	private final DocUrlSigner signer;
	private final WpsSettingStore settings;

	@Override
	public String domain() {
		return WpsFileId.DOMAIN_DOC;
	}

	/** 生效的公网 base：统一走 WpsSettingStore（库表优先、空白回退兜底），与编辑器配置同源 */
	@Override
	public String publicBase() {
		return settings.current().getPublicBase();
	}

	/**
	 * fileId → 实际文件ID：优先按 DocImageFile.fileId 命中；否则视为 docId 取其主文件。
	 * 与原控制器 {@code actualFileId} 逐字一致。
	 */
	private Long actualFileId(String bizId) {
		Long fid = parseLong(bizId);
		if (fid == null) {
			return null;
		}
		DocImageFile img = imageFileMapper.selectOne(
			Wrappers.<DocImageFile>lambdaQuery().eq(DocImageFile::getFileId, fid));
		if (img != null) {
			return fid;
		}
		DocDetail doc = detailMapper.selectById(fid);
		if (doc != null && doc.getMainFileId() != null) {
			return doc.getMainFileId();
		}
		return fid;
	}

	private DocImageFile imageOf(Long actualFileId) {
		return actualFileId == null ? null
			: imageFileMapper.selectOne(Wrappers.<DocImageFile>lambdaQuery().eq(DocImageFile::getFileId, actualFileId));
	}

	private Long docIdOf(String bizId) {
		Long af = actualFileId(bizId);
		DocImageFile img = imageOf(af);
		return img != null ? img.getDocId() : af;
	}

	/** 版本链：主文件正文/编辑版本（versionId 非空）按 versionId 正序，v1 最早 */
	@Override
	public List<WpsFileMeta> versions(String bizId) {
		List<DocImageFile> list = imageFileMapper.selectList(Wrappers.<DocImageFile>lambdaQuery()
			.eq(DocImageFile::getDocId, docIdOf(bizId)).eq(DocImageFile::getDocFileType, 2)
			.isNotNull(DocImageFile::getVersionId)
			.orderByAsc(DocImageFile::getVersionId));
		List<WpsFileMeta> out = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			out.add(toMeta(WpsFileId.of(domain(), bizId), list.get(i), i + 1));
		}
		return out;
	}

	@Override
	public WpsFileMeta info(String bizId) {
		Long af = actualFileId(bizId);
		if (af == null) {
			return null;
		}
		List<WpsFileMeta> chain = versions(bizId);
		return toMeta(WpsFileId.of(domain(), bizId), imageOf(af), chain.isEmpty() ? 1 : chain.size());
	}

	@Override
	public String downloadUrl(String bizId) {
		Long af = actualFileId(bizId);
		return af == null ? null : publicBase() + "/doc/file/" + af + "/content?token=" + signer.sign(String.valueOf(af));
	}

	@Override
	public String uploadUrl(String wireFileId, String name) {
		// wireFileId 原样回带：存量知识管理会话是纯数字，结构化后是 doc:xxx，
		// WPS 中台后续 PUT 必须与本次 address 返回的 fileId 完全一致。
		String url = publicBase() + "/v3/3rd/files/" + wireFileId + "/upload";
		if (name != null && !name.isBlank()) {
			url += "?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
		}
		return url;
	}

	@Override
	public String versionDownloadUrl(String bizId, int version) {
		List<WpsFileMeta> chain = versions(bizId);
		int idx = version - 1;
		if (idx < 0 || idx >= chain.size()) {
			return null;
		}
		// 版本链的顺序与 DocImageFile 一致，重新查一次拿到真实 fileId
		List<DocImageFile> list = imageFileMapper.selectList(Wrappers.<DocImageFile>lambdaQuery()
			.eq(DocImageFile::getDocId, docIdOf(bizId)).eq(DocImageFile::getDocFileType, 2)
			.isNotNull(DocImageFile::getVersionId)
			.orderByAsc(DocImageFile::getVersionId));
		Long vf = list.get(idx).getFileId();
		return publicBase() + "/doc/file/" + vf + "/content?token=" + signer.sign(String.valueOf(vf));
	}

	/**
	 * 保存为新版本：写文件存储 + 新 DocImageFile 记录 + 回写文档主文件ID。
	 * 空内容由调用方拦截（入口返回 40005）。
	 */
	@Override
	public WpsFileMeta saveNewVersion(String bizId, byte[] content, String name) {
		Long af = actualFileId(bizId);
		DocImageFile img = imageOf(af);
		Long docId = img != null ? img.getDocId() : parseLong(bizId);
		Long newFileId = fileStore.save(content);
		DocImageFile rec = new DocImageFile();
		rec.setDocId(docId);
		rec.setVersionId(newFileId);
		rec.setFileId(newFileId);
		rec.setFileName(name != null && !name.isBlank() ? name
			: (img != null && img.getFileName() != null ? img.getFileName() : (bizId + ".docx")));
		rec.setFileSize((long) content.length);
		rec.setDocFileType(2);
		imageFileMapper.insert(rec);
		if (docId != null) {
			DocDetail doc = detailMapper.selectById(docId);
			if (doc != null) {
				doc.setMainFileId(newFileId);
				detailMapper.updateById(doc);
			}
		}
		log.info("WPS(doc) 保存新版本 bizId={} -> newFileId={}, size={}", bizId, newFileId, content.length);
		List<WpsFileMeta> chain = versions(bizId);
		return toMeta(WpsFileId.of(domain(), bizId), rec, chain.size());
	}

	@Override
	public void rename(String bizId, String name) {
		DocImageFile img = imageOf(actualFileId(bizId));
		if (img != null) {
			img.setFileName(name);
			imageFileMapper.updateById(img);
		}
	}

	@Override
	public Map<String, Object> permission(String bizId, Map<String, String> userQuery) {
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("read", 1);
		r.put("update", 1);
		r.put("download", 1);
		r.put("rename", 1);
		r.put("history", 1);
		r.put("copy", 1);
		r.put("print", 1);
		r.put("saveas", 0);
		r.put("comment", 1);
		// update=1 必须返回 user_id，且该用户可经 /v3/3rd/users 查到
		r.put("user_id", userQuery.getOrDefault("userId", "0"));
		return r;
	}

	/** 把 DocImageFile 映射成 WPS 协议 8 字段；Controller 可用 {@link WpsCallbackSupport#versionItem} 输出 */
	private WpsFileMeta toMeta(String id, DocImageFile img, int version) {
		WpsFileMeta m = new WpsFileMeta();
		long now = System.currentTimeMillis() / 1000;
		m.setId(id);
		m.setName(img != null && img.getFileName() != null ? img.getFileName() : id + ".docx");
		m.setVersion(version);
		m.setSize(img != null && img.getFileSize() != null ? img.getFileSize() : 0L);
		m.setCreateTime(now);
		m.setModifyTime(now);
		return m;
	}

	private static Long parseLong(String s) {
		try {
			return (s == null || s.isBlank()) ? null : Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
