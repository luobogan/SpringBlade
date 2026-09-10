package org.springblade.workflow;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.vo.TaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flowable 引擎适配验证（隔离式，内存 H2）：
 * 1. ProcessEngine 能装配成功；
 * 2. classpath:processes/*.bpmn20.xml 自动部署；
 * 3. 引擎发起 -> 查询当前任务 -> 完成 -> 流程结束。
 *
 * <p>说明：本测试只验证<b>引擎适配层</b>（IProcessService），
 * 不涉及 wf_* 语义表（语义层由 WfInstanceService / WfTaskService 承担）。</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FlowableTestConfig.class)
class FlowableIntegrationTest {

	@Autowired
	private ProcessEngine processEngine;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private TaskService taskService;
	@Autowired
	private IProcessService processService;

	@Test
	void engineShouldBeCreated() {
		assertThat(processEngine).isNotNull();
		assertThat(repositoryService).isNotNull();
		assertThat(runtimeService).isNotNull();
		assertThat(taskService).isNotNull();
	}

	@Test
	void bpmnShouldBeAutoDeployed() {
		long count = repositoryService.createProcessDefinitionQuery()
			.processDefinitionKey("simpleApproval")
			.count();
		assertThat(count).isEqualTo(1L);
	}

	@Test
	void shouldStartAndCompleteApproval() {
		// 引擎发起（业务主键 formId:dataId）
		String engineInstId = processService.startInstance(
			"simpleApproval", "1:1", Map.<String, Object>of("approver", "zhangsan"));
		assertThat(engineInstId).isNotNull();

		// 当前活动任务
		List<TaskVO> current = processService.currentTasks(engineInstId);
		assertThat(current).hasSize(1);
		assertThat(current.get(0).getTaskName()).isEqualTo("审批");

		// 按办理人查询
		List<TaskVO> tasks = processService.queryTasks("zhangsan");
		assertThat(tasks).isNotEmpty();
		TaskVO task = tasks.get(0);
		assertThat(task.getProcessInstanceId()).isEqualTo(engineInstId);

		// 完成审批
		processService.completeTask(task.getTaskId(), Map.<String, Object>of());

		// 流程应已结束（无活动实例）
		assertThat(runtimeService.createProcessInstanceQuery()
			.processInstanceId(engineInstId)
			.count()).isZero();
	}

}
