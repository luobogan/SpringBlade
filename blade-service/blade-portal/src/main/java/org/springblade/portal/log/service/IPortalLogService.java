package org.springblade.portal.log.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.portal.entity.PortalLog;

/**
 * 门户业务审计服务
 * <p>写操作埋点记录（操作人/前后值/IP/时间），供管理端查询；record 内部异常隔离，绝不影响业务返回。</p>
 */
public interface IPortalLogService {

	/**
	 * 记录一次成功的写操作（IP/操作人/时间由实现内部取当前请求与登录用户）
	 */
	void record(String logType, String bizType, String bizKey, String title,
				String summary, String beforeJson, String afterJson);

	/**
	 * 记录一次成功的写操作（带请求参数快照）
	 */
	void record(String logType, String bizType, String bizKey, String title,
				String summary, String beforeJson, String afterJson, String paramsJson);

	/**
	 * 记录一次失败的写操作（status=0）
	 */
	void recordFail(String logType, String bizType, String bizKey, String title, String errorMsg);

	/**
	 * 管理端分页查询（按操作时间倒序）
	 *
	 * @param current 页码
	 * @param size    每页条数
	 * @param logType 操作类型过滤（可空）
	 * @param bizKey  业务标识过滤（可空）
	 */
	IPage<PortalLog> page(long current, long size, String logType, String bizKey);
}
