package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.odoc.entity.WfOdocSetting;
import org.springblade.workflow.odoc.mapper.WfOdocSettingMapper;
import org.springblade.workflow.odoc.seal.IOdocSealStrategy;
import org.springblade.workflow.odoc.seal.OdocSealConfig;
import org.springblade.workflow.odoc.service.IWfOdocSettingService;
import org.springblade.workflow.odoc.vo.OdocSealConfigVO;
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

	private final OdocSealConfig sealConfig;

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

	@Override
	public OdocSealConfigVO sealConfig() {
		WfOdocSetting setting = get();
		OdocSealConfigVO vo = new OdocSealConfigVO();
		String vendor = sealConfig.mode(setting);
		vo.setVendor(vendor);
		vo.setSealMode(IOdocSealStrategy.MODE_ESIGN.equals(vendor)
			? OdocSealConfig.MODE_ESIGN_VALUE : OdocSealConfig.MODE_LOCAL_VALUE);
		vo.setEsignOrgId(sealConfig.esignOrgId(setting));
		vo.setEsignDefaultSealId(sealConfig.esignDefaultSealId(setting));
		vo.setEsignNotifyUrl(sealConfig.esignNotifyUrl(setting));
		vo.setEsignEnabled(sealConfig.esignEnabled());
		vo.setEsignReady(sealConfig.esignReady());
		vo.setEsignHost(sealConfig.esignHost());
		vo.setEsignAppIdMasked(sealConfig.maskedAppId());
		return vo;
	}

	@Override
	public OdocSealConfigVO saveSealConfig(OdocSealConfigVO config) {
		if (config == null || config.getSealMode() == null) {
			throw new ServiceException("请选择签章方案");
		}
		if (config.getSealMode() == OdocSealConfig.MODE_ESIGN_VALUE && !sealConfig.esignReady()) {
			throw new ServiceException("第三方电子签章尚未就绪，请先在 Nacos 配置 odoc.esign.enabled / app-id / app-secret");
		}
		WfOdocSetting exist = first();
		if (exist == null) {
			exist = defaults();
			settingMapper.insert(exist);
		}
		exist.setSealMode(config.getSealMode());
		exist.setSealEsignOrgId(trimToEmpty(config.getEsignOrgId()));
		exist.setSealEsignDefaultSealId(trimToEmpty(config.getEsignDefaultSealId()));
		exist.setSealEsignNotifyUrl(trimToEmpty(config.getEsignNotifyUrl()));
		settingMapper.updateById(exist);
		log.info("公文电子签章方案已更新 sealMode={}", config.getSealMode());
		return sealConfig();
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
		d.setSealMode(OdocSealConfig.MODE_LOCAL_VALUE);
		return d;
	}

	private String trimToEmpty(String v) {
		return v == null ? "" : v.trim();
	}
}
