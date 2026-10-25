package org.springblade.workflow.odoc.seal;

import lombok.Data;

/**
 * 用印结果（屏蔽 local / esign 两种模式的差异）。
 *
 * <p>{@link #isFinished()} 表达本次用印是否已产出签章件：</p>
 * <ul>
 *   <li>local 模式：同步完成，{@code finished=true}，{@code sealFileId} 为签章件文件ID；</li>
 *   <li>esign 模式：发起即返回，{@code finished=false}，签章件在签署完成后由回调/补偿
 *       取回归档并回写 {@code sealFileId} 与本地签署流程记录的 {@code archiveVersionId}。</li>
 * </ul>
 */
@Data
public class OdocSealOutcome {

	/** 用印模式：local / esign */
	private String mode;

	/** 是否成功发起 */
	private boolean success;

	/** 本次用印是否已完成（已产出签章件） */
	private boolean finished;

	/** 签章件文件ID（local 模式即时返回；esign 模式签署完成后回写） */
	private Long sealFileId;

	/** 归档后的公文版本ID（esign 模式签署完成后回写） */
	private Long archiveVersionId;

	/** 本地签章记录ID（local 模式对应 wf_odoc_seal.id） */
	private Long sealRecordId;

	/** e签宝签署流程ID */
	private String esignFlowId;

	/** 本地签署流程记录ID（esign 模式对应 wf_odoc_sign_flow.id） */
	private Long signFlowId;

	/** 签署链接（esign 模式，供签署人跳转签署） */
	private String signUrl;

	/** 处理说明 */
	private String message;

	public static OdocSealOutcome fail(String mode, String message) {
		OdocSealOutcome o = new OdocSealOutcome();
		o.setMode(mode);
		o.setSuccess(false);
		o.setFinished(false);
		o.setMessage(message);
		return o;
	}

}
