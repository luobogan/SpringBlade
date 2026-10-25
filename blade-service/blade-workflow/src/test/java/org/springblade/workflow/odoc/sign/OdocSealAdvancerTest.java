package org.springblade.workflow.odoc.sign;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.message.feign.INoticeClient;
import org.springblade.workflow.action.NodeActionExecutor;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.odoc.entity.WfOdocSignFlow;
import org.springblade.workflow.odoc.mapper.WfOdocSignFlowMapper;
import org.springblade.workflow.resolver.WfBpmnExtensionReader;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.IWfInstanceService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OdocSealAdvancer} 单测：「签完才流转」推进器的幂等与拒签恢复。
 */
class OdocSealAdvancerTest {

	private WfOdocSignFlowMapper flowMapper;
	private WfTaskMapper taskMapper;
	private WfInstanceMapper instanceMapper;
	private WfProcessNodeMapper nodeMapper;
	private WfBpmnExtensionReader bpmnReader;
	private IProcessService processService;
	private IWfInstanceService instanceService;
	private NodeActionExecutor nodeActionExecutor;
	private INoticeClient noticeClient;
	private OdocSealAdvancer advancer;

	@BeforeEach
	void setUp() {
		flowMapper = mock(WfOdocSignFlowMapper.class);
		taskMapper = mock(WfTaskMapper.class);
		instanceMapper = mock(WfInstanceMapper.class);
		nodeMapper = mock(WfProcessNodeMapper.class);
		bpmnReader = mock(WfBpmnExtensionReader.class);
		processService = mock(IProcessService.class);
		instanceService = mock(IWfInstanceService.class);
		nodeActionExecutor = mock(NodeActionExecutor.class);
		noticeClient = mock(INoticeClient.class);
		advancer = new OdocSealAdvancer(flowMapper, taskMapper, instanceMapper, nodeMapper, bpmnReader,
			processService, instanceService, nodeActionExecutor, noticeClient);
	}

	private WfOdocSignFlow flow(int advanceStatus) {
		WfOdocSignFlow f = new WfOdocSignFlow();
		f.setId(1L);
		f.setInstId(10L);
		f.setNodeKey("signNode");
		f.setEngineTaskId("engine-1");
		f.setOperator(100L);
		f.setAdvanceStatus(advanceStatus);
		return f;
	}

	@Test
	@DisplayName("saveContext：记录待推进上下文（引擎任务ID/提交人/变量）")
	void saveContext() {
		WfOdocSignFlow f = flow(WfOdocSignFlow.ADVANCE_STATUS_PENDING);
		when(flowMapper.selectById(1L)).thenReturn(f);

		WfTask task = new WfTask();
		task.setId(555L);
		task.setEngineTaskId("engine-1");
		Map<String, Object> vars = new HashMap<>();
		vars.put("k", "v");

		advancer.saveContext(1L, "signNode", task, 100L, vars);

		assertEquals("signNode", f.getNodeKey());
		assertEquals(555L, f.getTaskId());
		assertEquals("engine-1", f.getEngineTaskId());
		assertEquals(100L, f.getOperator());
		assertEquals(WfOdocSignFlow.ADVANCE_STATUS_PENDING, f.getAdvanceStatus());
		verify(flowMapper).updateById(f);
	}

	@Test
	@DisplayName("advance：CAS 抢占成功后完成引擎任务并推进流程")
	void advanceSuccess() {
		WfOdocSignFlow f = flow(WfOdocSignFlow.ADVANCE_STATUS_PENDING);
		when(flowMapper.selectById(1L)).thenReturn(f);
		when(flowMapper.update(any(), any())).thenReturn(1);
		WfInstance inst = new WfInstance();
		inst.setId(10L);
		when(instanceMapper.selectById(10L)).thenReturn(inst);

		assertTrue(advancer.advance(1L));

		verify(processService).completeTask(anyString(), any());
		verify(instanceService).advance(10L, 100L);
		assertEquals(WfOdocSignFlow.ADVANCE_STATUS_DONE, f.getAdvanceStatus());
	}

	@Test
	@DisplayName("advance：已被其它线程抢占时跳过（幂等，不重复推进）")
	void advanceClaimFailed() {
		WfOdocSignFlow f = flow(WfOdocSignFlow.ADVANCE_STATUS_PENDING);
		when(flowMapper.selectById(1L)).thenReturn(f);
		when(flowMapper.update(any(), any())).thenReturn(0);

		assertFalse(advancer.advance(1L));
		verify(processService, never()).completeTask(anyString(), any());
	}

	@Test
	@DisplayName("advance：已推进过的记录直接返回成功")
	void advanceAlreadyDone() {
		WfOdocSignFlow f = flow(WfOdocSignFlow.ADVANCE_STATUS_DONE);
		when(flowMapper.selectById(1L)).thenReturn(f);

		assertTrue(advancer.advance(1L));
		verify(processService, never()).completeTask(anyString(), any());
	}

	@Test
	@DisplayName("advance：引擎异常时回落为待推进，等待补偿重试")
	void advanceFailureKeepsPending() {
		WfOdocSignFlow f = flow(WfOdocSignFlow.ADVANCE_STATUS_PENDING);
		when(flowMapper.selectById(1L)).thenReturn(f);
		when(flowMapper.update(any(), any())).thenReturn(1);
		WfInstance inst = new WfInstance();
		inst.setId(10L);
		when(instanceMapper.selectById(10L)).thenReturn(inst);
		org.mockito.Mockito.doThrow(new RuntimeException("engine down"))
			.when(processService).completeTask(anyString(), any());

		assertFalse(advancer.advance(1L));
		assertEquals(WfOdocSignFlow.ADVANCE_STATUS_PENDING, f.getAdvanceStatus());
	}

	@Test
	@DisplayName("terminate：拒签/超时时把业务待办恢复为待办")
	void terminateRestoresTodo() {
		WfOdocSignFlow f = flow(WfOdocSignFlow.ADVANCE_STATUS_PENDING);
		f.setTaskId(555L);
		when(flowMapper.selectById(1L)).thenReturn(f);
		WfTask task = new WfTask();
		task.setId(555L);
		task.setStatus(WfTask.STATUS_DONE);
		when(taskMapper.selectById(555L)).thenReturn(task);

		advancer.terminate(1L, "拒签");

		assertEquals(WfOdocSignFlow.ADVANCE_STATUS_TERMINATED, f.getAdvanceStatus());
		assertEquals(WfTask.STATUS_TODO, task.getStatus());
		verify(taskMapper).updateById(task);
	}
}