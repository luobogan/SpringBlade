package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.entity.WfOdocLayoutFile;

import java.util.List;

/**
 * 版式文件服务（L2，经 {@code IOdocFormatConvertor} SPI；未装配则降级）。
 */
public interface IWfOdocLayoutService {

	/** 是否有可用版式转换器（前端据此显示/隐藏「转版式」入口） */
	boolean layoutAvailable();

	/** docx 版本 → 版式文件（12PDF/13OFD），落库 WfOdocLayoutFile */
	WfOdocLayoutFile toLayout(Long docId, Long srcVersionId, Integer format);

	List<WfOdocLayoutFile> list(Long docId);

	/** 版式文件字节（预览/下载） */
	byte[] content(Long layoutFileId);

	/** 版式文件下载名（含扩展名，按 format 12=pdf/13=ofd），供 Content-Disposition */
	String contentName(Long layoutFileId);
}
