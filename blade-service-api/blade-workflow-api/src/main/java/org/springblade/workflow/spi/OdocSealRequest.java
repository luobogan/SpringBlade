package org.springblade.workflow.spi;

import lombok.Data;

/** 签章请求。 */
@Data
public class OdocSealRequest {

	/** 章型 1个人签字 2单位公章 3时间戳 */
	private Integer sealType;
	/** 页码 */
	private Integer page;
	/** 横坐标偏移（mm） */
	private Double offsetX;
	/** 纵坐标偏移（mm） */
	private Double offsetY;
	/** 签章人ID */
	private Long signerId;
	/** 签章人姓名 */
	private String signerName;
}
