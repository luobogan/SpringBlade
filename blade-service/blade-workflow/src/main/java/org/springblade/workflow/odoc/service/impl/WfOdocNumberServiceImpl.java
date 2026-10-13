package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.odoc.entity.WfOdocNumber;
import org.springblade.workflow.odoc.mapper.WfOdocNumberMapper;
import org.springblade.workflow.odoc.service.IWfOdocNumberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 文号服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfOdocNumberServiceImpl implements IWfOdocNumberService {

	private final WfOdocNumberMapper numberMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public String assign(Long defId) {
		WfOdocNumber rule = resolveRule(defId);
		if (rule == null) {
			throw new ServiceException("未找到可用的文号规则(defId=" + defId + ")");
		}
		// 行锁原子自增：UPDATE 持行锁至事务提交，串行化并发取号
		numberMapper.update(null, Wrappers.<WfOdocNumber>lambdaUpdate()
			.eq(WfOdocNumber::getId, rule.getId())
			.setSql("serial = serial + 1"));
		WfOdocNumber fresh = numberMapper.selectById(rule.getId());
		String number = format(fresh, fresh.getSerial());
		log.info("文号取号完成 defId={} ruleId={} serial={} number={}", defId, fresh.getId(), fresh.getSerial(), number);
		return number;
	}

	@Override
	public String preview(Long defId) {
		WfOdocNumber rule = resolveRule(defId);
		if (rule == null) {
			throw new ServiceException("未找到可用的文号规则(defId=" + defId + ")");
		}
		int cur = rule.getSerial() == null ? 0 : rule.getSerial();
		return format(rule, cur + 1);
	}

	@Override
	public List<WfOdocNumber> listRules(Long defId) {
		List<WfOdocNumber> all = numberMapper.selectList(Wrappers.<WfOdocNumber>lambdaQuery()
			.eq(WfOdocNumber::getIsOpen, 1).orderByAsc(WfOdocNumber::getId));
		if (defId == null) {
			return all;
		}
		return all.stream()
			.filter(r -> matchesDef(r, defId))
			.collect(Collectors.toList());
	}

	@Override
	public WfOdocNumber createRule(WfOdocNumber rule) {
		if (rule.getSerial() == null) {
			rule.setSerial(0);
		}
		if (rule.getIsOpen() == null) {
			rule.setIsOpen(1);
		}
		numberMapper.insert(rule);
		return rule;
	}

	@Override
	public WfOdocNumber updateRule(WfOdocNumber rule) {
		numberMapper.updateById(rule);
		return rule;
	}

	@Override
	public void removeRule(Long id) {
		numberMapper.deleteById(id);
	}

	/** 解析文号规则：优先「绑定该 defId」的，其次「全局（defIds 为空）」 */
	private WfOdocNumber resolveRule(Long defId) {
		List<WfOdocNumber> all = numberMapper.selectList(Wrappers.<WfOdocNumber>lambdaQuery()
			.eq(WfOdocNumber::getIsOpen, 1).orderByAsc(WfOdocNumber::getId));
		if (all.isEmpty()) {
			return null;
		}
		if (defId != null) {
			WfOdocNumber bound = all.stream().filter(r -> matchesDef(r, defId)).findFirst().orElse(null);
			if (bound != null) {
				return bound;
			}
		}
		return all.stream().filter(r -> r.getDefIds() == null || r.getDefIds().isBlank()).findFirst().orElse(null);
	}

	private boolean matchesDef(WfOdocNumber rule, Long defId) {
		String defIds = rule.getDefIds();
		if (defIds == null || defIds.isBlank()) {
			return false;
		}
		return Arrays.stream(defIds.split(","))
			.map(String::trim)
			.anyMatch(s -> s.equals(String.valueOf(defId)));
	}

	/** 格式化：prefix + [年份] + serialPre + 补零流水 */
	private String format(WfOdocNumber rule, int serial) {
		StringBuilder sb = new StringBuilder();
		if (notBlank(rule.getPrefix())) {
			sb.append(rule.getPrefix());
		}
		if (Objects.equals(rule.getShowYear(), 1)) {
			sb.append(rule.getYearPre() == null ? "" : rule.getYearPre());
			sb.append(Year.now().getValue());
			sb.append(rule.getYearEnd() == null ? "" : rule.getYearEnd());
		}
		if (notBlank(rule.getSerialPre())) {
			sb.append(rule.getSerialPre());
		}
		sb.append(pad(serial, rule.getDigit(), rule.getFillZero()));
		return sb.toString();
	}

	private String pad(int serial, Integer digit, Integer fillZero) {
		if (Objects.equals(fillZero, 1) && digit != null && digit > 0) {
			return String.format("%0" + digit + "d", serial);
		}
		return String.valueOf(serial);
	}

	private boolean notBlank(String s) {
		return s != null && !s.isBlank();
	}
}
