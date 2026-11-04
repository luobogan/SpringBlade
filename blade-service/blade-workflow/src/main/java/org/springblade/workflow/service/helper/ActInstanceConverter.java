package org.springblade.workflow.service.helper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.ActHiProcinst;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.utils.WfAuthUtil;

/**
 * ACT_HI_PROCINST → WfInstance 只读投影转换（去 wf_ 表读源切换复用）。
 *
 * <p>与 {@code WfInstanceServiceImpl#loadInstance} 的 stub 映射保持一致：仅填充运行期读 / 表单渲染 /
 * 自定义操作实际消费的字段（id / starter / status / currentNodeKey / isTest / defId / formId /
 * dataId / engineInstId），不写回 wf_instance。</p>
 *
 * <p>另提供 {@link #queryByBusinessId(Long)}：集中构造「按业务实例ID 反查 ACT_HI_PROCINST」的条件，
 * 统一带上租户过滤（T-13）。</p>
 */
@Slf4j
public final class ActInstanceConverter {

	private ActInstanceConverter() {
	}

	public static WfInstance fromAct(ActHiProcinst t) {
		if (t == null) {
			return null;
		}
		WfInstance w = new WfInstance();
		w.setId(t.getBusinessId());
		w.setStarter(t.getStarter());
		w.setStatus(ActHiProcinst.statusCode(t.getBusinessStatus()));
		w.setCurrentNodeKey(t.getCurrentNodeKey());
		w.setIsTest(t.getIsTest());
		w.setDefId(t.getDefId());
		w.setFormId(t.getFormId());
		w.setDataId(t.getDataId());
		w.setEngineInstId(t.getId());
		return w;
	}

	/**
	 * 构造「按业务实例ID（{@code BUSINESS_ID_}）反查 ACT_HI_PROCINST」的查询条件（含 T-13 租户过滤）。
	 *
	 * <p>ACT_HI_PROCINST 的租户列是 {@code TENANT_ID_}（非 {@code tenant_id}），
	 * <b>不会被 MyBatis-Plus 租户插件自动追加条件</b>，必须显式带上，否则多租户下会串数据。</p>
	 *
	 * <p>采用<b>条件式过滤</b>：有登录上下文时按租户过滤；无登录上下文（定时任务、系统自动流转等
	 * 后台调用 {@code WfAuthUtil.tenantId()} 返回 null）时<b>不追加</b>租户条件，
	 * 以免这些调用读空而误伤业务。</p>
	 *
	 * @param businessId 业务实例ID（等于 wf_instance.id）
	 */
	public static LambdaQueryWrapper<ActHiProcinst> queryByBusinessId(Long businessId) {
		LambdaQueryWrapper<ActHiProcinst> q = Wrappers.<ActHiProcinst>lambdaQuery()
			.eq(ActHiProcinst::getBusinessId, businessId);
		String tenant = WfAuthUtil.tenantId();
		if (tenant != null && !tenant.isBlank()) {
			q.eq(ActHiProcinst::getTenantId, tenant);
		} else {
			log.warn("[blade-workflow] ACT 实例读源缺少租户上下文，本次未按租户过滤（后台调用属预期）businessId={}",
				businessId);
		}
		// ⚠️ 归档实例被「强制收回 / 监控回退」复活后，同一 BUSINESS_ID_ 在 ACT_HI_PROCINST 会有
		// **多条行**（每次 revive 新建引擎运行态，旧行保留作审计）；LIMIT 1 无排序会随机命中
		// 旧的已归档行（BUSINESS_STATUS_=终态、旧节点）→ fresh / detail / 表单渲染 / 自定义操作
		// 全部拿到过期状态，复活后的实例被误判「已归档」而无法继续办理。
		// 口径：活跃行（END_TIME_ IS NULL，运行中的新运行态）优先；全部已结束时按 START_TIME_
		// 取最新（复活运行态的发起时间必然晚于旧行，最后一次归档的行即权威行）。
		q.last("ORDER BY END_TIME_ IS NULL DESC, START_TIME_ DESC LIMIT 1");
		return q;
	}
}
