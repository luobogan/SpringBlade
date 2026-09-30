package org.springblade.workflow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.impl.ProcessServiceImpl;
import org.springblade.workflow.vo.TaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 导入类流程「非标准 BPMNDI 属性」回归（根因修复验证）。
 *
 * <p><b>根因</b>：前端画布导出的 {@code bpmndi:BPMNShape / BPMNEdge} 上带
 * {@code strokeWidth="..."} 扩展属性，BPMNDI XSD 未定义它。生产部署
 * {@code deployProcess} 保留完整 XML Schema 校验 → 部署直接被拒：
 * {@code cvc-complex-type.3.2.2: 元素 'bpmndi:BPMNEdge' 中不允许出现属性 'strokeWidth'}。
 * 所有导入类流程（comprehensiveApproval 系列）正式发布必挂；测试部署
 * {@code deployProcessForTest} 因 {@code disableSchemaValidation} 而幸免，掩盖了问题。</p>
 *
 * <p><b>修复</b>：{@code ProcessServiceImpl.deployProcess} 部署前经
 * {@code sanitizeBpmnDiForSchema} 清理图形装饰属性（只删 DI 扩展属性，
 * 节点/连线/条件表达式等流程语义原样保留）。</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FlowableTestConfig.class)
class WfImportedBpmnDiSchemaTest {

	/** 复刻导入流程形态：含 strokeWidth DI + 排他网关条件（生产部署开 Schema 校验） */
	private static final String DIRTY_BPMN = """
		<?xml version="1.0" encoding="UTF-8"?>
		<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
		             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		             xmlns:flowable="http://flowable.org/bpmn"
		             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
		             xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
		             xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
		             targetNamespace="http://springblade.workflow"
		             id="defs_compApprovalRepro">
		    <process id="compApprovalRepro" name="导入综合审批回归" isExecutable="true">
		        <startEvent id="startEvent" name="发起"/>
		        <exclusiveGateway id="amountGateway" name="金额判断" default="f_high"/>
		        <userTask id="financeApproval" name="财务审批"/>
		        <userTask id="directorApproval" name="总经理审批"/>
		        <endEvent id="endEvent" name="归档"/>
		        <sequenceFlow id="f1" sourceRef="startEvent" targetRef="amountGateway"/>
		        <sequenceFlow id="f_low" sourceRef="amountGateway" targetRef="financeApproval">
		            <conditionExpression xsi:type="tFormalExpression">${amount &lt;= 5000}</conditionExpression>
		        </sequenceFlow>
		        <sequenceFlow id="f_high" sourceRef="amountGateway" targetRef="directorApproval">
		            <conditionExpression xsi:type="tFormalExpression">${amount &gt; 5000}</conditionExpression>
		        </sequenceFlow>
		        <sequenceFlow id="f_fin_end" sourceRef="financeApproval" targetRef="endEvent"/>
		        <sequenceFlow id="f_dir_end" sourceRef="directorApproval" targetRef="endEvent"/>
		    </process>
		    <bpmndi:BPMNDiagram id="di_1">
		        <bpmndi:BPMNPlane id="plane_1" bpmnElement="compApprovalRepro">
		            <bpmndi:BPMNShape id="s_start" bpmnElement="startEvent" strokeWidth="2.0">
		                <dc:Bounds x="100" y="100" width="30" height="30"/>
		            </bpmndi:BPMNShape>
		            <bpmndi:BPMNShape id="s_gw" bpmnElement="amountGateway" strokeWidth="2.0">
		                <dc:Bounds x="200" y="80" width="40" height="40"/>
		            </bpmndi:BPMNShape>
		            <bpmndi:BPMNEdge id="e_f1" bpmnElement="f1" strokeWidth="2.5">
		                <di:waypoint x="115" y="115"/>
		                <di:waypoint x="200" y="100"/>
		            </bpmndi:BPMNEdge>
		        </bpmndi:BPMNPlane>
		    </bpmndi:BPMNDiagram>
		</definitions>
		""";

	private static final String KEY = "compApprovalRepro";

	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private IProcessService processService;

	private String deploymentId;

