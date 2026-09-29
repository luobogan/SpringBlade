package org.springblade.workflow.service.impl;

import org.junit.jupiter.api.Test;
import org.springblade.workflow.entity.WfApprovalLog;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link WfInstanceServiceImpl#parseActComment} 对称单测：验证 ACT_HI_COMMENT.MESSAGE_ 中的
 * 审批日志 JSON（由 {@code WfWriteHelper.syncCommentToEngine} 写入，键 nodeKey/operator/wfTaskId/opinion/ts）
 * 能被正确还原为 {@link WfApprovalLog} 维度（nodeKey/operator/opinion/logType/operateTime）。
 *
 * <p>这是「logs 读源迁 ACT」最关键、最易错位的新逻辑：写侧与读侧必须同源同一 JSON 契约，
 * 否则 nodeKey/operator 维度丢失会导致流转意见「可见节点过滤 / 下一节点接收人」错乱。</p>
 */
class ActCommentParserTest {

	private static final Date TS = new Date(1_700_000_000_000L);

	/** 与 syncCommentToEngine 同一形态：nodeKey/operator/wfTaskId/opinion/ts */
	private static final String VALID_JSON = "{"
		+ "\"nodeKey\":\"approve\",\"operator\":1001,\"wfTaskId\":55,"
		+ "\"opinion\":\"同意\",\"ts\":1700000000000}";

	@Test
	void parsesValidCommentPayload() {
		WfApprovalLog l = WfInstanceServiceImpl.parseActComment(VALID_JSON, "0", TS);
		assertThat(l).isNotNull();
		assertThat(l.getNodeKey()).isEqualTo("approve");
		assertThat(l.getOperator()).isEqualTo(1001L);
		assertThat(l.getTaskId()).isEqualTo(55L);
		assertThat(l.getOpinion()).isEqualTo("同意");
		assertThat(l.getLogType()).isEqualTo("0");
		assertThat(l.getOperateTime()).isEqualTo(TS);
		assertThat(l.getId()).isNull();
	}

	@Test
	void returnsNullForNonJsonMessage() {
		// Flowable 原生评论（非本模块写入）或空串：跳过，不污染流转意见
		assertThat(WfInstanceServiceImpl.parseActComment("just a plain text comment", "0", TS)).isNull();
		assertThat(WfInstanceServiceImpl.parseActComment("", "0", TS)).isNull();
		assertThat(WfInstanceServiceImpl.parseActComment(null, "0", TS)).isNull();
	}

	@Test
	void toleratesMissingOptionalFields() {
		// nodeKey 为空串、opinion 缺失：不应抛异常，维度降级
		String json = "{\"nodeKey\":\"\",\"operator\":0,\"wfTaskId\":0,\"ts\":1700000000000}";
		WfApprovalLog l = WfInstanceServiceImpl.parseActComment(json, "s", TS);
		assertThat(l).isNotNull();
		assertThat(l.getNodeKey()).isEqualTo("");
		assertThat(l.getOperator()).isEqualTo(0L);
		assertThat(l.getOpinion()).isEqualTo("");
		assertThat(l.getLogType()).isEqualTo("s");
	}
}
