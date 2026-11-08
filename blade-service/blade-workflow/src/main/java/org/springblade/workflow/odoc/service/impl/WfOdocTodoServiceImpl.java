package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.odoc.dto.OdocListQuery;
import org.springblade.workflow.dto.WfTaskListFilter;
import org.springblade.workflow.odoc.entity.WfOdocRequest;
import org.springblade.workflow.odoc.mapper.WfOdocRequestMapper;
import org.springblade.workflow.odoc.service.IWfOdocTodoService;
import org.springblade.workflow.odoc.vo.OdocTodoVO;
import org.springblade.workflow.service.IWfTaskService;
import org.springblade.workflow.vo.WfTaskVO;
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

	private final WfInstanceMapper instanceMapper;
	private final WfOdocRequestMapper requestMapper;
	private final IWfTaskService wfTaskService;

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
			rows.add(toVO(inst, reqMap.get(inst.getId())));
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

	/**
	 * 待办/已办：复用通用列表的混合分页（ACT 主表 + wf_task 扩展任务兜底，见 {@link WfTaskServiceImpl#page}），
	 * 经 {@code wf_odoc_request} 收敛公文维度（offical_type / secret_level 下推为 instance_id 范围），
	 * 公文属性批量回查镜像表。读源=act 时本方法不再直接查 {@code wf_task}（仅兜底残留仍回查）。
	 */
	private IPage<OdocTodoVO> pageTasks(OdocListQuery query, boolean todo) {
		Long target = WfAuthUtil.resolveSelfIfNotAdmin(query.getAssignee());
		// 公文维度：wf_odoc_request 的 instance_id 集合（offical_type / secret_level 过滤）
		List<Long> instIds = requestScopeIds(query);
		if (instIds.isEmpty()) {
			Page<OdocTodoVO> empty = new Page<>(query.getCurrent(), query.getSize());
			empty.setRecords(Collections.emptyList());
			empty.setTotal(0L);
			return empty;
		}
		WfTaskListFilter f = new WfTaskListFilter();
		f.setInstIds(instIds);
		f.setTitle(query.getTitle());
		f.setBeginTime(query.getBeginTime());
		f.setEndTime(query.getEndTime());
		IPage<WfTaskVO> page = todo
			? wfTaskService.todoPage(target, f, query.getCurrent(), query.getSize())
			: wfTaskService.donePage(target, f, query.getCurrent(), query.getSize());
		List<Long> ids = page.getRecords().stream()
			.map(WfTaskVO::getInstId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
		Map<Long, WfInstance> instMap = batchInstance(ids);
		Map<Long, WfOdocRequest> reqMap = batchRequest(ids);
		List<OdocTodoVO> rows = new ArrayList<>(page.getRecords().size());
		for (WfTaskVO v : page.getRecords()) {
			rows.add(toVO(v, instMap.get(v.getInstId()), reqMap.get(v.getInstId())));
		}
		Page<OdocTodoVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
		voPage.setRecords(rows);
		return voPage;
	}

	/** 从 WfTaskVO（混合分页结果）组装公文 VO，补实例与公文属性。 */
	private OdocTodoVO toVO(WfTaskVO v, WfInstance inst, WfOdocRequest req) {
		OdocTodoVO vo = new OdocTodoVO();
		if (v != null) {
			vo.setTaskId(v.getId());
			vo.setTaskStatus(v.getStatus());
			vo.setReceiveTime(v.getReceiveTime());
			vo.setOperateTime(v.getOperateTime());
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

	/** 公文维度实例范围：从 wf_odoc_request 取 instance_id（offical_type / secret_level 过滤）。 */
	private List<Long> requestScopeIds(OdocListQuery query) {
		com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WfOdocRequest> qw =
			Wrappers.<WfOdocRequest>lambdaQuery()
				.eq(WfOdocRequest::getIsDeleted, 0)
				.select(WfOdocRequest::getInstanceId);
		if (query.getOfficalType() != null && query.getOfficalType() > 0) {
			qw.eq(WfOdocRequest::getOfficalType, query.getOfficalType());
		} else {
			qw.gt(WfOdocRequest::getOfficalType, 0);
		}
		if (query.getSecretLevels() != null && !query.getSecretLevels().isEmpty()) {
			qw.in(WfOdocRequest::getSecretLevel, query.getSecretLevels());
		}
		List<Object> ids = requestMapper.selectObjs(qw);
		return ids.stream()
			.map(o -> o instanceof Number ? ((Number) o).longValue() : null)
			.filter(Objects::nonNull).distinct().collect(Collectors.toList());
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

	private OdocTodoVO toVO(WfInstance inst, WfOdocRequest req) {
		OdocTodoVO vo = new OdocTodoVO();
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