	@AfterEach
	void cleanup() {
		if (deploymentId != null) {
			try {
				processService.deleteDeployment(deploymentId);
			} catch (Exception ignore) {
				// 已被用例内清理
			}
			deploymentId = null;
		}
	}

	/** 修复前：本用例在 deployProcess 处抛 cvc-complex-type.3.2.2（strokeWidth）；修复后必须部署成功 */
	@Test
	void deployProcess_withCanvasStrokeWidthDi_shouldPassSchemaValidation() throws Exception {
		deploymentId = processService.deployProcess(KEY, DIRTY_BPMN);
		assertThat(deploymentId).isNotNull();

		ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
			.deploymentId(deploymentId).singleResult();
		assertThat(pd).isNotNull();
		assertThat(pd.getKey()).isEqualTo(KEY);

		// 引擎保存的是消毒后的 XML：strokeWidth 已被剥离
		String engineXml;
		try (java.io.InputStream in = repositoryService
				.getResourceAsStream(deploymentId, KEY + ".bpmn20.xml")) {
			engineXml = new String(in.readAllBytes());
		}
		assertThat(engineXml).doesNotContain("strokeWidth");
		// default 流（f_high）上的冗余条件已被剥离，仅剩非 default 流（f_low）的条件
		assertThat(engineXml).contains("conditionExpression");
		assertThat(countOccurrences(engineXml, "conditionExpression")).isEqualTo(2);
		// 非 default 流的条件保留，default 流（f_high）的条件被剥离
		assertThat(engineXml).contains("${amount &lt;= 5000}");
		assertThat(engineXml).doesNotContain("${amount &gt; 5000}");
	}

	/** 消毒只删 DI 装饰属性与 default 流冗余条件，不碰业务语义；干净 XML 原样透传 */
	@Test
	void sanitize_shouldStripOnlyDiAttr_andPreserveSemantics() {
		String cleaned = ProcessServiceImpl.sanitizeForDeploy(DIRTY_BPMN);
		assertThat(cleaned).doesNotContain("strokeWidth");
		// default 流（f_high）条件被剥离；非 default 流（f_low）条件保留
		assertThat(countOccurrences(cleaned, "conditionExpression")).isEqualTo(2);
		assertThat(cleaned).contains("${amount &lt;= 5000}");
		assertThat(cleaned).doesNotContain("${amount &gt; 5000}");
		assertThat(cleaned).contains("default=\"f_high\""); // default 引用本身合法，保留
		assertThat(cleaned).contains("amountGateway");
		// 幂等/无害：无 strokeWidth、无 default= 的 XML 原样返回（同一引用）
		assertThat(ProcessServiceImpl.sanitizeForDeploy("<a/>")).isSameAs("<a/>");
		assertThat(ProcessServiceImpl.sanitizeForDeploy(null)).isNull();
	}

	/** 消毒后的流程语义不变：金额分支按条件正确路由并驱动到归档 */
	@Test
	void drive_sanitizedFlow_routesByAmountCondition() {
		deploymentId = processService.deployProcess(KEY, DIRTY_BPMN);

		Set<String> high = driveOnce(8000);
		assertThat(high).contains("directorApproval");
		assertThat(high).doesNotContain("financeApproval");

		Set<String> low = driveOnce(300);
		assertThat(low).contains("financeApproval");
		assertThat(low).doesNotContain("directorApproval");
	}

	private static int countOccurrences(String text, String token) {
		return text.split(java.util.regex.Pattern.quote(token), -1).length - 1;
	}

	private Set<String> driveOnce(int amount) {
		String inst = processService.startInstance(KEY, "repro:" + amount,
			Map.of("amount", amount));
		assertThat(inst).isNotNull();
		Set<String> visited = new HashSet<>();
		for (int i = 0; i < 20; i++) {
			List<TaskVO> tasks = processService.currentTasks(inst);
			if (tasks.isEmpty()) {
				break;
			}
			visited.add(tasks.get(0).getTaskDefinitionKey());
			processService.completeTask(tasks.get(0).getTaskId(), Map.of("amount", amount));
		}
		assertThat(processService.pendingJobCount()).isZero();
		return visited;
	}
}
