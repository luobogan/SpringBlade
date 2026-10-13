package org.springblade.workflow.spi;

import lombok.Data;

/** 版式转换结果。 */
@Data
public class OdocConvertResult {

	private boolean success;
	/** 版式文件字节（success 时） */
	private byte[] content;
	private String format;
	private String fileName;
	private String vendor;
	private String message;

	public static OdocConvertResult ok(byte[] content, String format, String fileName, String vendor) {
		OdocConvertResult r = new OdocConvertResult();
		r.setSuccess(true);
		r.setContent(content);
		r.setFormat(format);
		r.setFileName(fileName);
		r.setVendor(vendor);
		return r;
	}

	public static OdocConvertResult fail(String message) {
		OdocConvertResult r = new OdocConvertResult();
		r.setSuccess(false);
		r.setMessage(message);
		return r;
	}
}
