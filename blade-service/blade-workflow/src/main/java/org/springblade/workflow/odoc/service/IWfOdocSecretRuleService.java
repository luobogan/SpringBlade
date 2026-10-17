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
}
