package org.springblade.workflow.odoc.sign;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.message.constant.MessageConstant;
import org.springblade.message.dto.NoticeBizStateDTO;
import org.springblade.message.feign.INoticeClient;
import org.springblade.workflow.action.NodeActionExecutor;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.odoc.entity.WfOdocSignFlow;
import org.springblade.workflow.odoc.mapper.WfOdocSignFlowMapper;
import org.springblade.workflow.resolver.WfBpmnExtensionReader;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.IWfInstanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 「签完才流转」推进器：第三方签章完成后，把卡在签章节点的流程推进到下一节点。
 *
 * <p>背景：用户拍板 e签宝走「回调再推进」——签章节点的「提交」不再直接流转引擎，
 * 而是把待推进上下文落在 {@code wf_odoc_sign_flow}（engine_task_id / operator / vars_json），
 * 签署完成（回调或补偿任务确认）后由本类推进。</p>
 *
 * <p>幂等与并发：{@code advance_status} 走 CAS（0待推进 → 1推进中 → 2已推进），
 * 多次回调/补偿并发只会有一次真正推进；推进失败回落为「待推进」等待下一轮补偿重试。</p>
 *
 * <p>拒签/超时：{@link #terminate} 把推进状态置「已终止」，并把业务待办恢复为待办，
 * 让办理人可以重新提交（重新发起新一轮签署）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OdocSealAdvancer {

	private final WfOdocSignFlowMapper flowMapper;
	private final WfTaskMapper taskMapper;
	private final WfInstanceMapper instanceMapper;
	private final WfProcessNodeMapper nodeMapper;
	private final WfBpmnExtensionReader bpmnReader;
	private final IProcessService processService;
	private final IWfInstanceService instanceService;
	private final NodeActionExecutor nodeActionExecutor;
	private final INoticeClient noticeClient;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/**
	 * 记录「待推进上下文」：发起签署时调用。
	 *
	 * @param signFlowId 本地签署流程ID（wf_odoc_sign_flow.id）
	 * @param nodeKey    签章节点Key
	 * @param task       业务待办（取 engineTaskId）
	 * @param operator   提交人
	 * @param vars       提交变量（推进时回填引擎）
	 */
	@Transactional(rollbackFor = Exception.class)
	public void saveContext(Long signFlowId, String nodeKey, WfTask task, Long operator, Map<String, Object> vars) {
		if (signFlowId == null) {
			return;
		}
		WfOdocSignFlow flow = flowMapper.selectById(signFlowId);
		if (flow == null) {
			return;
		}
		flow.setNodeKey(nodeKey);
		flow.setTaskId(task == null ? null : task.getId());
		flow.setEngineTaskId(task == null ? null : task.getEngineTaskId());
		flow.setOperator(operator);
		flow.setVarsJson(vars == null || vars.isEmpty() ? null : toJson(vars));
		flow.setAdvanceStatus(WfOdocSignFlow.ADVANCE_STATUS_PENDING);
		flow.setAdvanceMsg("等待签署完成后推进流程");
		flowMapper.updateById(flow);
		log.info("[odoc-seal] 已记录待推进上下文 signFlowId={}, nodeKey={}, engineTaskId={}",
			signFlowId, nodeKey, flow.getEngineTaskId());
	}

	/**
	 * 推进卡在签章节点的流程（幂等）。
	 *
	 * @param signFlowId 本地签署流程ID
	 * @return true=已推进（或此前已推进）
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean advance(Long signFlowId) {
		if (signFlowId == null) {
			return false;
		}
		WfOdocSignFlow flow = flowMapper.selectById(signFlowId);
		if (flow == null) {
			log.warn("[odoc-seal] 待推进的签署流程不存在 signFlowId={}", signFlowId);
			return false;
		}
		Integer status = flow.getAdvanceStatus();
		if (status != null && status == WfOdocSignFlow.ADVANCE_STATUS_DONE) {
			return true;
		}
		if (status != null && status == WfOdocSignFlow.ADVANCE_STATUS_TERMINATED) {
			return false;
		}
		if (!claim(flow)) {
			log.info("[odoc-seal] 推进已被其它线程抢占，跳过. signFlowId={}", signFlowId);
			return false;
		}
		try {
			WfInstance inst = instanceMapper.selectById(flow.getInstId());
			if (inst == null) {
				throw new IllegalStateException("流程实例不存在：" + flow.getInstId());
			}
			if (flow.getEngineTaskId() != null) {
				processService.completeTask(flow.getEngineTaskId(), parseVars(flow.getVarsJson()));
			}
			instanceService.advance(flow.getInstId(), flow.getOperator());
			// 节点后置动作与正常提交保持一致（提交时因等待签署被跳过，此处补做）
			WfProcessNode node = loadNode(inst.getDefId(), flow.getNodeKey());
			if (node != null) {
				nodeActionExecutor.execute(inst, node, NodeActionExecutor.PHASE_POST, flow.getOperator());
				nodeActionExecutor.triggerSubflow(inst, node, NodeActionExecutor.TRIGGER_AFTER_SUBMIT, flow.getOperator());
			}
			markNoticeHandled(inst, flow.getEngineTaskId());
			flow.setAdvanceStatus(WfOdocSignFlow.ADVANCE_STATUS_DONE);
			flow.setAdvanceTime(LocalDateTime.now());
			flow.setAdvanceMsg("签署完成，流程已推进");
			flowMapper.updateById(flow);
			log.info("[odoc-seal] 签署完成，流程已推进. signFlowId={}, instId={}, nodeKey={}",
				signFlowId, flow.getInstId(), flow.getNodeKey());
			return true;
		} catch (Exception e) {
			flow.setAdvanceStatus(WfOdocSignFlow.ADVANCE_STATUS_PENDING);
			flow.setAdvanceMsg("推进失败：" + e.getMessage());
			flowMapper.updateById(flow);
			log.warn("[odoc-seal] 推进流程失败（保留待推进，等待补偿重试）. signFlowId={}, {}",
				signFlowId, e.getMessage());
			return false;
		}
	}

	/**
	 * 拒签/超时：终止推进并把业务待办恢复为待办（办理人可重新提交）。
	 *
	 * @param signFlowId 本地签署流程ID
	 * @param reason     原因
	 */
	@Transactional(rollbackFor = Exception.class)
	public void terminate(Long signFlowId, String reason) {
		if (signFlowId == null) {
			return;
		}
		WfOdocSignFlow flow = flowMapper.selectById(signFlowId);
		if (flow == null) {
			return;
		}
		flow.setAdvanceStatus(WfOdocSignFlow.ADVANCE_STATUS_TERMINATED);
		flow.setAdvanceTime(LocalDateTime.now());
		flow.setAdvanceMsg(reason);
		flowMapper.updateById(flow);
		if (flow.getTaskId() != null) {
			WfTask task = taskMapper.selectById(flow.getTaskId());
			if (task != null && task.getStatus() != null && task.getStatus() != WfTask.STATUS_TODO) {
				task.setStatus(WfTask.STATUS_TODO);
				task.setOperateTime(null);
				taskMapper.updateById(task);
				log.info("[odoc-seal] 签署未完成，业务待办已恢复为待办 taskId={}, reason={}",
					task.getId(), reason);
			}
		}
	}

	/** CAS 抢占推进权：0待推进 → 1推进中 */
	private boolean claim(WfOdocSignFlow flow) {
		// 用列名字符串（UpdateWrapper 支持 String 列名），避免依赖 MyBatis-Plus 的实体字段缓存
		int rows = flowMapper.update(null, Wrappers.<WfOdocSignFlow>update()
			.set("advance_status", WfOdocSignFlow.ADVANCE_STATUS_RUNNING)
			.set("update_time", LocalDateTime.now())
			.eq("id", flow.getId())
			.and(w -> w.eq("advance_status", WfOdocSignFlow.ADVANCE_STATUS_PENDING)
				.or().isNull("advance_status")));
		return rows > 0;
	}

	/** 变量 → JSON */
	private String toJson(Object value) {
		try {
			return MAPPER.writeValueAsString(value);
		} catch (Exception e) {
			log.warn("[odoc-seal] 提交变量序列化失败，推进时不带变量. {}", e.getMessage());
			return null;
		}
	}

	/** 提交变量 JSON → 引擎变量 */
	private Map<String, Object> parseVars(String json) {
		if (json == null || json.isBlank()) {
			return new HashMap<>(4);
		}
		try {
			Map<String, Object> map = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
			});
			return map == null ? new HashMap<>(4) : map;
		} catch (Exception e) {
			log.warn("[odoc-seal] 提交变量解析失败，按空变量推进. {}", e.getMessage());
			return new HashMap<>(4);
		}
	}

	/** 加载节点（BPMN 优先，回退 wf_process_node） */
	private WfProcessNode loadNode(Long defId, String nodeKey) {
		if (defId == null || nodeKey == null) {
			return null;
		}
		try {
			WfProcessNode fromBpmn = bpmnReader.node(defId, nodeKey);
			if (fromBpmn != null) {
				return fromBpmn;
			}
		} catch (Exception e) {
			log.warn("[odoc-seal] 读取节点失败，回退 wf_process_node. defId={}, nodeKey={}, {}",
				defId, nodeKey, e.getMessage());
		}
		return nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
			.eq(WfProcessNode::getDefId, defId)
			.eq(WfProcessNode::getNodeKey, nodeKey)
			.last("LIMIT 1"));
	}

	/** 待办通知状态回写（已处理） */
	private void markNoticeHandled(WfInstance inst, String engineTaskId) {
		if (engineTaskId == null) {
			return;
		}
		try {
			NoticeBizStateDTO biz = new NoticeBizStateDTO();
			biz.setTenantId(inst == null ? null : inst.getTenantId());
			biz.setBizRefType("WF_TASK");
			biz.setBizRefId(engineTaskId);
			biz.setBizState(MessageConstant.BIZ_STATE_HANDLED);
			noticeClient.markBizState(biz);
		} catch (Exception e) {
			log.warn("[odoc-seal] 待办通知状态回写失败（忽略）. engineTaskId={}, {}", engineTaskId, e.getMessage());
		}
	}

}
