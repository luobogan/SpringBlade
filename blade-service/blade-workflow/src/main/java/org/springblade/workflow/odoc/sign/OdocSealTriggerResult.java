package org.springblade.workflow.odoc.sign;

import lombok.Data;

/**
 * 用印触发结果（节点提交后发起用印的结果，用于决定流程是否推进）。
 *
 * <p>语义（对应「签完才流转」策略）：</p>
 * <ul>
 *   <li>{@code triggered=false}：本次节点不涉及用印 → 流程照常推进；</li>
 *   <li>{@code waiting=true}：第三方签章已发起、需等待签署完成 → <b>本次提交不推进引擎</b>，
 *       待回调/补偿确认签署完成后由 {@code OdocSealAdvancer} 推进；</li>
 *   <li>{@code waiting=false} 且 {@code triggered=true}：自研印章同步盖章完成 → 流程照常推进。</li>
 * </ul>
 */
@Data
public class OdocSealTriggerResult {

	/** 是否命中用印节点（含「已发起过、幂等复用」的中间态） */
	private boolean triggered;

	/** 是否需要等待第三方签署（true=本次提交不推进引擎） */
	private boolean waiting;

	/** e签宝签署流程ID */
	private String esignFlowId;

	/** 本地签署流程ID（wf_odoc_sign_flow.id） */
	private Long signFlowId;

	/** 提示信息 */
	private String message;

	/** 未触发用印 */
	public static OdocSealTriggerResult none() {
		OdocSealTriggerResult r = new OdocSealTriggerResult();
		r.setTriggered(false);
		r.setWaiting(false);
		return r;
	}

}
