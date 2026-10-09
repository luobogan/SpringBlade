/**
 * Copyright (c) 2018-2099, Chill Zhuang 庄骞 (bladejava@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.system.dto.StatusFlowStartDTO;
import org.springblade.system.entity.PersonStatusFlow;
import org.springblade.system.entity.PersonStatusFlowRecord;
import org.springblade.system.enums.PersonStatusEnum;
import org.springblade.system.mapper.PersonStatusFlowMapper;
import org.springblade.system.mapper.PersonStatusFlowRecordMapper;
import org.springblade.system.mapper.UserMapper;
import org.springblade.system.service.IPersonStatusFlowService;
import org.springblade.system.user.entity.User;
import org.springblade.workflow.dto.StartProcessDTO;
import org.springblade.workflow.feign.IWorkflowClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 人员状态流转服务实现类（P3，对齐 ecology hrm_state_proc_set）
 * <p>
 * 状态变更不再直接改字段：命中配置且已绑定流程 → 发起 Flowable 审批，
 * 由 blade-workflow 审批完成时回调 {@link #callback} 才落库 {@code person_status}。
 * 未配置流程时降级为「直接变更」，保证功能不因流程缺失而不可用。
 *
 * @author Chill
 */
@Slf4j
@Service
public class PersonStatusFlowServiceImpl
	extends BaseServiceImpl<PersonStatusFlowMapper, PersonStatusFlow> implements IPersonStatusFlowService {

	/** 流程标题前缀：blade-workflow 的完成回调监听器据此粗筛，避免无关流程也跨服务回调 */
	public static final String TITLE_PREFIX = "PSF:";

	private final PersonStatusFlowRecordMapper recordMapper;
	private final UserMapper userMapper;
	private final IWorkflowClient workflowClient;

	public PersonStatusFlowServiceImpl(PersonStatusFlowRecordMapper recordMapper,
		UserMapper userMapper, IWorkflowClient workflowClient) {
		this.recordMapper = recordMapper;
		this.userMapper = userMapper;
		this.workflowClient = workflowClient;
	}

	@Override
	public R<String> start(StatusFlowStartDTO dto) {
		if (dto == null || Func.isEmpty(dto.getUserId()) || dto.getToStatus() == null) {
			return R.fail("用户与目标状态不能为空");
		}
		if (PersonStatusEnum.of(dto.getToStatus()) == null) {
			return R.fail("目标人员状态不合法");
		}
		User user = userMapper.selectById(dto.getUserId());
		if (user == null) {
			return R.fail("用户不存在");
		}
		Integer fromStatus = user.getPersonStatus() == null ? PersonStatusEnum.REGULAR.getCode() : user.getPersonStatus();
		if (Objects.equals(fromStatus, dto.getToStatus())) {
			return R.fail("目标状态与当前状态相同");
		}
		String tenantId = Func.toStr(SecureUtil.getTenantId(), "000000");
		PersonStatusFlow config = this.getOne(Wrappers.<PersonStatusFlow>lambdaQuery()
			.eq(PersonStatusFlow::getTenantId, tenantId)
			.eq(PersonStatusFlow::getFromStatus, fromStatus)
			.eq(PersonStatusFlow::getToStatus, dto.getToStatus())
			.eq(PersonStatusFlow::getStatus, 1));

		PersonStatusFlowRecord record = new PersonStatusFlowRecord();
		record.setTenantId(tenantId);
		record.setUserId(user.getId());
		record.setFromStatus(fromStatus);
		record.setToStatus(dto.getToStatus());
		record.setOpinion(dto.getOpinion());
		record.setStarter(SecureUtil.getUserId());

		if (config == null || Func.isBlank(config.getFlowKey())) {
			// 决策 3：无流程配置 → 降级直接变更，功能不因流程未配置而不可用
			record.setMode(PersonStatusFlowRecord.MODE_DIRECT);
			record.setFlowStatus(PersonStatusFlowRecord.FLOW_STATUS_APPROVED);
			record.setFinishTime(LocalDateTime.now());
			recordMapper.insert(record);
			applyPersonStatus(user.getId(), dto.getToStatus());
			return R.data("已直接变更为「" + PersonStatusEnum.descOf(dto.getToStatus()) + "」", "未配置审批流程，按直接变更办理");
		}

		record.setMode(PersonStatusFlowRecord.MODE_FLOW);
		record.setFlowStatus(PersonStatusFlowRecord.FLOW_STATUS_RUNNING);
		record.setFlowKey(config.getFlowKey());
		recordMapper.insert(record);

		StartProcessDTO startDto = new StartProcessDTO();
		startDto.setProcKey(config.getFlowKey());
		startDto.setTenantId(tenantId);
		startDto.setStarter(SecureUtil.getUserId());
		startDto.setTitle(TITLE_PREFIX + PersonStatusEnum.descOf(fromStatus) + "→"
			+ PersonStatusEnum.descOf(dto.getToStatus()) + "：" + user.getRealName());
		// 无表单场景：跳过 request_id 回填（formId 为 null 时引擎侧仅 warn，不影响流转）
		startDto.setBillInitiated(Boolean.TRUE);
		Map<String, Object> variables = new HashMap<>();
		variables.put("userId", user.getId());
		variables.put("fromStatus", fromStatus);
		variables.put("toStatus", dto.getToStatus());
		variables.put("approver", resolveApprover(user));
		variables.put("reason", Func.toStr(dto.getOpinion(), ""));
		// BPMN 排他网关的条件 ${wfOutcome == 'reject'} 会在流程推进到网关时求值；
		// 若该变量不存在，Flowable 直接抛 Unknown property（实测 500）。审批动作会覆盖它，
		// 这里按默认出口（审批通过）预置，保证发起人未产生审批动作时网关也能安全求值。
		variables.put("wfOutcome", "approve");
		startDto.setVariables(variables);

		R<String> startResult = workflowClient.startProcess(startDto);
		if (startResult == null || !startResult.isSuccess() || Func.isBlank(startResult.getData())) {
			log.warn("[PersonStatusFlow] 发起审批失败，已回滚流转记录. userId={}, flowKey={}, msg={}",
				user.getId(), config.getFlowKey(), startResult == null ? "null" : startResult.getMsg());
			recordMapper.deleteById(record.getId());
			return R.fail("发起审批失败：" + (startResult == null ? "流程服务不可用" : startResult.getMsg()));
		}
		record.setInstanceId(startResult.getData());
		recordMapper.updateById(record);

		// 竞态兜底：无表单 / 节点被自动推进时，流程可能在 startProcess 内部就同步走完，
		// PROCESS_COMPLETED 回调随之在「上面回填 instanceId」之前发生 —— 回调按 instanceId
		// 查不到流转记录会按幂等忽略（不视为失败），导致状态永远不生效。
		// 故回填后再确认一次：若实例已结束，就地补办一次回调（服务端按记录幂等）。
		try {
			R<Boolean> endedResult = workflowClient.isEnded(Long.valueOf(startResult.getData()));
			if (endedResult != null && endedResult.isSuccess() && Boolean.TRUE.equals(endedResult.getData())) {
				log.info("[PersonStatusFlow] 流程已同步走完，补办回调. instanceId={}", startResult.getData());
				this.callback(startResult.getData(), Boolean.TRUE, dto.getOpinion());
			}
		} catch (Exception e) {
			// 兜底失败不影响发起结果：仍有正常回调路径可补偿
			log.warn("[PersonStatusFlow] 校验流程是否已结束失败，忽略. instanceId={}, msg={}",
				startResult.getData(), e.getMessage());
		}

		return R.data(startResult.getData(), "已发起审批，通过后生效");
	}

	@Override
	public R<Boolean> callback(String instanceId, Boolean approved, String opinion) {
		if (Func.isBlank(instanceId)) {
			return R.fail("流程实例ID不能为空");
		}
		PersonStatusFlowRecord record = recordMapper.selectOne(Wrappers.<PersonStatusFlowRecord>lambdaQuery()
			.eq(PersonStatusFlowRecord::getInstanceId, instanceId)
			.last("LIMIT 1"));
		if (record == null) {
			// 非人员状态流转流程（或记录已清理）：幂等忽略，不视为失败
			log.info("[PersonStatusFlow] 回调未匹配到流转记录，忽略. instanceId={}", instanceId);
			return R.data(Boolean.FALSE, "未匹配到流转记录");
		}
		if (!Objects.equals(PersonStatusFlowRecord.FLOW_STATUS_RUNNING, record.getFlowStatus())) {
			// 已是终态：重复回调直接返回，保证幂等
			return R.data(Boolean.TRUE, "该流转已办结，重复回调忽略");
		}
		boolean pass = !Boolean.FALSE.equals(approved);
		if (pass) {
			applyPersonStatus(record.getUserId(), record.getToStatus());
			record.setFlowStatus(PersonStatusFlowRecord.FLOW_STATUS_APPROVED);
		} else {
			record.setFlowStatus(PersonStatusFlowRecord.FLOW_STATUS_REJECTED);
		}
		if (Func.isNotBlank(opinion)) {
			record.setOpinion(opinion);
		}
		record.setFinishTime(LocalDateTime.now());
		recordMapper.updateById(record);
		return R.data(Boolean.TRUE, pass ? "审批通过，状态已更新" : "审批驳回");
	}

	@Override
	public List<PersonStatusFlow> availableFlows(Long userId) {
		if (Func.isEmpty(userId)) {
			return List.of();
		}
		User user = userMapper.selectById(userId);
		if (user == null) {
			return List.of();
		}
		Integer fromStatus = user.getPersonStatus() == null ? PersonStatusEnum.REGULAR.getCode() : user.getPersonStatus();
		String tenantId = Func.toStr(SecureUtil.getTenantId(), "000000");
		return this.list(Wrappers.<PersonStatusFlow>lambdaQuery()
			.eq(PersonStatusFlow::getTenantId, tenantId)
			.eq(PersonStatusFlow::getFromStatus, fromStatus)
			.eq(PersonStatusFlow::getStatus, 1));
	}

	@Override
	public List<PersonStatusFlowRecord> records(Long userId) {
		if (Func.isEmpty(userId)) {
			return List.of();
		}
		return recordMapper.selectList(Wrappers.<PersonStatusFlowRecord>lambdaQuery()
			.eq(PersonStatusFlowRecord::getUserId, userId)
			.orderByDesc(PersonStatusFlowRecord::getCreateTime));
	}

	/** 审批人：优先直属主管，无主管时回退 admin（保证流程有人可办） */
	private Long resolveApprover(User user) {
		if (user.getManagerId() != null && user.getManagerId() > 0) {
			return user.getManagerId();
		}
		User admin = userMapper.selectOne(Wrappers.<User>lambdaQuery()
			.eq(User::getAccount, "admin")
			.last("LIMIT 1"));
		return admin == null ? SecureUtil.getUserId() : admin.getId();
	}

	/** 落库 person_status（仅改该列，避免覆盖其它字段） */
	private void applyPersonStatus(Long userId, Integer toStatus) {
		userMapper.update(null, Wrappers.<User>lambdaUpdate()
			.set(User::getPersonStatus, toStatus)
			.eq(User::getId, userId));
	}

}
