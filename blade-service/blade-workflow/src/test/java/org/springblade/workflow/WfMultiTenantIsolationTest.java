package org.springblade.workflow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多租户越权隔离回归（《去 wf_ 表改造分析》V19–V21 项）。
 *
 * <p>去 wf_ 表后，实例/任务/审批日志大量读源切到原生 {@code ACT_*} 表。这些表<b>不受 Blade 租户插件
 * 自动过滤</b>（见 {@code WfAuthUtil#tenantId} 注释），读查询必须显式 {@code eq(TENANT_ID_, ...)}
 * 才能避免跨租户串数据。本测试用 Flowable 引擎的真实多租户能力（同流程 key 不同 tenantId 部署）
 * 实证，并直接对 {@code ACT_*} 表的 {@code TENANT_ID_} 列做断言——即应用 act-read 层所依赖的契约：</p>
 * <ul>
 *   <li><b>V19</b>：双租户部署 + 发起，引擎把 {@code TENANT_ID_} 落到 ACT_HI_PROCINST / ACT_RU_TASK；</li>
 *   <li><b>V20</b>：越权隔离 —— 两租户同 assignee 的待办，按租户过滤后互不越权可见；</li>
 *   <li><b>V21</b>：读源=act 的列表类查询（ACT_RU_TASK / ACT_HI_PROCINST）必须带租户谓词，
 *        否则无租户过滤会串数据（控制用例显式暴露该风险）。</li>
 * </ul>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FlowableTestConfig.class)
class WfMultiTenantIsolationTest {

	private static final String PROC_KEY = "tenantIso";
	private static final String T1 = "T1";
	private static final String T2 = "T2";

	private static final String BPMN = """
		<?xml version="1.0" encoding="UTF-8"?>
		<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
		             xmlns:flowable="http://flowable.org/bpmn"
		             targetNamespace="http://springblade.workflow"
		             id="definitions_tenantIso">
		    <process id="%s" name="租户隔离" isExecutable="true">
		        <startEvent id="startEvent" name="发起"/>
		        <userTask id="nodeA" name="节点A" flowable:assignee="1001"/>
		        <endEvent id="endEvent" name="结束"/>
		        <sequenceFlow id="f1" sourceRef="startEvent" targetRef="nodeA"/>
		        <sequenceFlow id="f2" sourceRef="nodeA" targetRef="endEvent"/>
		    </process>
		</definitions>
		""".formatted(PROC_KEY);

	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private DataSource dataSource;

	private JdbcTemplate jdbc;
	private final List<String> deployed = new java.util.ArrayList<>();

	@BeforeEach
	void deployTwoTenants() {
		jdbc = new JdbcTemplate(dataSource);
		// 同 key 跨租户部署（Flowable 多租户：TENANT_ID_ 落到 ACT_RE_PROCDEF / ACT_RU_TASK / ACT_HI_*）
		deployed.add(deploy(PROC_KEY, T1));
		deployed.add(deploy(PROC_KEY, T2));
		// 两租户各发起一实例，且待办 assignee 同为 1001（刻意同人，隔离只能靠租户过滤）
		runtimeService.startProcessInstanceByKeyAndTenantId(PROC_KEY, new HashMap<>(), T1);
		runtimeService.startProcessInstanceByKeyAndTenantId(PROC_KEY, new HashMap<>(), T2);
	}

	private String deploy(String key, String tenant) {
		return repositoryService.createDeployment()
			.addString(key + "-" + tenant + ".bpmn20.xml", BPMN)
			.tenantId(tenant)
			.deploy()
			.getId();
	}

	@AfterEach
	void cleanup() {
		for (String depId : deployed) {
			repositoryService.deleteDeployment(depId, true);
		}
		deployed.clear();
	}

