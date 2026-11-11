package org.springblade.workflow.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.service.helper.TaskBizRecord;
import org.springblade.workflow.service.helper.WfTaskActDirectWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * `task-ledger-mode=legacy` 回退后的 ACT 业务列重建器（幂等、手动触发）。
 *
 * <p>回退到 legacy 期间新产生的 {@code wf_task} 行不会自动投影到 ACT
 * （旧 {@code sync} 仍会写，但历史/异常窗口可能留下缺口）。本组件从存量台账行
 * 重建 {@code ACT_RU_TASK}/{@code ACT_HI_TASKINST} 的 10 个业务列，
 * 供运维在回退窗口后一次性补齐；重复执行结果一致。</p>
 *
 * <p><b>不自动运行</b>：仅在明确调用 {@link #rebuild()} 时执行，避免启动期
 * 对已零写入的库做无谓扫描。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfTaskActRebuilder {

	private final JdbcTemplate jdbcTemplate;
	private final WfTaskActDirectWriter directWriter;

	/**
	 * @return 成功重建的行数
	 */
	public int rebuild() {
		List<TaskBizRecord> rows = jdbcTemplate.query(
			"SELECT * FROM wf_task WHERE engine_task_id IS NOT NULL AND engine_task_id <> ''",
			(rs, i) -> toRecord(rs));
		int n = 0;
		for (TaskBizRecord rec : rows) {
			if (rec.getEngineTaskId() == null) {
				continue;
			}
			directWriter.writeFull(rec);
			n++;
		}
		log.info("[WfTaskActRebuilder] 从存量 wf_task 重建 ACT 业务列完成, rows={}", n);
		return n;
	}

	private static TaskBizRecord toRecord(ResultSet rs) throws SQLException {
		TaskBizRecord rec = new TaskBizRecord();
		rec.setBizTaskId(getLong(rs, "id"));
		rec.setEngineTaskId(rs.getString("engine_task_id"));
		rec.setInstId(getLong(rs, "inst_id"));
		rec.setNodeKey(rs.getString("node_key"));
		rec.setAssignee(getLong(rs, "assignee"));
		rec.setOriginalUser(getLong(rs, "original_user"));
		rec.setStatus(getInt(rs, "status"));
		rec.setIsTest(getInt(rs, "is_test"));
		rec.setSignOrder(getInt(rs, "sign_order"));
		rec.setViewTime(rs.getTimestamp("view_time"));
		rec.setTimeoutHandled(getInt(rs, "timeout_handled"));
		rec.setDueTime(rs.getTimestamp("due_time"));
		rec.setReceiveTime(rs.getTimestamp("receive_time"));
		rec.setOperateTime(rs.getTimestamp("operate_time"));
		rec.setTenantId(rs.getString("tenant_id"));
		return rec;
	}

	private static Long getLong(ResultSet rs, String col) throws SQLException {
		try {
			Object v = rs.getObject(col);
			return v == null ? null : ((Number) v).longValue();
		} catch (SQLException e) {
			return null;
		}
	}

	private static Integer getInt(ResultSet rs, String col) throws SQLException {
		try {
			Object v = rs.getObject(col);
			return v == null ? null : ((Number) v).intValue();
		} catch (SQLException e) {
			return null;
		}
	}
}
