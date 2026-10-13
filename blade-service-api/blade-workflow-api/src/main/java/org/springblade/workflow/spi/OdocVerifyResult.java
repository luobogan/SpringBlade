package org.springblade.workflow.spi;

import lombok.Data;

import java.time.LocalDateTime;

/** 验章结果。 */
@Data
public class OdocVerifyResult {

	/** 签章是否有效 */
	private boolean valid;
	/** 是否检测到篡改 */
	private boolean tampered;
	private String certSn;
	private LocalDateTime signTime;
	private String message;
}
