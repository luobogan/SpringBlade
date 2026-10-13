package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.entity.WfOdocSeal;
import org.springblade.workflow.spi.OdocSealRequest;

import java.util.List;

/**
 * 电子签章服务（L2，经 {@code IOdocSealService} SPI；未装配则降级）。
 */
public interface IWfOdocSealService {

	/** 是否有可用签章服务（前端据此显示/隐藏签章入口） */
	boolean sealAvailable();

	/** 对版式文件签章，落库 WfOdocSeal */
	WfOdocSeal sign(Long docId, Long layoutFileId, OdocSealRequest request);

	/** 验章 + 篡改检测，回写记录 */
	WfOdocSeal verify(Long sealId);

	List<WfOdocSeal> list(Long docId);
}
