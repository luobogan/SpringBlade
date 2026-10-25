package org.springblade.workflow.odoc.seal;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 用印命令（屏蔽 local / esign 两种模式的差异）。
 *
 * <p>坐标口径：{@code offsetX/offsetY} 为<b>毫米</b>（与版式文件坐标一致，
 * 供 local CA 签章使用）；e签宝签署区坐标为<b>像素</b>，由 esign 策略按 96dpi 换算，
 * 若需精确控制可直接给 {@code positionX/positionY}（像素），此时忽略毫米换算。</p>
 */
@Data
public class OdocSealCmd {

	/** 公文正文ID */
	private Long docId;

	/** 流程实例ID（esign 模式落库必填） */
	private Long instId;

	/** 被用印的版式文件ID（local 模式必填；esign 模式为空则取正文版本文件） */
	private Long layoutFileId;

	/** 正文版本ID（esign 模式上传原文用；归档后原版本仍保留） */
	private Long srcVersionId;

	/** 用印标题 */
	private String title;

	/** 业务流水号（幂等键：同一 bizNum 只会发起一次） */
	private String bizNum;

	/** 印章ID（esign 模式为 e签宝 sealId，为空取配置默认值；local 模式可空） */
	private String sealId;

	/** 章型 1个人签字 2单位公章 3时间戳（local 模式） */
	private Integer sealType;

	/** 页码（从 1 开始） */
	private Integer page;

	/** 用印位置 X（毫米） */
	private Double offsetX;

	/** 用印位置 Y（毫米） */
	private Double offsetY;

	/** 用印位置 X（像素，esign 签署区；给出则优先于毫米换算） */
	private Integer positionX;

	/** 用印位置 Y（像素，esign 签署区） */
	private Integer positionY;

	/** 是否自动落章（esign 模式；为空取配置 odoc.esign.sign.auto-sign-platform） */
	private Boolean autoSign;

	/** 签章人ID（local 模式） */
	private Long signerId;

	/** 签章人姓名（local 模式） */
	private String signerName;

	/** 签署方列表（esign 模式必填） */
	private List<OdocSealSignerCmd> signers = new ArrayList<>();

}
