package org.springblade.workflow.service.helper;

import lombok.RequiredArgsConstructor;
import org.springblade.workflow.entity.WfTask;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 任务业务解析器（【去 wf_ 表 · 批次 1 / 2026-11-10】）。
 *
 * <p>取代 {@code WfTaskServiceImpl#resolveTask()} 的
 * {@code taskMapper.selectById(taskId)}：直接按 {@code BIZ_TASK_ID_} 反查
 * {@code ACT_RU_TASK}（待办）→ {@code ACT_HI_TASKINST}（已办/办结），
 * 并 {@code LEFT JOIN ACT_HI_PROCINST} 补齐实例维度字段。</p>
 *
 * <p><b>双模式入参（顺序不可换）</b>：</p>
 * <ol>
 *   <li><b>模式 A</b>：列表 / 办理页传入的雪花 {@code BIZ_TASK_ID_}；</li>
 *   <li><b>模式 B</b>：消息中心 {@code bizRefId} / 超时通知传入的<b>引擎 Task.getId()</b>
 *       （Flowable 自增整数，如 453214），按 {@code ID_} 反查。
 *       两者无碰撞，先 A 后 B，不用位数启发式。</li>
 * </ol>
 *
 * <p>返回完整 {@link WfTask} 语义对象（不落库）：{@code requireTodoTask} 的 status 判定、
 * {@code doApprove} 的 instId/engineTaskId/nodeKey/assignee、以及
 * {@code WfAuthUtil.requireOperateTask} 的 originalUser 均由本解析器提供。
 * 列表 VO.id 仍吐 {@code BIZ_TASK_ID_}，前端与消息中心<b>零改动</b>。</p>
 */
@Component
@RequiredArgsConstructor
public class WfTaskBizResolver {

	private static final String RU_SQL =
		"SELECT r.ID_ AS engineTaskId, r.BIZ_TASK_ID_ AS bizTaskId, r.TASK_DEF_KEY_ AS nodeKey, "
			+ "COALESCE(r.BIZ_ASSIGNEE_, r.ASSIGNEE_) AS assignee, r.ORIGINAL_USER_ AS originalUser, "
			+ "r.IS_TEST_ AS isTest, r.SIGN_ORDER_ AS signOrder, r.VIEW_TIME_ AS viewTime, "
			+ "r.TIMEOUT_HANDLED_ AS timeoutHandled, r.DUE_DATE_ AS dueTime, "
			+ "r.CREATE_TIME_ AS receiveTime, r.TENANT_ID_ AS tenantId, "
			+ "p.BUSINESS_ID_ AS instId "
			+ "FROM ACT_RU_TASK r LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = r.PROC_INST_ID_ "
			+ "WHERE r.%s = ? LIMIT 1";

	private static final String HI_SQL =
		"SELECT h.ID_ AS engineTaskId, h.BIZ_TASK_ID_ AS bizTaskId, h.TASK_DEF_KEY_ AS nodeKey, "
			+ "COALESCE(h.BIZ_ASSIGNEE_, h.ASSIGNEE_) AS assignee, h.ORIGINAL_USER_ AS originalUser, "
			+ "h.IS_TEST_ AS isTest, h.SIGN_ORDER_ AS signOrder, h.VIEW_TIME_ AS viewTime, "
			+ "h.TIMEOUT_HANDLED_ AS timeoutHandled, h.DUE_DATE_ AS dueTime, "
			+ "h.START_TIME_ AS receiveTime, h.END_TIME_ AS operateTime, "
			+ "h.BUSINESS_STATUS_ AS taskStatus, h.TENANT_ID_ AS tenantId, "
			+ "p.BUSINESS_ID_ AS instId "
			+ "FROM ACT_HI_TASKINST h LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = h.PROC_INST_ID_ "
			+ "WHERE h.%s = ? LIMIT 1";

	private final JdbcTemplate jdbcTemplate;

	/**
	 * 解析任务；未命中返回 {@code null}（由调用方抛「任务不存在」）。
	 */
	public WfTask resolve(Long taskId) {
		if (taskId == null) {
			return null;
		}
		String biz = String.valueOf(taskId);
		WfTask t = first(query(RU_SQL, "BIZ_TASK_ID_", biz, true));
		if (t == null) {
			t = first(query(HI_SQL, "BIZ_TASK_ID_", biz, false));
		}
		if (t == null) {
			// 模式 B：引擎 Task.getId()（消息中心 / 超时通知）
			t = first(query(RU_SQL, "ID_", biz, true));
		}
		if (t == null) {
			t = first(query(HI_SQL, "ID_", biz, false));
		}
		if (t != null && t.getId() == null) {
			t.setId(taskId);
		}
		return t;
	}

	private List<WfTask> query(String template, String whereColumn, Object arg, boolean ru) {
		// whereColumn 仅取本类内两个常量，非外部输入，不存在注入面
		String sql = String.format(template, whereColumn);
		return jdbcTemplate.query(sql, rowMapper(ru), arg);
	}

	private static WfTask first(List<WfTask> rows) {
		return rows == null || rows.isEmpty() ? null : rows.get(0);
	}

	private static RowMapper<WfTask> rowMapper(boolean ru) {
		return (rs, i) -> {
			WfTask t = new WfTask();
			t.setId(getLong(rs, "bizTaskId"));
			t.setEngineTaskId(rs.getString("engineTaskId"));
			t.setNodeKey(rs.getString("nodeKey"));
			t.setAssignee(getLong(rs, "assignee"));
			t.setOriginalUser(getLong(rs, "originalUser"));
			t.setIsTest(getInt(rs, "isTest"));
			t.setSignOrder(getInt(rs, "signOrder"));
			t.setViewTime(rs.getTimestamp("viewTime"));
			t.setTimeoutHandled(getInt(rs, "timeoutHandled"));
			t.setDueTime(rs.getTimestamp("dueTime"));
			t.setReceiveTime(rs.getTimestamp("receiveTime"));
			t.setInstId(getLong(rs, "instId"));
			t.setTenantId(rs.getString("tenantId"));
			if (ru) {
				t.setStatus(WfTask.STATUS_TODO);
			} else {
				t.setStatus(taskStatus(rs.getString("taskStatus")));
				t.setOperateTime(rs.getTimestamp("operateTime"));
			}
			return t;
		};
	}

	private static Integer taskStatus(String s) {
		if (s == null) {
			return WfTask.STATUS_DONE;
		}
		return switch (s) {
			case "TODO" -> WfTask.STATUS_TODO;
			case "DONE" -> WfTask.STATUS_DONE;
			case "FINISHED" -> WfTask.STATUS_FINISHED;
			case "AUTO_SUBMIT" -> WfTask.STATUS_AUTO_SUBMIT;
			case "COADJUTANT" -> WfTask.STATUS_COADJUTANT;
			case "CIRCULATE" -> WfTask.STATUS_CIRCULATE;
			case "READ" -> WfTask.STATUS_READ;
			default -> WfTask.STATUS_DONE;
		};
	}

	private static Long getLong(ResultSet rs, String col) throws SQLException {
		Object v = rs.getObject(col);
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer getInt(ResultSet rs, String col) throws SQLException {
		Object v = rs.getObject(col);
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v));
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
