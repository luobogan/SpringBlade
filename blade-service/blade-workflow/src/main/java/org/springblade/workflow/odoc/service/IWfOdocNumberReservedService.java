package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.entity.WfOdocNumberReserved;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 文号保留号服务。
 *
 * <p>保留号用于人工跳过特定流水（如吉祥号/已占用号）。取号 {@code WfOdocNumberServiceImpl}
 * 会在自增后跳过「当前有效（未过期）」的保留号，避免发出被保留的文号。</p>
 */
public interface IWfOdocNumberReservedService {

	/** 某文号规则当前有效（未过期）的保留流水集合 */
	Set<Integer> activeSerialSet(Long numberId);

	/** 按规则列出保留号（升序） */
	List<WfOdocNumberReserved> listByNumberId(Long numberId);

	/** 按 [startSerial,endSerial] 批量保留流水，自动跳过已存在的；返回实际新增条数 */
	int reserveRange(Long numberId, int startSerial, int endSerial, LocalDateTime expireTime);

	/** 释放（删除）一条保留号 */
	boolean removeReserved(Long id);
}
