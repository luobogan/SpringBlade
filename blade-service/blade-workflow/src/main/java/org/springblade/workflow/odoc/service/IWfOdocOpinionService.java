package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.entity.WfOdocOpinion;

import java.util.List;

/**
 * 公文意见模板服务（常用批示语 / 签字意见模板）。
 */
public interface IWfOdocOpinionService {

	/** 可用模板：全局(defId 空) + 指定流程；按 showOrder, id 升序 */
	List<WfOdocOpinion> list(Long defId);

	/** 保存（有 id 更新，无 id 新增） */
	WfOdocOpinion save(WfOdocOpinion opinion);

	/** 删除 */
	boolean remove(Long id);

	/** 灌入全局常用批示语（已存在则跳过），返回新增条数 */
	int seedIfAbsent();
}
