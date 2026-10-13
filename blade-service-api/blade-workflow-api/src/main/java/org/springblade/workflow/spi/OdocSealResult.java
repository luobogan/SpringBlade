package org.springblade.workflow.spi;

import lombok.Data;

/** 签章结果。 */
@Data
public class OdocSealResult {

	private boolean success;
	/** 签章后文件字节 */
	private byte[] content;
	/** 厂商侧签章/合同ID */
	private String vendorSealId;
	/** 签章证书序列号 */
	private String certSn;
	/** 签名算法 RSA/SM2 */
	private String algorithm;
	/** RFC3161 时间戳令牌 */
	private String tsaToken;
	private String message;

	public static OdocSealResult ok(byte[] content, String vendorSealId, String certSn,
									String algorithm, String tsaToken) {
		OdocSealResult r = new OdocSealResult();
		r.setSuccess(true);
		r.setContent(content);
		r.setVendorSealId(vendorSealId);
		r.setCertSn(certSn);
		r.setAlgorithm(algorithm);
		r.setTsaToken(tsaToken);
		return r;
	}

	public static OdocSealResult fail(String message) {
		OdocSealResult r = new OdocSealResult();
		r.setSuccess(false);
		r.setMessage(message);
		return r;
	}
}
