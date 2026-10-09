package org.springblade.workflow.listener;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.task.api.Task;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.message.dto.NoticeSendDTO;
import org.springblade.message.feign.INoticeClient;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 流程消息通知监听：待办产生 / 流程办结 → 经统一消息中心通知相关人。
 *
 * <p>与 {@link WfBizCallbackListener} 同构：只依赖 {@code wf_*} Mapper + 跨服务 Feign 契约
 * （{@link INoticeClient}，{@code /feign/client/notice} 前缀，网关 InnerFilter 隔离），
 * 走 {@code setEventListeners} 直接装配以避免与 {@code processEngineConfiguration}
 * 构造期循环依赖。失败只记日志、不回滚引擎事务（{@link #isFailOnException()}=false）。</p>
 *
 * <p><b>通知语义</b>（消息中心设计文档 §10）：</p>
 * <ul>
 *   <li>{@code TASK_CREATED} → 通知任务办理人：「您有一条流程待办需要处理：{标题}（{节点名}）」，
 *       {@code bizRefType=WF_TASK}，前端点击直达审批页；</li>
 *   <li>{@code PROCESS_COMPLETED} → 通知发起人：「您的流程已办结：{标题}」，
 *       {@code bizRefType=WF_INSTANCE}。</li>
 * </ul>
 *
 * <p><b>边界</b>：候选人/候选组任务（assignee 为空）暂不通知；实例记录查不到
 * （engine_inst_id 未落 wf_instance）跳过；消息由 blade-message 系统代发
 * （senderId=0，category=2，写入每人一条的 type=3 系统通知会话）。</p>
 *
 * <p>开关 {@code blade.workflow.notice.enabled}，默认 <b>true</b>；
 * 关闭即完全回到「无流程消息」的历史行为。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "blade.workflow.notice.enabled", havingValue = "true", matchIfMissing = true)
public class WfProcessNoticeListener implements FlowableEventListener {

	private final WfInstanceMapper instanceMapper;
	private final INoticeClient noticeClient;

	public WfProcessNoticeListener(WfInstanceMapper instanceMapper, INoticeClient noticeClient) {
		this.instanceMapper = instanceMapper;
		this.noticeClient = noticeClient;
	}

	@Override
	public void onEvent(FlowableEvent event) {
		if (!(event.getType() instanceof FlowableEngineEventType type)) {
			return;
		}
		if (type == FlowableEngineEventType.TASK_CREATED) {
			handleTaskCreated(event);
		} else if (type == FlowableEngineEventType.PROCESS_COMPLETED) {
			handleProcessCompleted(event);
		}
	}

	/**
	 * 待办产生：通知任务办理人（bizRefType=WF_TASK，前端跳审批页）
	 */
	private void handleTaskCreated(FlowableEvent event) {
		try {
			if (!(event instanceof FlowableEngineEntityEvent entityEvent)
				|| !(entityEvent.getEntity() instanceof Task task)) {
				return;
			}
			// 候选人/候选组任务无固定办理人，暂不通知（后续可按候选解析扩展）
			String assignee = task.getAssignee();
			if (Func.isBlank(assignee)) {
				log.debug("[WfNotice] 任务 {} 未指定办理人（候选场景），跳过通知", task.getId());
				return;
			}
			Long userId = parseUserId(assignee);
			if (userId == null) {
				return;
			}
			WfInstance instance = findInstance(entityEvent.getProcessInstanceId());
			if (instance == null) {
				log.debug("[WfNotice] 未找到 engine_inst_id={} 的 wf_instance，跳过待办通知",
					entityEvent.getProcessInstanceId());
				return;
			}
			NoticeSendDTO dto = new NoticeSendDTO();
			dto.setTenantId(instance.getTenantId());
			dto.setUserIds(Collections.singletonList(userId));
			dto.setContentType(4);
			// 模板标题在监听器内拼好（借鉴 ecology detailTitle+params），前端不二次拼装
			dto.setContent(String.format("您有一条流程待办需要处理：%s（%s）",
				Func.toStr(instance.getTitle(), "未命名流程"), Func.toStr(task.getName(), "审批")));
			dto.setBizRefType("WF_TASK");
			dto.setBizRefId(task.getId());
			push(dto, instance, "待办通知");
		} catch (Exception e) {
			log.error("[WfNotice] 待办通知异常. procInstId={}, {}", event, e.getMessage(), e);
		}
	}

	/**
	 * 流程办结：通知发起人（bizRefType=WF_INSTANCE）
	 */
	private void handleProcessCompleted(FlowableEvent event) {
		// procInstId 提到 try 外：catch 日志需要引用它
		String procInstId = event instanceof FlowableEngineEvent ee ? ee.getProcessInstanceId() : null;
		try {
			if (procInstId == null) {
				return;
			}
			WfInstance instance = findInstance(procInstId);
			if (instance == null || instance.getStarter() == null) {
				return;
			}
			NoticeSendDTO dto = new NoticeSendDTO();
			dto.setTenantId(instance.getTenantId());
			dto.setUserIds(Collections.singletonList(instance.getStarter()));
			dto.setContentType(4);
			dto.setContent(String.format("您的流程已办结：%s", Func.toStr(instance.getTitle(), "未命名流程")));
			dto.setBizRefType("WF_INSTANCE");
			dto.setBizRefId(String.valueOf(instance.getId()));
			push(dto, instance, "办结通知");
		} catch (Exception e) {
			log.error("[WfNotice] 办结通知异常. procInstId={}, {}", procInstId, e.getMessage(), e);
		}
	}

	private void push(NoticeSendDTO dto, WfInstance instance, String scene) {
		try {
			R<Boolean> result = noticeClient.sendToUsers(dto);
			if (result == null || !result.isSuccess()) {
				log.warn("[WfNotice] {}投递失败. instanceId={}, msg={}",
					scene, instance.getId(), result == null ? "null" : result.getMsg());
				return;
			}
			log.info("[WfNotice] {}已投递消息中心. instanceId={}, users={}",
				scene, instance.getId(), dto.getUserIds());
		} catch (Exception e) {
			// 通知失败不得回滚引擎事务（同 WfBizCallbackListener：通知语义，可补偿）
			log.error("[WfNotice] {}异常. instanceId={}, {}", scene, instance.getId(), e.getMessage(), e);
		}
	}

	private WfInstance findInstance(String procInstId) {
		if (Func.isBlank(procInstId)) {
			return null;
		}
		return instanceMapper.selectOne(Wrappers.<WfInstance>lambdaQuery()
			.eq(WfInstance::getEngineInstId, procInstId)
			.last("LIMIT 1"));
	}

	/**
	 * 引擎 assignee 为字符串用户ID，非数字（如候选组名）返回 null 跳过
	 */
	private Long parseUserId(String assignee) {
		try {
			return Long.valueOf(assignee.trim());
		} catch (NumberFormatException e) {
			log.debug("[WfNotice] assignee 非用户ID（{}），跳过通知", assignee);
			return null;
		}
	}

	@Override
	public boolean isFailOnException() {
		// 通知属「尽力而为」语义：失败只记日志，不影响引擎事务
		return false;
	}

	@Override
	public boolean isFireOnTransactionLifecycleEvent() {
		return false;
	}

	@Override
	public String getOnTransaction() {
		return null;
	}
}
