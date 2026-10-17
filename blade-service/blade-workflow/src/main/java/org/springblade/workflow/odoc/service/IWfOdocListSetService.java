package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.entity.WfOdocListSet;

/**
 * 公文列表列设置服务（按列表编码单例）。
 */
public interface IWfOdocListSetService {

	/** 按列表编码取（无则 null） */
	WfOdocListSet getByCode(String listCode);

	/** 保存（按 listCode 幂等 upsert） */
	WfOdocListSet save(WfOdocListSet set);
}
