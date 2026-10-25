package org.springblade.workflow.odoc.esign.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建 e签宝签署流程的入参（对齐 {@code POST /v3/sign-flow/create-by-file}）。
 */
@Data
public class EsignCreateFlowCmd {

	/** 公文正文ID */
	private Long docId;

	/** 发起签署时的正文版本ID（原文版本，归档后仍保留） */
	private Long srcVersionId;

	/** 签署标题 */
	private String title;

	/** 业务流水号（customBizNum），发起幂等键 */
	private String bizNum;

	/** 用印方式：0自研 1e签宝 */
	private Integer sealMode;

	/** 计费模式（取配置 odoc.esign.sign.charge-mode） */
	private Integer chargeMode;

	/** 通知方式（取配置） */
	private String noticeTypes;

	/** 待签文件（e签宝 fileId，由 uploadFile 返回后填入） */
	private List<Doc> docs = new ArrayList<>();

	private List<SignerCmd> signers = new ArrayList<>();

	@Data
	public static class Doc {
		/** e签宝文件ID */
		private String fileId;
		/** 文件名 */
		private String fileName;
	}

	/** 签署方 */
	@Data
	public static class SignerCmd {
		/** 0个人 1企业 */
		private Integer signerType;
		/** 签署顺序（signOrder） */
		private Integer signOrder;
		/** e签宝账号ID（已有账号时传） */
		private String accountId;

		// —— 企业签署方 ——
		private String orgName;
		/** 统一社会信用代码 */
		private String orgIdCardNum;
		/** 证件类型，默认 CRED_ORG_USCC */
		private String orgIdCardType = "CRED_ORG_USCC";
		private String legalRepName;
		private String legalRepIdCardNum;

		// —— 个人签署方 / 企业经办人 ——
		private String psnAccount;
		private String psnName;
		private String psnIdCardNum;

		/** 是否后台自动落章 */
		private Boolean autoSign;
		/** 指定印章ID（assignedSealId） */
		private String sealId;
		/** 通知方式 */
		private String noticeTypes;

		/** 签署区（用印位置） */
		private List<SignFieldCmd> signFields = new ArrayList<>();
	}

	/** 签署区（用印位置） */
	@Data
	public static class SignFieldCmd {
		/** 所属文件ID */
		private String fileId;
		/** 业务流水号 */
		private String customBizNum;
		/** 页码 */
		private String positionPage = "1";
		/** X 坐标 */
		private Integer positionX;
		/** Y 坐标 */
		private Integer positionY;
		/** 签署区类型，默认 0 */
		private Integer signFieldType = 0;
		/** 签署区样式，默认 1 */
		private Integer signFieldStyle = 1;
		/** 是否允许自由拖动 */
		private Boolean freeMode = false;
		/** 是否可移动签署区 */
		private Boolean movableSignField = true;
		/** 指定印章ID */
		private String assignedSealId;
		/** 是否自动落章 */
		private Boolean autoSign;
	}
}
