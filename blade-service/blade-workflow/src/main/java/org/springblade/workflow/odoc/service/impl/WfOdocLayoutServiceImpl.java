package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.odoc.entity.WfOdocDocVersion;
import org.springblade.workflow.odoc.entity.WfOdocLayoutFile;
import org.springblade.workflow.odoc.mapper.WfOdocDocVersionMapper;
import org.springblade.workflow.odoc.mapper.WfOdocLayoutFileMapper;
import org.springblade.workflow.odoc.service.IWfOdocLayoutService;
import org.springblade.workflow.odoc.store.IOdocFileStore;
import org.springblade.workflow.spi.IOdocFormatConvertor;
import org.springblade.workflow.spi.OdocConvertResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 版式文件服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfOdocLayoutServiceImpl implements IWfOdocLayoutService {

	private final WfOdocLayoutFileMapper layoutFileMapper;
	private final WfOdocDocVersionMapper versionMapper;
	private final IOdocFileStore fileStore;
	/** 版式转换 SPI 实现集合（来自可选库模块；无则为空 → 降级 L1） */
	private final List<IOdocFormatConvertor> convertors;

	@Override
	public boolean layoutAvailable() {
		return resolveConvertor() != null;
	}

	@Override
	public WfOdocLayoutFile toLayout(Long docId, Long srcVersionId, Integer format) {
		if (format == null || (format != 12 && format != 13)) {
			throw new IllegalArgumentException("版式格式仅支持 12(PDF)/13(OFD)");
		}
		WfOdocDocVersion version = versionMapper.selectById(srcVersionId);
		if (version == null) {
			throw new IllegalArgumentException("来源 docx 版本不存在: " + srcVersionId);
		}
		IOdocFormatConvertor convertor = resolveConvertor();
		if (convertor == null) {
			throw new ServiceException("版式转换未启用：未装配可用转换器（请部署 blade-workflow-odoc-layout 并配置厂商）");
		}
		byte[] docx = fileStore.download(version.getFileId());
		String fmt = String.valueOf(format);
		String fileName = withExt(baseName(version.getFileName()), extOf(format));
		OdocConvertResult r = convertor.toLayout(docx, fmt, fileName);
		if (!r.isSuccess()) {
			throw new ServiceException("版式转换失败: " + r.getMessage());
		}
		Long fileId = fileStore.store(r.getContent(), r.getFileName() != null ? r.getFileName() : fileName);

		WfOdocLayoutFile lf = new WfOdocLayoutFile();
		lf.setDocId(docId);
		lf.setSrcVersionId(srcVersionId);
		lf.setFormat(format);
		lf.setFileId(fileId);
		lf.setVendor(convertor.name());
		lf.setLayoutStage(3);
		lf.setIsFinal(0);
		layoutFileMapper.insert(lf);
		log.info("版式转换完成 docId={} srcVersionId={} format={} vendor={}", docId, srcVersionId, format, convertor.name());
		return lf;
	}

	@Override
	public List<WfOdocLayoutFile> list(Long docId) {
		return layoutFileMapper.selectList(Wrappers.<WfOdocLayoutFile>lambdaQuery()
			.eq(WfOdocLayoutFile::getDocId, docId)
			.orderByDesc(WfOdocLayoutFile::getId));
	}

	@Override
	public byte[] content(Long layoutFileId) {
		WfOdocLayoutFile lf = layoutFileMapper.selectById(layoutFileId);
		if (lf == null) {
			throw new IllegalArgumentException("版式文件不存在: " + layoutFileId);
		}
		return fileStore.download(lf.getFileId());
	}

	@Override
	public String contentName(Long layoutFileId) {
		WfOdocLayoutFile lf = layoutFileMapper.selectById(layoutFileId);
		if (lf == null) {
			return "layout-" + layoutFileId + ".bin";
		}
		String resolved = fileStore.resolveFileName(lf.getFileId());
		if (resolved != null && !resolved.isBlank()) {
			return resolved;
		}
		String ext = lf.getFormat() != null && lf.getFormat() == 13 ? "ofd" : "pdf";
		return "layout-" + layoutFileId + "." + ext;
	}

	private IOdocFormatConvertor resolveConvertor() {
		if (convertors == null || convertors.isEmpty()) {
			return null;
		}
		return convertors.stream().filter(IOdocFormatConvertor::available).findFirst().orElse(null);
	}

	private String baseName(String name) {
		if (name == null) {
			return "odoc";
		}
		int i = name.lastIndexOf('.');
		return i > 0 ? name.substring(0, i) : name;
	}

	private String extOf(Integer format) {
		return format == 13 ? "ofd" : "pdf";
	}

	private String withExt(String base, String ext) {
		return base + "." + ext;
	}
}
