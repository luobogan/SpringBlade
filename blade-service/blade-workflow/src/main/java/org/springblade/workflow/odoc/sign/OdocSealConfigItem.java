package org.springblade.workflow.odoc.sign;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程设置里的「签章位置/签署方」配置（对应 {@code wf_odoc_process_set.seal_config} 的 JSON）。
 *
 * <p>示例：</p>
 * <pre>
 * {
 *   "page": 1, "offsetX": 120.5, "offsetY": 60.0, "sealType": 2,
 *   "signers": [
 *     { "signerType": 1, "order": 1, "orgName": "XX集团有限公司", "sealId": "SEAL_xxx", "autoSign": true },
 *     { "signerType": 0, "order": 2, "psnAccount": "13800000000", "psnName": "张三", "psnIdCardNum": "..." }
 *   ]
 * }
 * </pre>
 *
 * <p>解析容错：非法 JSON 一律退化为空配置（不抛异常），避免配置脏数据把流程卡死。</p>
 */
@Data
public class OdocSealConfigItem {

	/** 页码（从 1 开始） */
	private Integer page;

	/** 用印位置 X（毫米） */
	private Double offsetX;

	/** 用印位置 Y（毫米） */
	private Double offsetY;

	/** 章型 1个人签字 2单位公章 3时间戳 */
	private Integer sealType;

	/** 签署方（esign 模式必需；local 模式用当前办理人，不需配置） */
	private List<Signer> signers = new ArrayList<>();

	@Data
	public static class Signer {
		/** 签署方类型 0个人 1企业 */
		private Integer signerType;

		/** 签署顺序 */
		private Integer order;

		/** 已有 e签宝账号时传（psnId / orgId） */
		private String accountId;

		/** 个人账号（手机号） */
		private String psnAccount;

		/** 个人姓名 */
		private String psnName;

		/** 个人证件号 */
		private String psnIdCardNum;

		/** 企业名称 */
		private String orgName;

		/** 企业统一社会信用代码 */
		private String orgIdCardNum;

		/** 指定印章ID */
		private String sealId;

		/** 是否后台自动落章 */
		private Boolean autoSign;
	}

	/** 解析签章配置；非法 JSON 返回空配置 */
	public static OdocSealConfigItem parse(String json) {
		if (json == null || json.isBlank()) {
			return new OdocSealConfigItem();
		}
		try {
			ObjectMapper mapper = new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			OdocSealConfigItem item = mapper.readValue(json, OdocSealConfigItem.class);
			return item == null ? new OdocSealConfigItem() : item;
		} catch (Exception e) {
			return new OdocSealConfigItem();
		}
	}

}
