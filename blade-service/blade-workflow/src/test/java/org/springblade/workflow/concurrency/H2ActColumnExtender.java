package org.springblade.workflow.concurrency;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 给内存 H2 的 {@code ACT_HI_PROCINST} 补齐本改造新增的业务列。
 *
 * <p>生产环境由 {@code doc/sql/migration/act_add_columns.sql} 负责加列；H2 自动建表只含 Flowable 原生列，
 * 故测试里手动补齐，使 {@code WfInstanceActWriter.writeOnStart} 的 UPDATE 能命中这些列。</p>
 *
 * <p>列定义与 {@code act_add_columns.sql} 对齐（仅取 writer 实际写入的子集）。H2 大小写不敏感，列名统一大写。</p>
 */
public class H2ActColumnExtender {

	private final JdbcTemplate jdbc;

	public H2ActColumnExtender(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
		addColumns();
	}

	private void addColumns() {
		String[][] cols = {
			{"BUSINESS_ID_", "BIGINT"},
			{"DEF_ID_", "VARCHAR(64)"},
			{"DATA_ID_", "BIGINT"},
			{"FORM_ID_", "BIGINT"},
			{"TITLE_", "VARCHAR(255)"},
			{"IS_TEST_", "INT"},
			{"BUSINESS_STATUS_", "VARCHAR(32)"},
			{"STARTER_", "VARCHAR(64)"},
			{"CURRENT_NODE_KEY_", "VARCHAR(64)"},
			{"URGENCY_", "INT"},
			{"BUSINESS_ROW_READY_", "INT"},
			{"ENGINE_DEPLOY_MATCHED_", "INT"},
			{"PARENT_ID_", "VARCHAR(64)"},
			{"TEST_DEPLOYMENT_ID_", "VARCHAR(64)"},
			{"REQUEST_ID_BOUND_", "INT"},
			// R1/D6/M1 桥接列：引擎 KEY_（= ACT_RE_PROCDEF.KEY_），与 act_add_def_key_bridge.sql 对齐
			{"DEF_KEY_", "VARCHAR(255)"}
		};
		for (String[] c : cols) {
			try {
				jdbc.execute("ALTER TABLE ACT_HI_PROCINST ADD COLUMN " + c[0] + " " + c[1]);
			} catch (Exception ignored) {
				// 列已存在（H2 跨测试类复用内存库）则忽略
			}
		}
	}
}
