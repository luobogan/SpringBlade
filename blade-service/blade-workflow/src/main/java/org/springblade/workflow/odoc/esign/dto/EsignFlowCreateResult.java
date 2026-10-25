package org.springblade.workflow.odoc.esign.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建签署流程的返回结果。
 */
@Data
public class EsignFlowCreateResult {

	/** e签宝签署流程ID */
	private String flowId;

	/** e签宝文件ID */
	private String fileId;

	/** 各签署方的签署链接 */
	private List<SignerUrl> signerUrls = new ArrayList<>();

	@Data
	public static class SignerUrl {
		/** 签署方账号ID */
		private String accountId;
		/** 签署方名称 */
		private String accountName;
		/** 签署链接 */
		private String signUrl;
		/** 签署顺序 */
		private Integer signOrder;
	}
}
