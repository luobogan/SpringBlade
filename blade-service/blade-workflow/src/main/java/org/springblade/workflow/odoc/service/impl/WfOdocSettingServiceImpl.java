package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.odoc.entity.WfOdocSetting;
import org.springblade.workflow.odoc.mapper.WfOdocSettingMapper;
import org.springblade.workflow.odoc.service.IWfOdocSettingService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公文通用设置服务实现（单例）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfOdocSettingServiceImpl implements IWfOdocSettingService {

	private final WfOdocSettingMapper settingMapper;

	@Override
	public WfOdocSetting get() {
		WfOdocSetting exist = first();
		return exist != null ? exist : defaults();
	}

	@Override
	public WfOdocSetting save(WfOdocSetting setting) {
		if (setting == null) {
			throw new IllegalArgumentException("设置不能为空");
		}
		WfOdocSetting exist = first();
		if (exist != null) {
			setting.setId(exist.getId());
			settingMapper.updateById(setting);
			return setting;
		}
		settingMapper.insert(setting);
		return setting;
	}

	@Override
	public int seedIfAbsent() {
		if (first() != null) {
			return 0;
		}
		settingMapper.insert(defaults());
		log.info("公文通用设置默认值灌入完成");
		return 1;
	}

	private WfOdocSetting first() {
		List<WfOdocSetting> list = settingMapper.selectList(Wrappers.<WfOdocSetting>lambdaQuery()
			.last("limit 1"));
		return list != null && !list.isEmpty() ? list.get(0) : null;
	}

	private WfOdocSetting defaults() {
		WfOdocSetting d = new WfOdocSetting();
		d.setIsOpenOfd(0);
		d.setDefaultLayoutFormat(0);
		d.setDefaultMustTaohong(1);
		d.setDefaultSealRequired(0);
		return d;
	}
}
