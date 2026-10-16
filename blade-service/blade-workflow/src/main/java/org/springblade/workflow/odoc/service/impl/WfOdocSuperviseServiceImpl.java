package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.odoc.dto.OdocSuperviseQuery;
import org.springblade.workflow.odoc.dto.OdocSuperviseSaveDTO;
import org.springblade.workflow.odoc.entity.WfOdocRequest;
import org.springblade.workflow.odoc.entity.WfOdocSupervise;
import org.springblade.workflow.odoc.entity.WfOdocSuperviseSetting;
import org.springblade.workflow.odoc.mapper.WfOdocRequestMapper;
import org.springblade.workflow.odoc.mapper.WfOdocSuperviseMapper;
import org.springblade.workflow.odoc.mapper.WfOdocSuperviseSettingMapper;
import org.springblade.workflow.odoc.service.IWfOdocSuperviseService;
import org.springblade.workflow.odoc.vo.OdocSuperviseVO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WfOdocSuperviseServiceImpl implements IWfOdocSuperviseService {

	private final WfOdocSuperviseMapper superviseMapper;
	private final WfOdocSuperviseSettingMapper settingMapper;
	private final WfInstanceMapper instanceMapper;
	private final WfOdocRequestMapper requestMapper;

	@Override
	public WfOdocSuperviseSetting getSetting(Long defId) {
		if (defId == null) {
			return null;
		}
		return settingMapper.selectOne(Wrappers.<WfOdocSuperviseSetting>lambdaQuery()
			.eq(WfOdocSuperviseSetting::getDefId, defId));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public WfOdocSuperviseSetting saveSetting(WfOdocSuperviseSetting setting) {
		if (setting == null || setting.getDefId() == null) {
			throw new ServiceException("流程定义ID不能为空");
		}
		WfOdocSuperviseSetting exist = getSetting(setting.getDefId());
		if (exist != null) {
			setting.setId(exist.getId());
			settingMapper.updateById(setting);
			return setting;
		}
		setting.setId(null);
		settingMapper.insert(setting);
		return setting;
	}

	@Override
	public IPage<OdocSuperviseVO> page(OdocSuperviseQuery query) {
		Long me = SecureUtil.getUserId();
		boolean supervised = query.getSuperviseType() != null && query.getSuperviseType() == 1;
		LambdaQueryWrapper<WfOdocSupervise> qw = Wrappers.<WfOdocSupervise>lambdaQuery()
			.eq(supervised ? WfOdocSupervise::getStarterId : WfOdocSupervise::getSupervisorId, me)
			.eq(WfOdocSupervise::getIsTest, 0);
		if (query.getOfficalType() != null && query.getOfficalType() > 0) {
			qw.eq(WfOdocSupervise::getOfficalType, query.getOfficalType());
		}
		if (query.getStatus() != null) {
			qw.eq(WfOdocSupervise::getSuperviseStatus, query.getStatus());
		}
		qw.orderByDesc(WfOdocSupervise::getCreateTime);
		Page<WfOdocSupervise> page = superviseMapper.selectPage(pageOf(query), qw);

		List<Long> instIds = page.getRecords().stream()
			.map(WfOdocSupervise::getInstanceId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
		Map<Long, WfInstance> instMap = instIds.isEmpty() ? Collections.emptyMap()
			: instanceMapper.selectList(Wrappers.<WfInstance>lambdaQuery().in(WfInstance::getId, instIds))
				.stream().collect(Collectors.toMap(WfInstance::getId, Function.identity(), (a, b) -> a));
		Map<Long, WfOdocRequest> reqMap = instIds.isEmpty() ? Collections.emptyMap()
			: requestMapper.selectList(Wrappers.<WfOdocRequest>lambdaQuery()
				.in(WfOdocRequest::getInstanceId, instIds).eq(WfOdocRequest::getIsDeleted, 0))
				.stream().collect(Collectors.toMap(WfOdocRequest::getInstanceId, Function.identity(), (a, b) -> a));

		List<OdocSuperviseVO> rows = new ArrayList<>(page.getRecords().size());
		for (WfOdocSupervise s : page.getRecords()) {
			rows.add(toVO(s, instMap.get(s.getInstanceId()), reqMap.get(s.getInstanceId())));
		}
		Page<OdocSuperviseVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
		voPage.setRecords(rows);
		return voPage;
	}

	@Override
	public Long count(OdocSuperviseQuery query) {
		Long me = SecureUtil.getUserId();
		boolean supervised = query.getSuperviseType() != null && query.getSuperviseType() == 1;
		LambdaQueryWrapper<WfOdocSupervise> qw = Wrappers.<WfOdocSupervise>lambdaQuery()
			.eq(supervised ? WfOdocSupervise::getStarterId : WfOdocSupervise::getSupervisorId, me)
			.eq(WfOdocSupervise::getIsTest, 0)
			.eq(WfOdocSupervise::getSuperviseStatus, WfOdocSupervise.STATUS_DOING);
		if (query.getOfficalType() != null && query.getOfficalType() > 0) {
			qw.eq(WfOdocSupervise::getOfficalType, query.getOfficalType());
		}
		return superviseMapper.selectCount(qw);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public WfOdocSupervise add(OdocSuperviseSaveDTO dto) {
		if (dto == null || dto.getInstanceId() == null) {
			throw new ServiceException("流程实例ID不能为空");
		}
		WfInstance inst = instanceMapper.selectById(dto.getInstanceId());
		if (inst == null) {
			throw new ServiceException("流程实例不存在");
		}
		Long me = SecureUtil.getUserId();
		Long supervisorId = dto.getSupervisorId() != null ? dto.getSupervisorId() : me;

		Integer officalType = null;
		WfOdocRequest req = requestMapper.selectOne(Wrappers.<WfOdocRequest>lambdaQuery()
			.eq(WfOdocRequest::getInstanceId, inst.getId()).eq(WfOdocRequest::getIsDeleted, 0));
		if (req != null) {
			officalType = req.getOfficalType();
		}

		WfOdocSupervise s = new WfOdocSupervise();
		s.setInstanceId(inst.getId());
		s.setDefId(inst.getDefId());
		s.setOfficalType(officalType);
		s.setSupervisorId(supervisorId);
		s.setStarterId(inst.getStarter());
		s.setSuperviseStatus(WfOdocSupervise.STATUS_DOING);
		s.setDeadline(dto.getDeadline());
		s.setRemark(dto.getRemark());
		s.setIsTest(0);
		superviseMapper.insert(s);
		return s;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remind(Long id) {
		WfOdocSupervise s = superviseMapper.selectById(id);
		if (s == null) {
			throw new ServiceException("督办记录不存在");
		}
		s.setLastRemindTime(LocalDateTime.now());
		superviseMapper.updateById(s);
		// 站内/消息通知为后续增强（复用 INoticeClient）；此处先落提醒时间
		return true;
	}

	// ------------------------------------------------------------------ 私有

	private <T> Page<T> pageOf(OdocSuperviseQuery query) {
		long cur = query.getCurrent() == null || query.getCurrent() < 1 ? 1 : query.getCurrent();
		long size = query.getSize() == null || query.getSize() < 1 ? 10 : query.getSize();
		return new Page<>(cur, size);
	}

	private OdocSuperviseVO toVO(WfOdocSupervise s, WfInstance inst, WfOdocRequest req) {
		OdocSuperviseVO vo = new OdocSuperviseVO();
		vo.setId(s.getId());
		vo.setInstanceId(s.getInstanceId());
		vo.setDefId(s.getDefId());
		vo.setOfficalType(s.getOfficalType());
		vo.setSupervisorId(s.getSupervisorId());
		vo.setStarterId(s.getStarterId());
		vo.setStatus(s.getSuperviseStatus());
		vo.setDeadline(s.getDeadline());
		vo.setRemark(s.getRemark());
		vo.setLastRemindTime(s.getLastRemindTime());
		vo.setCreateTime(s.getCreateTime());
		if (inst != null) {
			vo.setTitle(inst.getTitle());
		}
		if (req != null) {
			vo.setIssuedNum(req.getIssuedNum());
			if (vo.getOfficalType() == null) {
				vo.setOfficalType(req.getOfficalType());
			}
		}
		return vo;
	}

}
