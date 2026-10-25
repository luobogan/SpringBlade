package org.springblade.workflow.odoc.esign.dto;

import lombok.Data;

/** 印章选项（封装 e签宝印章与自研印章，供上层统一使用） */
@Data
public class EsignSeal {

	/** 印章ID：e签宝 sealId 或自研印章ID */
	private String sealId;

	/** 印章名称 */
	private String sealName;

	/** 归属机构ID（个人印章为空） */
	private String orgId;

	/** 印章类型（由 e签宝返回，自研印章可为空） */
	private String sealType;

	/** 是否为默认印章 */
	private boolean defaultSeal;
}
