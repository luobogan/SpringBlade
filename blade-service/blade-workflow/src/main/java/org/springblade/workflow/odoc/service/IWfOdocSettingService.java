package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.entity.WfOdocSetting;
import org.springblade.workflow.odoc.vo.OdocSealConfigVO;

/**
 * 公文通用设置服务（单例）。
 */
public interface IWfOdocSettingService {

	/** 取设置；无落库行时返回默认值（不落库） */
	WfOdocSetting get();

	/** 保存（单例 upsert） */
	WfOdocSetting save(WfOdocSetting setting);

	/** 无行时灌入默认设置，返回新增条数（0/1） */
	int seedIfAbsent();

	/**
	 * 取电子签章方案配置（业务表设置 + Nacos 第三方接入参数合并后的生效视图）。
	 */
	OdocSealConfigVO sealConfig();

	/**
	 * 保存电子签章方案配置（只更新签章相关列，不影响其它设置项）。
	 *
	 * <p>第三方 appId / appSecret 不入库，仍由 Nacos 维护。</p>
	 */
	OdocSealConfigVO saveSealConfig(OdocSealConfigVO config);

}
