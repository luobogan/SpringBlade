package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.odoc.entity.WfOdocDict;
import org.springblade.workflow.odoc.entity.WfOdocNumber;
import org.springblade.workflow.odoc.mapper.WfOdocDictMapper;
import org.springblade.workflow.odoc.mapper.WfOdocNumberMapper;
import org.springblade.workflow.odoc.service.IWfOdocNumberReservedService;
import org.springblade.workflow.odoc.service.IWfOdocNumberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文号服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfOdocNumberServiceImpl implements IWfOdocNumberService {

	private final WfOdocNumberMapper numberMapper;
	private final IWfOdocNumberReservedService reservedService;
	private final WfOdocDictMapper dictMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public String assign(Long defId) {
		WfOdocNumber rule = resolveRule(defId);
		if (rule == null) {
			throw new ServiceException("未找到可用的文号规则(defId=" + defId + ")");
		}
		int curYear = Year.now().getValue();
		// 行锁原子更新：跨年归零（year_reset=1 且年份不符则归 1）或自增，并记录当前年份；
		// UPDATE 持行锁至事务提交，串行化并发取号。year_reset=0 时跨年累计（不重置）。
		numberMapper.update(null, Wrappers.<WfOdocNumber>lambdaUpdate()
			.eq(WfOdocNumber::getId, rule.getId())
			.setSql("serial = CASE WHEN serial_year IS NULL OR (serial_year <> " + curYear + " AND year_reset = 1)"
				+ " THEN 1 ELSE serial + 1 END, serial_year = " + curYear));
		WfOdocNumber fresh = numberMapper.selectById(rule.getId());
		// 跳过「当前有效（未过期）」的保留号，避免发出被保留的流水
		Set<Integer> reserved = reservedService.activeSerialSet(rule.getId());
		int guard = 0;
		while (reserved.contains(fresh.getSerial()) && guard < 100000) {
			guard += 1;
			numberMapper.update(null, Wrappers.<WfOdocNumber>lambdaUpdate()
				.eq(WfOdocNumber::getId, rule.getId())
				.setSql("serial = serial + 1"));
			fresh = numberMapper.selectById(rule.getId());
		}
		String number = format(fresh, fresh.getSerial());
		log.info("文号取号完成 defId={} ruleId={} serial={} year={} number={}", defId, fresh.getId(), fresh.getSerial(), curYear, number);
		return number;
	}

	@Override
	public String preview(Long defId) {
		WfOdocNumber rule = resolveRule(defId);
		if (rule == null) {
			throw new ServiceException("未找到可用的文号规则(defId=" + defId + ")");
		}
		int curYear = Year.now().getValue();
		// 预览下一号：跨年归零(yr_reset=1 且年份不符则归 1)或从当前+1，并跳过保留号（与 assign 对齐）
		boolean resetOnYearChange = rule.getYearReset() == null || rule.getYearReset() == 1;
		int next = (rule.getSerialYear() == null
				|| (rule.getSerialYear() != curYear && resetOnYearChange))
			? 1
			: (rule.getSerial() == null ? 1 : rule.getSerial() + 1);
		Set<Integer> reserved = reservedService.activeSerialSet(rule.getId());
		int guard = 0;
		while (reserved.contains(next) && guard < 100000) {
			next += 1;
			guard += 1;
		}
		return format(rule, next);
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
		String prefix = resolvePrefix(rule);
		if (notBlank(prefix)) {
			sb.append(prefix);
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

	/**
	 * 解析文号代字：优先用 {@code orgAbbrId} 关联的机构代字字典（`wf_odoc_dict.org_abbr` 的 name），
	 * 实现「机构代字独立建模」——代字由字典维护而非散落于规则自由文本；未关联或字典缺失时
	 * 回退到规则自身的 {@code prefix} 自由文本，保证历史规则向后兼容。
	 */
	private String resolvePrefix(WfOdocNumber rule) {
		Long orgAbbrId = rule.getOrgAbbrId();
		if (orgAbbrId != null) {
			WfOdocDict abbr = dictMapper.selectById(orgAbbrId);
			if (abbr != null && notBlank(abbr.getName())) {
				return abbr.getName();
			}
		}
		return rule.getPrefix();
	}

	private boolean notBlank(String s) {
		return s != null && !s.isBlank();
	}
}
