package org.springblade.workflow.odoc.seal;

import lombok.Data;

/**
 * 签署方（用印命令中的签署人/用印单位）。
 *
 * <p>local 模式只用其中签章人相关字段；esign 模式按签署方类型（个人/企业）
 * 取账号与实名信息组装签署流程。</p>
 */
@Data
public class OdocSealSignerCmd {

	/** 签署方类型：0个人 1企业 */
	private Integer signerType;

	/** 签署顺序（从 1 开始） */
	private Integer signOrder;

	/** 账号ID（已有 e签宝账号时传：psnId / orgId） */
	private String accountId;

	/** 签署方名称 */
	private String accountName;

	// —— 企业签署方 ——

	/** 企业名称 */
	private String orgName;

	/** 企业统一社会信用代码 */
	private String orgIdCardNum;

	/** 企业经办人姓名 */
	private String legalRepName;

	/** 企业经办人证件号 */
	private String legalRepIdCardNum;

	// —— 个人签署方 ——

	/** 个人账号（手机号） */
	private String psnAccount;

	/** 个人姓名 */
	private String psnName;

	/** 个人证件号 */
	private String psnIdCardNum;

	/** 是否后台自动落章（esign：true 平台自动盖章，无需签署人到签署页） */
	private Boolean autoSign;

	/** 指定印章ID（esign：assignedSealId） */
	private String sealId;

}
