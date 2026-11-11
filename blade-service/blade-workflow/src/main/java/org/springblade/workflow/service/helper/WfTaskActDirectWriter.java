package org.springblade.workflow.service.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.utils.WfAuthUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * ACT 任务业务列直写器（【去 wf_ 表 · 批次 1 / 2026-11-10】）。
 *
 * <p>取代 {@link WfTaskActWriter#sync(WfTask)} 的「先落 wf_task 行、再投影到 ACT」链路：
 * 由业务侧直接把 {@link TaskBizRecord} 写入 {@code ACT_RU_TASK}/{@code ACT_HI_TASKINST}
 * 的 10 个自定义业务列。这样 {@code wf_task} 不再产生任何新增行，但列表 VO 的
 * {@code BIZ_TASK_ID_} 契约与操作链 id 语义保持不变。</p>
 *
 * <p><b>按 D18 不含任何 N:1 守卫统计</b>：{@code wf_ext_task} 行
 * （{@code WfExtTask extends TenantEntity}，非 {@code WfTask} 子类）从不调用本写入器，
 * 不会踩踏 ACT 单行；旧守卫把扩展行计入 {@code siblings} 已由
 * {@code WfCirculateDriftReproTest} 证实会造成「抄送后审批」状态漂移。</p>
 *
 * <p><b>HI 先 / RU 后不可反</b>：{@code history=audit} 下 HI 行创建即落库、RU 行完成即删除。
 * 两处 UPDATE 的异常沿用旧写入器的容错级别（{@code catch → log.warn}，不抛，
 * 不因双写预热阻断主流程）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfTaskActDirectWriter {

	/** 允许动态拼 SET 的列白名单（防注入，也避免误写引擎原生列） */
	private static final Set<String> ALLOWED_COLUMNS = Set.of(
		"BUSINESS_STATUS_", "IS_TEST_", "ORIGINAL_USER_", "SIGN_ORDER_",
		"VIEW_TIME_", "TIMEOUT_HANDLED_", "DUE_DATE_",
		"BIZ_TASK_ID_", "BIZ_ASSIGNEE_", "TENANT_ID_");

	private final JdbcTemplate jdbcTemplate;

	/** 全量写入：任务创建时调用，10 列一次写齐 */
	public void writeFull(TaskBizRecord rec) {
		if (rec == null || rec.getEngineTaskId() == null) {
			return;
		}
		LinkedHashMap<String, Object> cols = new LinkedHashMap<>();
		cols.put("BUSINESS_STATUS_", statusStr(rec.getStatus()));
		cols.put("IS_TEST_", rec.getIsTest());
		cols.put("ORIGINAL_USER_", rec.getOriginalUser() == null ? null : String.valueOf(rec.getOriginalUser()));
		cols.put("SIGN_ORDER_", rec.getSignOrder());
		cols.put("VIEW_TIME_", rec.getViewTime());
		cols.put("TIMEOUT_HANDLED_", rec.getTimeoutHandled());
		cols.put("DUE_DATE_", rec.getDueTime());
		cols.put("BIZ_TASK_ID_", rec.getBizTaskId());
		cols.put("BIZ_ASSIGNEE_", rec.getAssignee());
		cols.put("TENANT_ID_", resolveTenant(rec.getTenantId()));
		writePartial(rec.getEngineTaskId(), cols);
	}

	/**
	 * 局部写入：状态变更 / 查看 / 超时处置等只更新指定列，
	 * 避免「读回全量再覆盖」的并发踩踏。
	 *
	 * @param cols 列名→值；列名必须落在 {@link #ALLOWED_COLUMNS} 白名单内
	 */
	public void writePartial(String engineTaskId, LinkedHashMap<String, Object> cols) {
		writePartial(jdbcTemplate, engineTaskId, cols);
	}

	/**
	 * 条件反写状态：仅当当前 {@code BUSINESS_STATUS_} 为待办（{@code 'TODO'}）或 {@code NULL} 时，
	 * 才改为 {@code newStatus}。
	 *
	 * <p><b>为什么需要条件式</b>：direct 模式下没有 {@code wf_task} 行可作锚点，事件驱动的状态反写
	 * 改为「按引擎 taskId 直接改 ACT 业务列」。但协办（{@code COADJUTANT}）/抄送（{@code CIRCULATE}）
	 * 等其它业务语义<b>不能</b>被当成待办改掉 —— 条件式等价于旧实现的「仅待办态才动」，
	 * 且天然幂等（已是目标态则命中 0 行）。</p>
	 *
	 * @param engineTaskId 引擎任务 id（{@code ACT_*.ID_}）
	 * @param newStatus    目标业务状态字符串（{@link #statusStr(Integer)} 口径）
	 * @return 命中行数（0 = 当前不是待办态，或该行已不存在）
	 */
	public int markStatusIfTodo(String engineTaskId, String newStatus) {
		if (engineTaskId == null || newStatus == null) {
			return 0;
		}
		String sql = "UPDATE %s SET BUSINESS_STATUS_=? WHERE ID_=? "
			+ "AND (BUSINESS_STATUS_='TODO' OR BUSINESS_STATUS_ IS NULL)";
		int n = 0;
		for (String table : new String[]{"ACT_HI_TASKINST", "ACT_RU_TASK"}) {
			try {
				n += jdbcTemplate.update(String.format(sql, table), newStatus, engineTaskId);
			} catch (Exception e) {
				log.warn("[WfTaskActDirectWriter] 条件反写 ACT 业务状态失败（不阻断主流程）. {}", e.getMessage());
			}
		}
		return n;
	}

	/**
	 * 关闭实例下的残留待办（待办 {@code TODO} / 协办 {@code COADJUTANT} → 办结 {@code newStatus}）。
	 *
	 * <p>按 {@code ACT_HI_TASKINST.PROC_INST_ID_} + {@code END_TIME_ IS NULL} 定位「引擎已终结、
	 * 但历史行仍标待办」的孤儿行。direct 模式下 {@code wf_task} 为空，旧实现
	 * （{@code WfStateProjector#closeResidualTodos} 查 wf_task）必然一条都关不掉，
	 * 致撤销/撤回/归档后残留任务的 {@code BUSINESS_STATUS_} 永远停在 {@code 'TODO'}
	 * （已办列表显示「待办」、按状态筛选漏行）。</p>
	 *
	 * @param procInstId 引擎流程实例 id（{@code ACT_*.PROC_INST_ID_}）
	 * @return 关闭条数
	 */
	public int closeResidualByProcInst(String procInstId, String newStatus) {
		if (procInstId == null || newStatus == null) {
			return 0;
		}
		String sql = "UPDATE ACT_HI_TASKINST SET BUSINESS_STATUS_=? WHERE PROC_INST_ID_=? "
			+ "AND END_TIME_ IS NULL AND (BUSINESS_STATUS_='TODO' OR BUSINESS_STATUS_='COADJUTANT')";
		try {
			return jdbcTemplate.update(sql, newStatus, procInstId);
		} catch (Exception e) {
			log.warn("[WfTaskActDirectWriter] 关闭残留待办失败（不阻断主流程）. {}", e.getMessage());
			return 0;
		}
	}

	/**
	 * 静态版本：供 {@link WfTaskActWriter#sync} 委托复用，避免其构造器签名变化
	 * （H2 测试以 {@code new WfTaskActWriter(jdbc)} 构造）。
	 */
	public static void writePartial(JdbcTemplate jdbcTemplate, String engineTaskId,
									LinkedHashMap<String, Object> cols) {
		if (engineTaskId == null || cols == null || cols.isEmpty()) {
			return;
		}
		StringBuilder set = new StringBuilder();
		Object[] values = new Object[cols.size() + 1];
		int i = 0;
		for (Map.Entry<String, Object> e : cols.entrySet()) {
			if (!ALLOWED_COLUMNS.contains(e.getKey())) {
				log.warn("[WfTaskActDirectWriter] 非法业务列名被忽略: {}", e.getKey());
				continue;
			}
			if (set.length() > 0) {
				set.append(", ");
			}
			set.append(e.getKey()).append("=?");
			values[i++] = e.getValue();
		}
		if (set.length() == 0) {
			return;
		}
		values[i] = engineTaskId;
		Object[] args = new Object[i + 1];
		System.arraycopy(values, 0, args, 0, i + 1);
		String sql = "UPDATE %s SET " + set + " WHERE ID_=?";
		updateQuietly(jdbcTemplate, String.format(sql, "ACT_HI_TASKINST"), args);
		updateQuietly(jdbcTemplate, String.format(sql, "ACT_RU_TASK"), args);
	}

	private static void updateQuietly(JdbcTemplate jdbcTemplate, String sql, Object[] args) {
		try {
			jdbcTemplate.update(sql, args);
		} catch (Exception e) {
			log.warn("[WfTaskActDirectWriter] 写回 ACT 业务列失败（不阻断主流程）. {}", e.getMessage());
		}
	}

	/** 业务子状态码 → 原生 BUSINESS_STATUS_ 字符串（与旧 WfTaskActWriter 同口径） */
	public static String statusStr(Integer status) {
		if (status == null) {
			return null;
		}
		return switch (status) {
			case WfTask.STATUS_TODO -> "TODO";
			case WfTask.STATUS_DONE -> "DONE";
			case WfTask.STATUS_FINISHED -> "FINISHED";
			case WfTask.STATUS_AUTO_SUBMIT -> "AUTO_SUBMIT";
			case WfTask.STATUS_COADJUTANT -> "COADJUTANT";
			case WfTask.STATUS_CIRCULATE -> "CIRCULATE";
			case WfTask.STATUS_READ -> "READ";
			default -> null;
		};
	}

	/** 租户兜底：无登录上下文时 WfAuthUtil.tenantId() 返回空串，落默认租户 */
	private static String resolveTenant(String tenantId) {
		if (tenantId != null && !tenantId.isBlank()) {
			return tenantId;
		}
		String current = WfAuthUtil.tenantId();
		if (current != null && !current.isBlank()) {
			return current;
		}
		return "000000";
	}
}
