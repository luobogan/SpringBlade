package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.gbt.Gbt9704Profile;
import org.springblade.workflow.odoc.vo.OdocLayoutRuleVO;
import org.springblade.workflow.odoc.vo.OdocSampleVO;
import org.springblade.workflow.odoc.vo.OdocSensitiveHitVO;

import java.util.List;

/**
 * 公文 AI 增强（L1）。
 *
 * <p>L1 为「规则/内置库」层，不依赖外部大模型，开箱可用：</p>
 * <ul>
 *   <li>智能排版：基于 GB/T 9704 规则库给出版式（页面/边距/要素字体字号坐标）。</li>
 *   <li>范文：按文种给出公文结构示例。</li>
 *   <li>敏感词：扫描正文命中项（内置中性词库，可扩展）。</li>
 * </ul>
 * L2/L3（结构识别、合规校验、大模型改写）在此接口上演进，不动调用方。
 */
public interface IWfOdocAiService {

	/** 按文种取 GB/T 9704 版式（智能排版结果） */
	Gbt9704Profile suggestLayout(String docTypeCode);

	/** 版式规则展平（可读列表，供前端展示排版建议） */
	List<OdocLayoutRuleVO> layoutRules(String docTypeCode);

	/** 范文列表 */
	List<OdocSampleVO> samples(String docTypeCode);

	/** 敏感词扫描 */
	List<OdocSensitiveHitVO> checkSensitive(String text);
}
