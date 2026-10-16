package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.odoc.dto.OdocListQuery;
import org.springblade.workflow.odoc.entity.WfOdocRequest;
import org.springblade.workflow.odoc.mapper.WfOdocRequestMapper;
import org.springblade.workflow.odoc.service.IWfOdocReportService;
import org.springblade.workflow.odoc.vo.OdocStatisticVO;
import org.springblade.workflow.odoc.vo.OdocTodoVO;
import org.springblade.workflow.utils.WfAuthUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WfOdocReportServiceImpl implements IWfOdocReportService {

	private final WfInstanceMapper instanceMapper;
	private final WfOdocRequestMapper requestMapper;

	@Override
	public List<OdocStatisticVO> statistics(Integer officalType) {
		// 预置 1发文/2收文/3签报 三个桶，保证无数据也返回完整维度
		Map<Integer, OdocStatisticVO> byType = new LinkedHashMap<>();
		for (int ot : new int[]{1, 2, 3}) {
			OdocStatisticVO v = new OdocStatisticVO();
			v.setOfficalType(ot);
			byType.put(ot, v);
		}
		for (Map<String, Object> row : requestMapper.statByOfficalType()) {
			Integer ot = toInt(row.get("officalType"));
			OdocStatisticVO v = byType.get(ot);
			if (v == null) {
				continue;
			}
			long cnt = toLong(row.get("cnt"));
			int st = toInt(row.get("status")) == null ? -1 : toInt(row.get("status"));
			v.setTotal(v.getTotal() + cnt);
			switch (st) {
				case WfInstance.STATUS_RUNNING:
					v.setRunning(v.getRunning() + cnt);
					break;
				case WfInstance.STATUS_APPROVED:
					v.setApproved(v.getApproved() + cnt);
					break;
				case WfInstance.STATUS_REJECTED:
					v.setRejected(v.getRejected() + cnt);
					break;
				case WfInstance.STATUS_CANCELED:
					v.setCanceled(v.getCanceled() + cnt);
					break;
				case WfInstance.STATUS_SUSPENDED:
					v.setSuspended(v.getSuspended() + cnt);
					break;
				case WfInstance.STATUS_DRAFT:
					v.setDraft(v.getDraft() + cnt);
					break;
				default:
					break;
			}
		}
		List<OdocStatisticVO> result = new ArrayList<>(byType.values());
		if (officalType != null && officalType > 0) {
			final int ot = officalType;
			result.removeIf(v -> v.getOfficalType() == null || v.getOfficalType() != ot);
		}
		return result;
	}

	@Override
	public IPage<OdocTodoVO> monitorPage(OdocListQuery query) {
		if (!WfAuthUtil.isAdmin()) {
			throw new ServiceException("仅流程管理员可查看公文监控");
		}
		LambdaQueryWrapper<WfInstance> qw = Wrappers.<WfInstance>lambdaQuery()
			.eq(WfInstance::getStatus, WfInstance.STATUS_RUNNING)
			.eq(WfInstance::getIsTest, 0);
		if (StringUtils.hasText(query.getTitle())) {
			qw.like(WfInstance::getTitle, query.getTitle());
		}
		StringBuilder scope = new StringBuilder("select instance_id from wf_odoc_request where is_deleted = 0");
		if (query.getOfficalType() != null && query.getOfficalType() > 0) {
			scope.append(" and offical_type = ").append(query.getOfficalType());
		} else {
			scope.append(" and offical_type > 0");
		}
		qw.inSql(WfInstance::getId, scope.toString());
		qw.orderByDesc(WfInstance::getStartTime);

		Page<WfInstance> page = instanceMapper.selectPage(pageOf(query), qw);
		List<Long> instIds = page.getRecords().stream()
			.map(WfInstance::getId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
		Map<Long, WfOdocRequest> reqMap = instIds.isEmpty() ? Collections.emptyMap()
			: requestMapper.selectList(Wrappers.<WfOdocRequest>lambdaQuery()
				.in(WfOdocRequest::getInstanceId, instIds).eq(WfOdocRequest::getIsDeleted, 0))
				.stream().collect(Collectors.toMap(WfOdocRequest::getInstanceId, Function.identity(), (a, b) -> a));

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

	private OdocTodoVO toVO(WfInstance inst, WfOdocRequest req) {
		OdocTodoVO vo = new OdocTodoVO();
		vo.setInstanceId(inst.getId());
		vo.setDefId(inst.getDefId());
		vo.setTitle(inst.getTitle());
		vo.setCurrentNodeKey(inst.getCurrentNodeKey());
		vo.setStarter(inst.getStarter());
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

	private static Integer toInt(Object o) {
		return o instanceof Number ? ((Number) o).intValue() : null;
	}

	private static long toLong(Object o) {
		return o instanceof Number ? ((Number) o).longValue() : 0L;
	}

}
