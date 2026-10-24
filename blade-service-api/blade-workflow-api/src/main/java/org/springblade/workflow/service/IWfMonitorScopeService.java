package org.springblade.workflow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.workflow.entity.WfMonitorScope;
import org.springblade.workflow.vo.MonitorScopeView;

/**
 * 流程监控委托范围服务
 *
 * <p>管理「谁可以监控哪些流程」的配置（对应设计文档 §1.3），并提供
 * {@link #resolve(Long)} 把当前用户的委托范围解析为可查看的「发起人/流程定义」集合，
 * 供监控列表与异常聚合做数据范围裁剪。</p>
 */
public interface IWfMonitorScopeService {

	/**
	 * 解析指定用户的监控委托范围。
	 *
	 * @param userId 当前登录用户ID
	 * @return 汇总后的可见范围；{@link MonitorScopeView#isHasAny()} 为 false 表示无任何委托
	 */
	MonitorScopeView resolve(Long userId);

	/**
	 * 分页查询委托范围配置（管理面）。
	 */
	IPage<WfMonitorScope> pageScope(Integer current, Integer pageSize, WfMonitorScope condition);

	/**
	 * 新增委托范围配置。
	 */
	boolean saveScope(WfMonitorScope scope);

	/**
	 * 更新委托范围配置。
	 */
	boolean updateScope(WfMonitorScope scope);

	/**
	 * 删除委托范围配置。
	 */
	boolean removeScope(Long id);

	/**
	 * 查询单条委托范围配置。
	 */
	WfMonitorScope getScope(Long id);
}
