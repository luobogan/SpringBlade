package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springblade.system.user.feign.IUserClient;
import org.springblade.workflow.entity.WfMonitorScope;
import org.springblade.workflow.mapper.WfMonitorScopeMapper;
import org.springblade.workflow.service.IWfMonitorScopeService;
import org.springblade.workflow.vo.MonitorScopeView;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 流程监控委托范围服务实现（对应设计文档 §1.3）
 *
 * <p>委托范围解析：把当前用户被授权的「部门/分部/人员」范围，统一解析为可查看的
 * 发起人ID集合（部门/分部经 {@code IUserClient#userIdsByDept} 展开为具体人员），
 * 以及允许查看的流程定义集合（defId 为 null 表示全部）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfMonitorScopeServiceImpl implements IWfMonitorScopeService {

	private final WfMonitorScopeMapper scopeMapper;
	/** 用户中心：部门/分部范围 → 具体人员ID集合 */
	private final IUserClient userClient;

	@Override
	public MonitorScopeView resolve(Long userId) {
		MonitorScopeView view = new MonitorScopeView();
		Set<Long> defIds = new LinkedHashSet<>();
		Set<Long> starterIds = new LinkedHashSet<>();
		view.setDefIds(defIds);
		view.setStarterIds(starterIds);
		if (userId == null) {
			return view;
		}
		// 当前租户全部委托范围（TenantEntity 自动带 tenant_id 过滤）
		List<WfMonitorScope> all = scopeMapper.selectList(Wrappers.<WfMonitorScope>lambdaQuery());
		boolean hasAny = false;
		for (WfMonitorScope s : all) {
			if (!s.containsMonitorUser(userId)) {
				continue;
			}
			hasAny = true;
			if (s.getDefId() == null) {
				view.setAllDefs(true);
			} else {
				defIds.add(s.getDefId());
			}
			Integer type = s.getScopeType();
			if (type != null && type == 3) {
				// 人员范围：直接加入发起人集合
				if (s.getScopeValue() != null) {
					starterIds.add(s.getScopeValue());
				}
			} else {
				// 分部/部门范围：经用户中心展开为具体人员（containChild=true 含下级）
				if (s.getScopeValue() != null) {
					try {
						R<List<Long>> r = userClient.userIdsByDept(s.getScopeValue(), true);
						if (r != null && r.isSuccess() && r.getData() != null) {
							starterIds.addAll(r.getData());
						}
					} catch (Exception e) {
						log.warn("[blade-workflow] 监控委托范围解析部门人员失败. scopeId={}, err={}",
							s.getId(), e.getMessage());
					}
				}
			}
		}
		view.setHasAny(hasAny);
		return view;
	}

	@Override
	public IPage<WfMonitorScope> pageScope(Integer current, Integer pageSize, WfMonitorScope condition) {
		long cur = (current == null || current < 1) ? 1 : current;
		long size = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 200);
		Page<WfMonitorScope> page = new Page<>(cur, size);
		var q = Wrappers.<WfMonitorScope>lambdaQuery();
		if (condition != null) {
			q.eq(condition.getScopeType() != null, WfMonitorScope::getScopeType, condition.getScopeType())
				.like(condition.getMonitorUserIds() != null && !condition.getMonitorUserIds().isBlank(),
					WfMonitorScope::getMonitorUserIds, condition.getMonitorUserIds())
				.eq(condition.getDefId() != null, WfMonitorScope::getDefId, condition.getDefId());
		}
		q.orderByDesc(WfMonitorScope::getId);
		return scopeMapper.selectPage(page, q);
	}

	@Override
	public boolean saveScope(WfMonitorScope scope) {
		normalize(scope);
		return scopeMapper.insert(scope) > 0;
	}

	@Override
	public boolean updateScope(WfMonitorScope scope) {
		normalize(scope);
		return scopeMapper.updateById(scope) > 0;
	}

	/**
	 * 归一化：defId / scopeValue ≤ 0（含 BladeX nullToEmpty 的 -1 哨兵）视为未设置 → null。
	 * 否则会被当成"具体的定义/人员 ID"导致范围匹配落空。
	 */
	private void normalize(WfMonitorScope scope) {
		if (scope == null) {
			return;
		}
		if (scope.getDefId() != null && scope.getDefId() <= 0) {
			scope.setDefId(null);
		}
		if (scope.getScopeValue() != null && scope.getScopeValue() <= 0) {
			scope.setScopeValue(null);
		}
	}

	@Override
	public boolean removeScope(Long id) {
		return scopeMapper.deleteById(id) > 0;
	}

	@Override
	public WfMonitorScope getScope(Long id) {
		return scopeMapper.selectById(id);
	}
}
