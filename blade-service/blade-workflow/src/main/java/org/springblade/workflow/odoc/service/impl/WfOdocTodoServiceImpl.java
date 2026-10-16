package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.odoc.dto.OdocListQuery;
import org.springblade.workflow.odoc.entity.WfOdocRequest;
import org.springblade.workflow.odoc.mapper.WfOdocRequestMapper;
import org.springblade.workflow.odoc.service.IWfOdocTodoService;
import org.springblade.workflow.odoc.vo.OdocTodoVO;
import org.springblade.workflow.utils.WfAuthUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 公文待办 / 已办 / 我的请求 列表实现。
 *
 * <p>复用 {@code wf_task}/{@code wf_instance}（与通用 WfTaskServiceImpl 同数据源），
 * 经 {@code wf_odoc_request.offical_type} 收敛为公文 + 发文/收文/签报维度；
 * 公文属性批量回查镜像表，杜绝 N+1。办理/退回等动作复用通用 {@code /task/**} 接口，不在此重复造。</p>
 */
@Service
@RequiredArgsConstructor
public class WfOdocTodoServiceImpl implements IWfOdocTodoService {

	private final WfTaskMapper taskMapper;
	private final WfInstanceMapper instanceMapper;
	private final WfOdocRequestMapper requestMapper;

	@Override
	public IPage<OdocTodoVO> pageTodo(OdocListQuery query) {
		return pageTasks(query, true);
	}

	@Override
	public IPage<OdocTodoVO> pageDone(OdocListQuery query) {
		return pageTasks(query, false);
	}

	@Override
	public IPage<OdocTodoVO> pageMyRequest(OdocListQuery query) {
		Long target = WfAuthUtil.resolveSelfIfNotAdmin(query.getAssignee());
		LambdaQueryWrapper<WfInstance> qw = Wrappers.<WfInstance>lambdaQuery()
			.eq(WfInstance::getStarter, target)
			.eq(WfInstance::getIsTest, 0);
		if (StringUtils.hasText(query.getTitle())) {
			qw.like(WfInstance::getTitle, query.getTitle());
		}
		if (query.getOfficalType() != null && query.getOfficalType() > 0) {
			qw.inSql(WfInstance::getId, requestScopeSql(query));
		}
		qw.orderByDesc(WfInstance::getStartTime);
		Page<WfInstance> page = instanceMapper.selectPage(pageOf(query), qw);

		List<Long> instIds = page.getRecords().stream()
			.map(WfInstance::getId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
		Map<Long, WfOdocRequest> reqMap = batchRequest(instIds);
		List<OdocTodoVO> rows = new ArrayList<>(page.getRecords().size());
		for (WfInstance inst : page.getRecords()) {
			rows.add(toVO(null, inst, reqMap.get(inst.getId())));
		}
		Page<OdocTodoVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
		voPage.setRecords(rows);
		return voPage;
	}

	// ------------------------------------------------------------------ 私有

	private <T> Page<T> pageOf(OdocListQuery query) {
		long cur = query.getCurrent() == null || query.getCurrent() < 1 ? 1 : query.getCurrent();
		long size = query.getSize() == null || query.getSize() < 1 ? 10 : query.getSize();
		return new Page<>(cur, size);
	}

	/** 待办/已办：分页查 wf_task，再批量回查实例与公文属性。 */
	private IPage<OdocTodoVO> pageTasks(OdocListQuery query, boolean todo) {
		Long target = WfAuthUtil.resolveSelfIfNotAdmin(query.getAssignee());
		List<Integer> statuses = todo
			? List.of(WfTask.STATUS_TODO)
			: List.of(WfTask.STATUS_DONE, WfTask.STATUS_FINISHED, WfTask.STATUS_AUTO_SUBMIT,
				WfTask.STATUS_COADJUTANT, WfTask.STATUS_CIRCULATE, WfTask.STATUS_READ);

		LambdaQueryWrapper<WfTask> qw = Wrappers.<WfTask>lambdaQuery()
			.eq(WfTask::getAssignee, target)
			.in(WfTask::getStatus, statuses)
			.eq(WfTask::getIsTest, 0);
		qw.inSql(WfTask::getInstId, requestScopeSql(query));
		applyTitleScopeTask(qw, query);
		if (query.getBeginTime() != null) {
			qw.ge(todo ? WfTask::getReceiveTime : WfTask::getOperateTime, query.getBeginTime());
		}
		if (query.getEndTime() != null) {
			qw.le(todo ? WfTask::getReceiveTime : WfTask::getOperateTime, query.getEndTime());
		}
		qw.orderByDesc(todo ? WfTask::getReceiveTime : WfTask::getOperateTime);

		Page<WfTask> page = taskMapper.selectPage(pageOf(query), qw);
		List<Long> instIds = page.getRecords().stream()
			.map(WfTask::getInstId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
		Map<Long, WfInstance> instMap = batchInstance(instIds);
		Map<Long, WfOdocRequest> reqMap = batchRequest(instIds);

		List<OdocTodoVO> rows = new ArrayList<>(page.getRecords().size());
		for (WfTask t : page.getRecords()) {
			rows.add(toVO(t, instMap.get(t.getInstId()), reqMap.get(t.getInstId())));
		}
		Page<OdocTodoVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
		voPage.setRecords(rows);
		return voPage;
	}

	private void applyTitleScopeTask(LambdaQueryWrapper<WfTask> qw, OdocListQuery query) {
		if (!StringUtils.hasText(query.getTitle())) {
			return;
		}
		List<Long> instIds = instanceMapper.selectObjs(Wrappers.<WfInstance>lambdaQuery()
				.select(WfInstance::getId)
				.like(WfInstance::getTitle, query.getTitle())
				.eq(WfInstance::getIsTest, 0))
			.stream()
			.map(o -> o instanceof Number ? ((Number) o).longValue() : null)
			.filter(Objects::nonNull).distinct().collect(Collectors.toList());
		if (instIds.isEmpty()) {
			qw.eq(WfTask::getId, -1L); // 无命中实例 → 恒不成立，避免全表
			return;
		}
		qw.in(WfTask::getInstId, instIds);
	}

	/**
	 * 公文维度子查询 SQL（原始串，供 inSql 使用）。officalType 必带（空=全部公文 offical_type>0）；密级可选。
	 */
	private String requestScopeSql(OdocListQuery query) {
		StringBuilder sb = new StringBuilder("select instance_id from wf_odoc_request where is_deleted = 0");
		if (query.getOfficalType() != null && query.getOfficalType() > 0) {
			sb.append(" and offical_type = ").append(query.getOfficalType());
		} else {
			sb.append(" and offical_type > 0");
		}
		if (query.getSecretLevels() != null && !query.getSecretLevels().isEmpty()) {
			String in = query.getSecretLevels().stream()
				.filter(Objects::nonNull)
				.map(s -> "'" + s.replace("'", "''") + "'")
				.collect(Collectors.joining(","));
			if (!in.isEmpty()) {
				sb.append(" and secret_level in (").append(in).append(")");
			}
		}
		return sb.toString();
	}

	private Map<Long, WfInstance> batchInstance(List<Long> instIds) {
		if (instIds.isEmpty()) {
			return Collections.emptyMap();
		}
		return instanceMapper.selectList(Wrappers.<WfInstance>lambdaQuery().in(WfInstance::getId, instIds))
			.stream().collect(Collectors.toMap(WfInstance::getId, Function.identity(), (a, b) -> a));
	}

	private Map<Long, WfOdocRequest> batchRequest(List<Long> instIds) {
		if (instIds.isEmpty()) {
			return Collections.emptyMap();
		}
		return requestMapper.selectList(Wrappers.<WfOdocRequest>lambdaQuery()
				.in(WfOdocRequest::getInstanceId, instIds)
				.eq(WfOdocRequest::getIsDeleted, 0))
			.stream().collect(Collectors.toMap(WfOdocRequest::getInstanceId, Function.identity(), (a, b) -> a));
	}

	private OdocTodoVO toVO(WfTask task, WfInstance inst, WfOdocRequest req) {
		OdocTodoVO vo = new OdocTodoVO();
		if (task != null) {
			vo.setTaskId(task.getId());
			vo.setTaskStatus(task.getStatus());
			vo.setReceiveTime(task.getReceiveTime());
			vo.setOperateTime(task.getOperateTime());
		}
		if (inst != null) {
			vo.setInstanceId(inst.getId());
			vo.setDefId(inst.getDefId());
			vo.setTitle(inst.getTitle());
			vo.setCurrentNodeKey(inst.getCurrentNodeKey());
			vo.setStarter(inst.getStarter());
		}
		if (req != null) {
			vo.setOfficalType(req.getOfficalType());
			vo.setIssuedNum(req.getIssuedNum());
			vo.setReceiveNum(req.getReceiveNum());
			vo.setIssuedUnit(req.getIssuedUnit());
			vo.setReceiveUnit(req.getReceiveUnit());
			vo.setSecretLevel(req.getSecretLevel());
			vo.setUrgency(req.getUrgency());
			vo.setTopicType(req.getTopicType());
			vo.setWrittenDate(req.getWrittenDate());
			vo.setIssuedDate(req.getIssuedDate());
			vo.setDocId(req.getDocId());
		}
		return vo;
	}

}
