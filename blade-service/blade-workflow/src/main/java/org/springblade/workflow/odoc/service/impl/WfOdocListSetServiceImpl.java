package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.odoc.entity.WfOdocListSet;
import org.springblade.workflow.odoc.mapper.WfOdocListSetMapper;
import org.springblade.workflow.odoc.service.IWfOdocListSetService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 公文列表列设置服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfOdocListSetServiceImpl implements IWfOdocListSetService {

	private final WfOdocListSetMapper listSetMapper;

	@Override
	public WfOdocListSet getByCode(String listCode) {
		if (!StringUtils.hasText(listCode)) {
			return null;
		}
		return listSetMapper.selectOne(Wrappers.<WfOdocListSet>lambdaQuery()
			.eq(WfOdocListSet::getListCode, listCode));
	}

	@Override
	public WfOdocListSet save(WfOdocListSet set) {
		if (set == null || !StringUtils.hasText(set.getListCode())) {
			throw new IllegalArgumentException("列表编码不能为空");
		}
		if (set.getEnabled() == null) {
			set.setEnabled(1);
		}
		WfOdocListSet exist = getByCode(set.getListCode());
		if (exist != null) {
			set.setId(exist.getId());
			listSetMapper.updateById(set);
			return set;
		}
		listSetMapper.insert(set);
		return set;
	}
}
