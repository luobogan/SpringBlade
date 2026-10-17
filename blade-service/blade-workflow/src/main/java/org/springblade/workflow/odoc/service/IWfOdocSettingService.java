package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.entity.WfOdocSetting;

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
}
