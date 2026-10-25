package org.springblade.workflow.odoc.esign.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 签署流程详情（补偿查询 {@code GET /v3/sign-flow/{flowId}/detail} 的简化视图）。
 */
@Data
public class EsignFlowDetail {

	/** e签宝签署流程ID */
	private String flowId;

	/** 流程状态（e签宝原值） */
	private Integer flowStatus;

	/** 流程标题 */
	private String flowTitle;

	/** 是否已全部完成 */
	private boolean finished;

	/** 签署方状态明细 */
	private List<SignerStatus> signers = new ArrayList<>();

	@Data
	public static class SignerStatus {
		private String accountId;
		private String accountName;
		/** e签宝 signResult */
		private Integer signResult;
		/** 是否已签 */
		private boolean signed;
		private String signTime;
	}
}
