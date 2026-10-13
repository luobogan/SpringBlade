package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.entity.WfOdocProcessSet;

/**
 * 公文流程建文设置服务（按流程定义存取，承载 L1/L2 流程级开关）。
 */
public interface IWfOdocProcessSetService {

	/** 按流程定义取设置（无则返回 null） */
	WfOdocProcessSet getByDefId(Long defId);

	/**
	 * 保存/更新流程设置：按 defId 幂等（已存在则更新，否则新建）。
	 */
	WfOdocProcessSet save(WfOdocProcessSet set);
}
