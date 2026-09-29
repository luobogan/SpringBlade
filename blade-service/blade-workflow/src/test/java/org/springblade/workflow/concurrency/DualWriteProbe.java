package org.springblade.workflow.concurrency;

import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.helper.WfInstanceActWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 双写并发探针：把「引擎发起 + 业务列回写 ACT_*」收敛到同一个 Spring 事务里，
 * 模拟生产 {@code WfInstanceServiceImpl.start} 的事务边界。
 *
 * <p>每个探针方法都 {@code @Transactional}：引擎 INSERT {@code ACT_HI_PROCINST} 与
 * {@code WfInstanceActWriter} 的 JdbcTemplate UPDATE 经共享事务管理器落到同一事务，
 * 以此验证「双写原子性」（引擎成则业务列必成、引擎回滚则业务列一并回滚）。</p>
 */
@Component
public class DualWriteProbe {

	@Autowired
	private IProcessService processService;
	@Autowired
	private WfInstanceActWriter writer;

	/** 发起流程并双写业务列，返回 Flowable 流程实例ID（=ACT_HI_PROCINST.ID_） */
	@Transactional
	public String startAndWrite(Long wfId, String procKey, Long starterId, String title) {
		Map<String, Object> vars = new HashMap<>();
		String instId = processService.startInstance(procKey, String.valueOf(wfId), vars);

		WfInstance inst = new WfInstance();
		inst.setId(wfId);
		inst.setDefId(123L);
		inst.setFormId(1L);
		inst.setDataId(wfId);
		inst.setTitle(title);
		inst.setStarter(starterId);
		inst.setIsTest(0);
		inst.setCurrentNodeKey("start");
		inst.setUrgency(0);
		inst.setBusinessRowReady(1);
		inst.setEngineDeploymentMatched(1);
		inst.setProcDefId(processService.historicProcess(instId).getProcessDefinitionId());

		writer.writeOnStart(inst, instId, WfInstance.STATUS_RUNNING);
		return instId;
	}

	@Transactional
	public void writeNode(String instId, String nodeKey) {
		writer.writeCurrentNodeKey(instId, nodeKey);
	}

	@Transactional
	public void terminate(String instId) {
		writer.writeLifecycle(instId, WfInstance.STATUS_CANCELED, new Date());
	}
}
