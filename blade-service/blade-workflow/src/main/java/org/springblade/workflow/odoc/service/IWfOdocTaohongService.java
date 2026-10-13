package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.entity.WfOdocDocVersion;
import org.springblade.workflow.odoc.vo.OdocTaohongInfoVO;

import java.util.Map;

/**
 * 公文套红服务。
 *
 * <p>套红：用套红模板（含红头+书签）承载正文，按书签→字段映射填入表单值，
 * 在正文占位书签处插入正文，产出套红版 docx 并作为新版本落库（hasUsedTemplet=1）。
 * 取消套红：回退到最近一个未套红版本。</p>
 */
public interface IWfOdocTaohongService {

	/**
	 * 执行套红。
	 *
	 * @param docId       正文ID
	 * @param mouldId     套红模板ID（mouldType=2）
	 * @param fieldValues 表单字段值（键=表单字段名），用于填充书签
	 * @return 套红版新版本
	 */
	WfOdocDocVersion taohong(Long docId, Long mouldId, Map<String, Object> fieldValues);

	/** 取消套红（回退到最近未套红版本） */
	WfOdocDocVersion cancelTaohong(Long docId);

	/** 套红状态 + 可用模板 + 是否强制套红 */
	OdocTaohongInfoVO taohongInfo(Long docId);
}
