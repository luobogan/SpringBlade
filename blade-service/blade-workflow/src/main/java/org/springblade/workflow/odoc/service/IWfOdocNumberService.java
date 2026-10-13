package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.entity.WfOdocNumber;

import java.util.List;

/**
 * 公文文号服务。
 *
 * <p>文号规则（{@link WfOdocNumber}）持有「当前流水号 serial」。取号用<b>行锁原子自增</b>
 * （UPDATE serial=serial+1 后同事务内读回），保证并发下不重号；不采用内存自增。</p>
 */
public interface IWfOdocNumberService {

	/**
	 * 取号（占用）：按流程定义解析文号规则，行锁自增 serial，返回格式化文号。
	 *
	 * @param defId 流程定义ID
	 */
	String assign(Long defId);

	/** 预览下一号（不自增，用于展示「下一文号」） */
	String preview(Long defId);

	/** 规则列表（defId 命中或全局） */
	List<WfOdocNumber> listRules(Long defId);

	WfOdocNumber createRule(WfOdocNumber rule);

	WfOdocNumber updateRule(WfOdocNumber rule);

	void removeRule(Long id);
}
