package org.springblade.workflow.odoc.seal;

import lombok.Data;

/**
 * 用印选项（印章下拉项，屏蔽 local / esign 两种模式的差异）。
 */
@Data
public class OdocSealOption {

	/** 印章ID：esign 模式为 e签宝 sealId；local 模式为空（按章型由 CA 决定） */
	private String sealId;

	/** 印章名称 */
	private String sealName;

	/** 印章类型：esign 模式为 e签宝返回的类型；local 模式为章型 1个人签字 2单位公章 3时间戳 */
	private String sealType;

	/** 归属机构ID（个人印章为空） */
	private String orgId;

	/** 是否为默认印章 */
	private boolean defaultSeal;

	/** 来源模式：local / esign */
	private String mode;

}
