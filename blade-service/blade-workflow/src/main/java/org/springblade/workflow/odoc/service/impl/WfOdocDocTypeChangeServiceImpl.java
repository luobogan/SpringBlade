package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.odoc.dto.OdocDocTypeChangeSubmitDTO;
import org.springblade.workflow.odoc.entity.WfOdocDocTypeChange;
import org.springblade.workflow.odoc.entity.WfOdocDocTypeChangeRevision;
import org.springblade.workflow.odoc.entity.WfOdocProcessSet;
import org.springblade.workflow.odoc.entity.WfOdocRequest;
import org.springblade.workflow.odoc.mapper.WfOdocDocTypeChangeMapper;
import org.springblade.workflow.odoc.mapper.WfOdocDocTypeChangeRevisionMapper;
import org.springblade.workflow.odoc.mapper.WfOdocProcessSetMapper;
import org.springblade.workflow.odoc.mapper.WfOdocRequestMapper;
import org.springblade.workflow.odoc.service.IWfOdocDocTypeChangeService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WfOdocDocTypeChangeServiceImpl implements IWfOdocDocTypeChangeService {

	/** 不限制变更次数 */
	private static final int CHANGE_NUMBER_UNLIMITED = 1;
	/** 仅允许变更一次 */
	private static final int CHANGE_NUMBER_ONCE = 2;
	/** 自定义次数 */
	private static final int CHANGE_NUMBER_CUSTOM = 3;

	private final WfOdocDocTypeChangeMapper changeMapper;
	private final WfOdocDocTypeChangeRevisionMapper revisionMapper;
	private final WfOdocProcessSetMapper processSetMapper;
	private final WfInstanceMapper instanceMapper;
	private final WfOdocRequestMapper requestMapper;

	@Override
	public List<WfOdocDocTypeChange> listByDefId(Long defId) {
		if (defId == null) {
			return List.of();
		}
		return changeMapper.selectList(Wrappers.<WfOdocDocTypeChange>lambdaQuery()
			.eq(WfOdocDocTypeChange::getDefId, defId)
			.orderByDesc(WfOdocDocTypeChange::getIsDefault));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public WfOdocDocTypeChange save(WfOdocDocTypeChange config) {
		if (config == null || config.getDefId() == null || config.getTargetDefId() == null) {
			throw new ServiceException("源流程与目标流程不能为空");
		}
		WfOdocDocTypeChange exist = changeMapper.selectOne(Wrappers.<WfOdocDocTypeChange>lambdaQuery()
			.eq(WfOdocDocTypeChange::getDefId, config.getDefId())
			.eq(WfOdocDocTypeChange::getTargetDefId, config.getTargetDefId()));
		if (exist != null) {
			config.setId(exist.getId());
			changeMapper.updateById(config);
			return config;
		}
		config.setId(null);
		changeMapper.insert(config);
		return config;
	}

	@Override
	public boolean remove(Long id) {
		return changeMapper.deleteById(id) > 0;
	}

	@Override
	public WfOdocProcessSet getAssignment(Long defId) {
		if (defId == null) {
			return null;
		}
		return processSetMapper.selectOne(Wrappers.<WfOdocProcessSet>lambdaQuery()
			.eq(WfOdocProcessSet::getDefId, defId));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public WfOdocProcessSet saveAssignment(WfOdocProcessSet set) {
		if (set == null || set.getDefId() == null) {
			throw new ServiceException("流程定义ID不能为空");
		}
		WfOdocProcessSet exist = getAssignment(set.getDefId());
		if (exist != null) {
			exist.setDocTypeChangeNumber(set.getDocTypeChangeNumber());
			exist.setDocTypeChangeTimes(set.getDocTypeChangeTimes());
			processSetMapper.updateById(exist);
			return exist;
		}
		WfOdocProcessSet created = new WfOdocProcessSet();
		created.setDefId(set.getDefId());
		created.setDocTypeChangeNumber(set.getDocTypeChangeNumber());
		created.setDocTypeChangeTimes(set.getDocTypeChangeTimes());
		processSetMapper.insert(created);
		return created;
	}

	@Override
	public List<WfOdocDocTypeChangeRevision> revisions(Long instanceId) {
		if (instanceId == null) {
			return List.of();
		}
		return revisionMapper.selectList(Wrappers.<WfOdocDocTypeChangeRevision>lambdaQuery()
			.eq(WfOdocDocTypeChangeRevision::getInstanceId, instanceId)
			.orderByDesc(WfOdocDocTypeChangeRevision::getCreateTime));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public WfOdocDocTypeChangeRevision submit(OdocDocTypeChangeSubmitDTO dto) {
		if (dto == null || dto.getInstanceId() == null || dto.getTargetDefId() == null) {
			throw new ServiceException("实例ID与目标流程不能为空");
		}
		WfInstance inst = instanceMapper.selectById(dto.getInstanceId());
		if (inst == null) {
			throw new ServiceException("流程实例不存在");
		}
		WfOdocDocTypeChange cfg = changeMapper.selectOne(Wrappers.<WfOdocDocTypeChange>lambdaQuery()
			.eq(WfOdocDocTypeChange::getDefId, inst.getDefId())
			.eq(WfOdocDocTypeChange::getTargetDefId, dto.getTargetDefId()));
		if (cfg == null) {
			throw new ServiceException("未配置到该目标文种的转换");
		}

		// 变更次数校验（对应 E9 getIsDocTypeChange：未配置/次数用尽则不允许）
		WfOdocProcessSet set = getAssignment(inst.getDefId());
		int changeNumber = set == null || set.getDocTypeChangeNumber() == null ? 0 : set.getDocTypeChangeNumber();
		long used = revisionMapper.selectCount(Wrappers.<WfOdocDocTypeChangeRevision>lambdaQuery()
			.eq(WfOdocDocTypeChangeRevision::getInstanceId, inst.getId()));
		boolean allowed;
		if (changeNumber == CHANGE_NUMBER_UNLIMITED) {
			allowed = true;
		} else if (changeNumber == CHANGE_NUMBER_ONCE) {
			allowed = used == 0;
		} else if (changeNumber == CHANGE_NUMBER_CUSTOM) {
			int times = set.getDocTypeChangeTimes() == null ? 0 : set.getDocTypeChangeTimes();
			allowed = used < times;
		} else {
			allowed = false; // 未开启文种变更
		}
		if (!allowed) {
			throw new ServiceException("文种变更次数已用完或未开启文种变更");
		}

		WfOdocDocTypeChangeRevision rev = new WfOdocDocTypeChangeRevision();
		rev.setInstanceId(inst.getId());
		rev.setDefId(inst.getDefId());
		rev.setTargetDefId(dto.getTargetDefId());
		rev.setTargetOfficalType(cfg.getTargetOfficalType());
		rev.setReasons(dto.getReasons());
		rev.setOperatorId(SecureUtil.getUserId());
		rev.setIsTest(0);
		revisionMapper.insert(rev);

		// 更新公文镜像的 officalType（引擎级重路由/起新实例为后续增强，见方案 §14.5）
		if (cfg.getTargetOfficalType() != null) {
			requestMapper.update(null, Wrappers.<WfOdocRequest>lambdaUpdate()
				.set(WfOdocRequest::getOfficalType, cfg.getTargetOfficalType())
				.eq(WfOdocRequest::getInstanceId, inst.getId())
				.eq(WfOdocRequest::getIsDeleted, 0));
		}
		return rev;
	}

}
