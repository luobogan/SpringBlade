package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.odoc.entity.WfOdocReceiveUnit;
import org.springblade.workflow.odoc.mapper.WfOdocReceiveUnitMapper;
import org.springblade.workflow.odoc.service.IWfOdocReceiveUnitService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收发文单位服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfOdocReceiveUnitServiceImpl implements IWfOdocReceiveUnitService {

	private final WfOdocReceiveUnitMapper unitMapper;

	@Override
	public List<WfOdocReceiveUnit> list() {
		return unitMapper.selectList(Wrappers.<WfOdocReceiveUnit>lambdaQuery()
			.orderByAsc(WfOdocReceiveUnit::getShowOrder)
			.orderByAsc(WfOdocReceiveUnit::getId));
	}

	@Override
	public List<WfOdocReceiveUnit> listEnabled() {
		return unitMapper.selectList(Wrappers.<WfOdocReceiveUnit>lambdaQuery()
			.eq(WfOdocReceiveUnit::getEnabled, 1)
			.orderByAsc(WfOdocReceiveUnit::getShowOrder)
			.orderByAsc(WfOdocReceiveUnit::getId));
	}

	@Override
	public WfOdocReceiveUnit get(Long id) {
		return id == null ? null : unitMapper.selectById(id);
	}

	@Override
	public List<WfOdocReceiveUnit> listChildren(Long superiorId) {
		return unitMapper.selectList(Wrappers.<WfOdocReceiveUnit>lambdaQuery()
			.eq(WfOdocReceiveUnit::getSuperiorId, superiorId)
			.orderByAsc(WfOdocReceiveUnit::getShowOrder)
			.orderByAsc(WfOdocReceiveUnit::getId));
	}

	@Override
	public WfOdocReceiveUnit save(WfOdocReceiveUnit unit) {
		if (unit == null || !StringUtils.hasText(unit.getUnitName())) {
			throw new IllegalArgumentException("单位名称不能为空");
		}
		if (unit.getSuperiorId() != null && unit.getSuperiorId().equals(unit.getId())) {
			throw new IllegalArgumentException("上级单位不能是自身");
		}
		if (unit.getCompanyType() == null) {
			unit.setCompanyType(WfOdocReceiveUnit.COMPANY_INTERNAL);
		}
		if (unit.getEnabled() == null) {
			unit.setEnabled(1);
		}
		if (unit.getId() != null) {
			unitMapper.updateById(unit);
			return unit;
		}
		unitMapper.insert(unit);
		return unit;
	}

	@Override
	public boolean remove(Long id) {
		if (id == null) {
			return false;
		}
		return unitMapper.deleteById(id) > 0;
	}

	@Override
	public int seedIfAbsent() {
		int inserted = 0;
		if (!existsByName("本单位")) {
			WfOdocReceiveUnit root = new WfOdocReceiveUnit();
			root.setUnitName("本单位");
			root.setCompanyType(WfOdocReceiveUnit.COMPANY_INTERNAL);
			root.setIsMain(1);
			root.setUnitLevel(1);
			root.setShowOrder(1);
			root.setEnabled(1);
			unitMapper.insert(root);
			inserted++;
		}
		if (!existsByName("示例外部单位")) {
			WfOdocReceiveUnit ext = new WfOdocReceiveUnit();
			ext.setUnitName("示例外部单位");
			ext.setCompanyType(WfOdocReceiveUnit.COMPANY_EXTERNAL);
			ext.setUnitLevel(1);
			ext.setShowOrder(2);
			ext.setEnabled(1);
			unitMapper.insert(ext);
			inserted++;
		}
		if (inserted > 0) {
			log.info("收发文单位种子灌入完成, 新增 {} 条", inserted);
		}
		return inserted;
	}

	@Override
	public String namesOfCsv(String csv) {
		if (csv == null || csv.isBlank()) {
			return csv;
		}
		String[] parts = csv.split(",");
		boolean anyNumeric = false;
		for (String p : parts) {
			if (isNumeric(p.trim())) {
				anyNumeric = true;
				break;
			}
		}
		if (!anyNumeric) {
			// 全是名称（存量数据），无需查库，原样返回
			return csv;
		}
		// 含单位 id：加载单位字典（数据量小）建 id→名称映射，逐 token 解析；非数字 token 原样保留。
		// 注：走 list() 而非 lambda .in()，避免 MP wrapper 建时即触发 lambda cache（单测未 bootstrap 会抛）。
		Map<Long, String> nameMap = new HashMap<>();
		list().forEach(u -> {
			if (u.getId() != null) {
				nameMap.put(u.getId(), u.getUnitName());
			}
		});
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			String t = parts[i].trim();
			String resolved = t;
			if (isNumeric(t)) {
				try {
					resolved = nameMap.getOrDefault(Long.valueOf(t), t);
				} catch (NumberFormatException ignore) {
					// 保留原名
				}
			}
			if (i > 0) {
				sb.append(',');
			}
			sb.append(resolved);
		}
		return sb.toString();
	}

	private static boolean isNumeric(String s) {
		return s != null && !s.isEmpty() && s.chars().allMatch(Character::isDigit);
	}

	private boolean existsByName(String name) {
		Long c = unitMapper.selectCount(Wrappers.<WfOdocReceiveUnit>lambdaQuery()
			.eq(WfOdocReceiveUnit::getUnitName, name));
		return c != null && c > 0;
	}
}