	@Test
	@DisplayName("V19: 双租户实例/运行期任务均按 TENANT_ID_ 隔离落库（各 1 条）")
	void v19_instancesAndTasksIsolatedByTenant() {
		assertThat(jdbc.queryForObject(
			"SELECT COUNT(*) FROM ACT_HI_PROCINST WHERE TENANT_ID_ = ?", Integer.class, T1))
			.isEqualTo(1);
		assertThat(jdbc.queryForObject(
			"SELECT COUNT(*) FROM ACT_HI_PROCINST WHERE TENANT_ID_ = ?", Integer.class, T2))
			.isEqualTo(1);
		assertThat(jdbc.queryForObject(
			"SELECT COUNT(*) FROM ACT_RU_TASK WHERE TENANT_ID_ = ?", Integer.class, T1))
			.isEqualTo(1);
		assertThat(jdbc.queryForObject(
			"SELECT COUNT(*) FROM ACT_RU_TASK WHERE TENANT_ID_ = ?", Integer.class, T2))
			.isEqualTo(1);
	}

	@Test
	@DisplayName("V20: 越权隔离 —— 同 assignee(1001) 的跨租户待办，按租户过滤后互不越权可见")
	void v20_noCrossTenantVisibility_forSameAssignee() {
		// T1 视野内（ASSIGNEE_=1001 且 TENANT_ID_=T1）恰好 1 条
		assertThat(jdbc.queryForObject(
			"SELECT COUNT(*) FROM ACT_RU_TASK WHERE ASSIGNEE_ = '1001' AND TENANT_ID_ = ?",
			Integer.class, T1)).isEqualTo(1);
		// 关键风险：若读查询只按 assignee 过滤（漏掉租户），会一次性捞出两租户共 2 条 → 越权可见
		int leaked = jdbc.queryForObject(
			"SELECT COUNT(*) FROM ACT_RU_TASK WHERE ASSIGNEE_ = '1001'", Integer.class);
		assertThat(leaked).isEqualTo(2);
		assertThat(leaked).isGreaterThan(1); // 提醒：应用侧 .eq(tenantId) 不可省略
	}

	@Test
	@DisplayName("V21: 读源=act 列表查询必须带 TENANT_ID_ 谓词（控制用例暴露无过滤即串数据）")
	void v21_actReadList_mustFilterByTenant() {
		// 带租户谓词：各自 1 条
		assertThat(jdbc.queryForObject(
			"SELECT COUNT(*) FROM ACT_RU_TASK WHERE TENANT_ID_ = ?", Integer.class, T1)).isEqualTo(1);
		assertThat(jdbc.queryForObject(
			"SELECT COUNT(*) FROM ACT_HI_PROCINST WHERE TENANT_ID_ = ?", Integer.class, T1)).isEqualTo(1);
		// 控制用例：若读查询漏掉租户过滤，会串出两租户数据
		int withoutFilter = jdbc.queryForObject(
			"SELECT COUNT(*) FROM ACT_RU_TASK WHERE TENANT_ID_ IS NOT NULL", Integer.class);
		assertThat(withoutFilter).isEqualTo(2);
	}

	@Test
	@DisplayName("V21-实例级读安全: ACT_HI_COMMENT/PROCINST 按全局唯一 PROC_INST_ID_ 读天然不越租户")
	void v21_instanceScopedRead_safeAcrossTenants() {
		// 取 T1 的引擎实例 id（按租户过滤），再按 PROC_INST_ID_ 读（WfApprovalLogActReader 的读模式）
		String t1EngineId = jdbc.queryForObject(
			"SELECT ID_ FROM ACT_HI_PROCINST WHERE TENANT_ID_ = ? LIMIT 1", String.class, T1);
		assertThat(t1EngineId).isNotNull();
		// 即便不加 TENANT_ID_ 过滤，按全局唯一的 PROC_INST_ID_ 读也只命中本实例
		assertThat(jdbc.queryForObject(
			"SELECT COUNT(*) FROM ACT_HI_PROCINST WHERE ID_ = ?", Integer.class, t1EngineId))
			.isEqualTo(1);
		// 且该实例确属 T1（不是 T2 的同 key 实例）
		String tenant = jdbc.queryForObject(
			"SELECT TENANT_ID_ FROM ACT_HI_PROCINST WHERE ID_ = ?", String.class, t1EngineId);
		assertThat(tenant).isEqualTo(T1);
	}
}
