package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.odoc.entity.WfOdocProcessSet;
import org.springblade.workflow.odoc.mapper.WfOdocProcessSetMapper;
import org.springblade.workflow.odoc.service.IWfOdocProcessSetService;
import org.springframework.stereotype.Service;

/**
 * 流程建文设置服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfOdocProcessSetServiceImpl implements IWfOdocProcessSetService {

	private final WfOdocProcessSetMapper processSetMapper;

	@Override
	public WfOdocProcessSet getByDefId(Long defId) {
		return processSetMapper.selectOne(
			Wrappers.<WfOdocProcessSet>lambdaQuery().eq(WfOdocProcessSet::getDefId, defId));
	}

	@Override
	public WfOdocProcessSet save(WfOdocProcessSet set) {
		if (set == null || set.getDefId() == null) {
			throw new IllegalArgumentException("流程定义ID不能为空");
		}
		WfOdocProcessSet exist = getByDefId(set.getDefId());
		if (exist != null) {
			set.setId(exist.getId());
			processSetMapper.updateById(set);
			return set;
		}
		processSetMapper.insert(set);
		return set;
	}
}
