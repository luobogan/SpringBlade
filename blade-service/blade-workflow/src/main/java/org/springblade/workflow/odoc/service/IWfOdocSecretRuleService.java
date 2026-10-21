package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.entity.WfOdocSecretRule;

import java.util.List;

/**
 * 密级收发限制规则服务（对应 E9 SENDREUNITRESTRICTIONS / CommReceiveGroup）。
 */
public interface IWfOdocSecretRuleService {

	/** 全部规则（按 id 升序） */
	List<WfOdocSecretRule> list();

	/** 保存（有 id 更新，无 id 新增；校验 from<=to） */
	WfOdocSecretRule save(WfOdocSecretRule rule);

	/** 删除 */
	boolean remove(Long id);

	/**
	 * 校验收文单位密级：公文密级 docSecretLevel 是否被各收文单位(单位 id 串)的限制规则允许。
	 *
	 * @return null=通过（或无规则/无法判定）；否则返回拒绝原因（含越限单位名）
	 */
	String violationMessage(String docSecretLevel, String receiveUnitIdsCsv);

	/**
	 * 正向过滤：返回「给定公文密级下允许收发的收文单位 id 集合」。
	 *
	 * <p>语义与 {@link #violationMessage} 一致（单位规则 / 公共组规则从严），但反向输出——
	 * 供前端发文选主送/抄送单位时按密级前置过滤候选。密级名无法判定（null/不识别）或无启用
	 * 规则时返回全部单位 id（不限制）。</p>
	 *
	 * @param docSecretLevel 公文密级名（公开/秘密/机密/绝密，对齐 wf_odoc_dict secret_level）
	 * @return 允许的单位 id 集合
	 */
	List<Long> allowedUnitIds(String docSecretLevel);
}
