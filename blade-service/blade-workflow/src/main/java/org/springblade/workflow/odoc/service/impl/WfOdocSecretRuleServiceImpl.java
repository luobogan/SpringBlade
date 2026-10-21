package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.odoc.entity.WfOdocReceiveGroup;
import org.springblade.workflow.odoc.entity.WfOdocReceiveUnit;
import org.springframework.util.StringUtils;
import org.springblade.workflow.odoc.entity.WfOdocSecretRule;
import org.springblade.workflow.odoc.mapper.WfOdocReceiveUnitMapper;
import org.springblade.workflow.odoc.mapper.WfOdocSecretRuleMapper;
import org.springblade.workflow.odoc.secret.OdocSecretLevel;
import org.springframework.stereotype.Service;
import org.springblade.workflow.odoc.service.IWfOdocReceiveGroupService;
import org.springblade.workflow.odoc.service.IWfOdocSecretRuleService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 密级收发限制规则服务实现。
 *
 * <p>规则数据量小，{@link #violationMessage} 全量加载内存匹配（避免 MP lambda {@code .in()}
 * 在未 bootstrap 环境抛 lambda cache）；单位/组名解析走 {@code selectBatchIds}/{@code list()}（非 lambda，可测）。</p>
 *
 * <p>公共组级规则语义：收文单位命中某公共组成员且公文密级超出该组允许范围即视为越限
 * （合规上从严——无法从「已展开的单位 id」区分「整组选择」与「逐个单位选择」）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfOdocSecretRuleServiceImpl implements IWfOdocSecretRuleService {

	private final WfOdocSecretRuleMapper ruleMapper;
	private final WfOdocReceiveUnitMapper unitMapper;
	private final IWfOdocReceiveGroupService receiveGroupService;

	@Override
	public List<WfOdocSecretRule> list() {
		return ruleMapper.selectList(Wrappers.<WfOdocSecretRule>lambdaQuery()
			.orderByAsc(WfOdocSecretRule::getId));
	}

	@Override
	public WfOdocSecretRule save(WfOdocSecretRule rule) {
		if (rule == null || rule.getTargetType() == null
			|| (rule.getTargetType() != WfOdocSecretRule.TARGET_UNIT && rule.getTargetType() != WfOdocSecretRule.TARGET_GROUP)
			|| rule.getTargetId() == null) {
			throw new IllegalArgumentException("目标类型(1单位/2公共组)与目标ID不能为空");
		}
		int from = rule.getSecLevelFrom() != null ? rule.getSecLevelFrom() : 0;
		int to = rule.getSecLevelTo() != null ? rule.getSecLevelTo() : 3;
		if (from > to) {
			throw new IllegalArgumentException("允许密级起不能大于至");
		}
		rule.setSecLevelFrom(from);
		rule.setSecLevelTo(to);
		if (rule.getEnabled() == null) {
			rule.setEnabled(1);
		}
		if (rule.getId() != null) {
			ruleMapper.updateById(rule);
			return rule;
		}
		ruleMapper.insert(rule);
		return rule;
	}

	@Override
	public boolean remove(Long id) {
		if (id == null) {
			return false;
		}
		return ruleMapper.deleteById(id) > 0;
	}

	@Override
	public String violationMessage(String docSecretLevel, String receiveUnitIdsCsv) {
		int docLevel = OdocSecretLevel.levelOf(docSecretLevel);
		if (docLevel < 0) {
			// 密级无法判定 → 不校验（避免误伤）
			return null;
		}
		List<Long> ids = numericIds(receiveUnitIdsCsv);
		if (ids.isEmpty()) {
			return null;
		}
		// 全量加载启用规则，分「单位 / 公共组」
		Map<Long, WfOdocSecretRule> unitRules = new HashMap<>();
		List<WfOdocSecretRule> groupRules = new ArrayList<>();
		list().stream()
			.filter(r -> r.getEnabled() != null && r.getEnabled() == 1 && r.getTargetId() != null)
			.forEach(r -> {
				if (r.getTargetType() != null && r.getTargetType() == WfOdocSecretRule.TARGET_GROUP) {
					groupRules.add(r);
				} else if (r.getTargetType() != null && r.getTargetType() == WfOdocSecretRule.TARGET_UNIT) {
					unitRules.put(r.getTargetId(), r);
				}
			});

		// 单位越限
		List<Long> badUnits = new ArrayList<>();
		for (Long id : ids) {
			WfOdocSecretRule r = unitRules.get(id);
			if (r != null && outOfRange(docLevel, r)) {
				badUnits.add(id);
			}
		}
		// 公共组越限：收文单位命中组成员且越限
		Map<Long, WfOdocReceiveGroup> groupById = new HashMap<>();
		List<Long> badGroups = new ArrayList<>();
		if (!groupRules.isEmpty()) {
			receiveGroupService.list().forEach(g -> {
				if (g.getId() != null) {
					groupById.put(g.getId(), g);
				}
			});
			for (WfOdocSecretRule r : groupRules) {
				WfOdocReceiveGroup g = groupById.get(r.getTargetId());
				if (g == null) {
					continue;
				}
				List<Long> members = numericIds(g.getUnitIds());
				boolean hit = members.stream().anyMatch(ids::contains);
				if (hit && outOfRange(docLevel, r)) {
					badGroups.add(r.getTargetId());
				}
			}
		}
		if (badUnits.isEmpty() && badGroups.isEmpty()) {
			return null;
		}

		StringBuilder sb = new StringBuilder("收文密级不符：");
		if (!badUnits.isEmpty()) {
			Map<Long, String> un = new HashMap<>();
			unitMapper.selectBatchIds(badUnits).forEach(u -> un.put(u.getId(), u.getUnitName()));
			sb.append("单位[")
				.append(badUnits.stream().map(id -> un.getOrDefault(id, String.valueOf(id))).collect(Collectors.joining("、")))
				.append("]");
		}
		if (!badGroups.isEmpty()) {
			if (!badUnits.isEmpty()) {
				sb.append("；");
			}
			sb.append("公共组[")
				.append(badGroups.stream()
					.map(id -> {
						WfOdocReceiveGroup g = groupById.get(id);
						return g != null && StringUtils.hasText(g.getGroupName()) ? g.getGroupName() : String.valueOf(id);
					})
					.collect(Collectors.joining("、")))
				.append("]");
		}
		sb.append("（当前公文密级「").append(docSecretLevel).append("」，超出其允许收发范围）");
		return sb.toString();
	}

	private static boolean outOfRange(int docLevel, WfOdocSecretRule r) {
		int from = r.getSecLevelFrom() != null ? r.getSecLevelFrom() : 0;
		int to = r.getSecLevelTo() != null ? r.getSecLevelTo() : 3;
		return docLevel < from || docLevel > to;
	}

	@Override
	public List<Long> allowedUnitIds(String docSecretLevel) {
		// 密级名无法判定 → 不限制，返回全部单位 id
		int docLevel = OdocSecretLevel.levelOf(docSecretLevel);
		if (docLevel < 0) {
			return allUnitIds();
		}
		Map<Long, WfOdocSecretRule> unitRules = new HashMap<>();
		List<WfOdocSecretRule> groupRules = new ArrayList<>();
		list().stream()
			.filter(r -> r.getEnabled() != null && r.getEnabled() == 1 && r.getTargetId() != null)
			.forEach(r -> {
				if (r.getTargetType() != null && r.getTargetType() == WfOdocSecretRule.TARGET_GROUP) {
					groupRules.add(r);
				} else if (r.getTargetType() != null && r.getTargetType() == WfOdocSecretRule.TARGET_UNIT) {
					unitRules.put(r.getTargetId(), r);
				}
			});

		Set<Long> disallowed = new java.util.HashSet<>();
		unitRules.forEach((uid, r) -> {
			if (outOfRange(docLevel, r)) {
				disallowed.add(uid);
			}
		});
		if (!groupRules.isEmpty()) {
			Map<Long, WfOdocReceiveGroup> groupById = new HashMap<>();
			receiveGroupService.list().forEach(g -> {
				if (g.getId() != null) {
					groupById.put(g.getId(), g);
				}
			});
			for (WfOdocSecretRule r : groupRules) {
				WfOdocReceiveGroup g = groupById.get(r.getTargetId());
				if (g == null) {
					continue;
				}
				if (outOfRange(docLevel, r)) {
					numericIds(g.getUnitIds()).forEach(disallowed::add);
				}
			}
		}

		List<Long> all = allUnitIds();
		if (disallowed.isEmpty()) {
			return all;
		}
		return all.stream().filter(id -> !disallowed.contains(id)).collect(Collectors.toList());
	}

	/** 全部收文单位 id（用于「无限制」场景与差集计算） */
	private List<Long> allUnitIds() {
		return unitMapper.selectList(Wrappers.<WfOdocReceiveUnit>lambdaQuery().select(WfOdocReceiveUnit::getId))
			.stream()
			.map(WfOdocReceiveUnit::getId)
			.collect(Collectors.toList());
	}

	private static List<Long> numericIds(String csv) {
		List<Long> ids = new ArrayList<>();
		if (!StringUtils.hasText(csv)) {
			return ids;
		}
		for (String p : csv.split(",")) {
			String t = p.trim();
			if (!t.isEmpty() && t.chars().allMatch(Character::isDigit)) {
				try {
					ids.add(Long.valueOf(t));
				} catch (NumberFormatException ignore) {
					// 超出 long 范围，忽略
				}
			}
		}
		return ids;
	}
}
