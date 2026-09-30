package org.springblade.workflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.entity.WfApprovalLog;
import org.springblade.workflow.service.helper.WfApprovalLogActReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P6 读侧校验（《去 wf_ 表改造分析》P6 项）：读源=act 时 {@link WfApprovalLogActReader#readFromAct}
 * 从 {@code ACT_HI_COMMENT} 还原审批日志，并按「logType+nodeKey+operator+opinion+时间±2s」
 * 兜底合并 {@code wf_approval_log} 中 ACT 写不进的缺口行（归档/挂起意见）。
 *
 * <p>本测试用内存 H2 实建 {@code ACT_HI_COMMENT} / {@code wf_approval_log} 两张表，直接驱动 reader，
 * 验证 P6「停写 wf_approval_log（仅留缺口填充）」后读侧仍能还原<b>完整</b>审批轨迹，且双写期
 * 的重复行被正确去重。</p>
 */
class WfApprovalLogActReaderTest {

	private DataSource dataSource;
	private JdbcTemplate jdbc;
	private WfApprovalLogActReader reader;

	@BeforeEach
	void setUp() {
		SimpleDriverDataSource ds = new SimpleDriverDataSource();
		ds.setDriverClass(org.h2.Driver.class);
		ds.setUrl("jdbc:h2:mem:actreader;DB_CLOSE_DELAY=-1");
		ds.setUsername("sa");
		ds.setPassword("");
		this.dataSource = ds;
		this.jdbc = new JdbcTemplate(ds);
		this.reader = new WfApprovalLogActReader(jdbc);
		jdbc.execute("CREATE TABLE ACT_HI_COMMENT ("
			+ "ID_ BIGINT, TYPE_ VARCHAR(64), TIME_ TIMESTAMP, MESSAGE_ VARCHAR(2000), PROC_INST_ID_ VARCHAR(64))");
		jdbc.execute("CREATE TABLE wf_approval_log ("
			+ "id BIGINT, task_id BIGINT, node_key VARCHAR(64), operator BIGINT, "
			+ "log_type VARCHAR(64), opinion VARCHAR(1000), operate_time TIMESTAMP, inst_id BIGINT)");
	}

	@AfterEach
	void tearDown() {
		jdbc.execute("DROP TABLE IF EXISTS ACT_HI_COMMENT");
		jdbc.execute("DROP TABLE IF EXISTS wf_approval_log");
	}

	/** ACT 意见 JSON 形态（与 WfWriteHelper#syncCommentToEngine 写入一致） */
	private String commentJson(String nodeKey, long operator, long wfTaskId, String opinion) {
		return String.format(
			"{\"nodeKey\":\"%s\",\"operator\":%d,\"wfTaskId\":%d,\"opinion\":\"%s\",\"ts\":%d}",
			nodeKey, operator, wfTaskId, opinion, System.currentTimeMillis());
	}

	@Test
	@DisplayName("P6-读完整: ACT 有 3 条正常意见 + wf 仅 1 条归档缺口 → 合并后共 4 条，归档不丢")
	void readFromAct_mergesGapFiller_completeness() {
		String eng = "eng-1";
		long wfInst = 1L;
		Timestamp now = Timestamp.valueOf(LocalDateTime.now());

		// ACT：3 条正常审批（P6 停写后这些只落在 ACT）
		jdbc.update("INSERT INTO ACT_HI_COMMENT VALUES (1,'approve',?,?,?)",
			now, commentJson("miApprove", 1001, 11, "同意A"), eng);
		jdbc.update("INSERT INTO ACT_HI_COMMENT VALUES (2,'approve',?,?,?)",
			now, commentJson("miApprove", 1002, 12, "同意B"), eng);
		jdbc.update("INSERT INTO ACT_HI_COMMENT VALUES (3,'approve',?,?,?)",
			now, commentJson("miApprove", 1003, 13, "同意C"), eng);
		// wf：仅 1 条归档缺口（AddCommentCmd 写不进 ACT，留作兜底填充）
		jdbc.update("INSERT INTO wf_approval_log VALUES (900,null,'',1001,'supervise','归档意见',?,?)",
			now, wfInst);

		List<WfApprovalLog> logs = reader.readFromAct(eng, wfInst);

		assertThat(logs).hasSize(4);
		assertThat(logs).anyMatch(l -> "归档意见".equals(l.getOpinion())
			&& "supervise".equals(l.getLogType()));
		assertThat(logs.stream().filter(l -> "同意A".equals(l.getOpinion())).count()).isEqualTo(1);
	}

	@Test
	@DisplayName("P6-去重: 同一意见同时存在于 ACT 与 wf（双写期残留）→ 合并后不重复")
	void readFromAct_dedup_dualWrittenRows() {
		String eng = "eng-2";
		long wfInst = 2L;
		Timestamp now = Timestamp.valueOf(LocalDateTime.now());

		jdbc.update("INSERT INTO ACT_HI_COMMENT VALUES (10,'approve',?,?,?)",
			now, commentJson("miApprove", 1001, 21, "同意X"), eng);
		// 双写期：同一条意见也写进了 wf（时间同源 ±2s）→ 应被去重
		jdbc.update("INSERT INTO wf_approval_log VALUES (910,21,'miApprove',1001,'approve','同意X',?,?)",
			now, wfInst);

		List<WfApprovalLog> logs = reader.readFromAct(eng, wfInst);

		assertThat(logs.stream().filter(l -> "同意X".equals(l.getOpinion())).count()).isEqualTo(1);
	}

	@Test
	@DisplayName("P6-空实例: engineInstId 为空 → 返回空列表（不查库、不报错）")
	void readFromAct_nullEngineInst_returnsEmpty() {
		assertThat(reader.readFromAct(null, 3L)).isEmpty();
	}

	@Test
	@DisplayName("P6-解析防御: 非本模块写入/格式异常的 MESSAGE_ → 跳过，不污染流转意见")
	void parseActComment_nonJsonOrNull_returnsNull() {
		assertThat(WfApprovalLogActReader.parseActComment(null, "approve", new java.util.Date())).isNull();
		assertThat(WfApprovalLogActReader.parseActComment("plain text", "approve", new java.util.Date())).isNull();
		WfApprovalLog parsed = WfApprovalLogActReader.parseActComment(
			commentJson("miApprove", 1001, 31, "OK"), "approve", new java.util.Date());
		assertThat(parsed).isNotNull();
		assertThat(parsed.getNodeKey()).isEqualTo("miApprove");
		assertThat(parsed.getOperator()).isEqualTo(1001L);
		assertThat(parsed.getOpinion()).isEqualTo("OK");
	}
}
