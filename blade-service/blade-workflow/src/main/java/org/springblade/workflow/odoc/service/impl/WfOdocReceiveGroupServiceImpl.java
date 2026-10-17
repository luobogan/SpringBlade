package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.odoc.entity.WfOdocReceiveGroup;
import org.springblade.workflow.odoc.mapper.WfOdocReceiveGroupMapper;
import org.springblade.workflow.odoc.service.IWfOdocReceiveGroupService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收发文公共组服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfOdocReceiveGroupServiceImpl implements IWfOdocReceiveGroupService {

	private final WfOdocReceiveGroupMapper groupMapper;

	@Override
	public List<WfOdocReceiveGroup> list() {
		return groupMapper.selectList(Wrappers.<WfOdocReceiveGroup>lambdaQuery()
			.orderByAsc(WfOdocReceiveGroup::getShowOrder)
			.orderByAsc(WfOdocReceiveGroup::getId));
	}

	@Override
	public List<WfOdocReceiveGroup> listEnabled() {
		return groupMapper.selectList(Wrappers.<WfOdocReceiveGroup>lambdaQuery()
			.eq(WfOdocReceiveGroup::getEnabled, 1)
			.orderByAsc(WfOdocReceiveGroup::getShowOrder)
			.orderByAsc(WfOdocReceiveGroup::getId));
	}

	@Override
	public WfOdocReceiveGroup get(Long id) {
		return id == null ? null : groupMapper.selectById(id);
	}

	@Override
	public WfOdocReceiveGroup save(WfOdocReceiveGroup group) {
		if (group == null || !StringUtils.hasText(group.getGroupName())) {
			throw new IllegalArgumentException("公共组名称不能为空");
		}
		if (group.getEnabled() == null) {
			group.setEnabled(1);
		}
		if (group.getId() != null) {
			groupMapper.updateById(group);
			return group;
		}
		groupMapper.insert(group);
		return group;
	}

	@Override
	public boolean remove(Long id) {
		if (id == null) {
			return false;
		}
		return groupMapper.deleteById(id) > 0;
	}

	@Override
	public String expandToUnitIds(String csv) {
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
		// 加载公共组（数据量小），建 groupId → 成员单位 id 列表
		Map<Long, List<Long>> groupMembers = new HashMap<>();
		list().forEach(g -> {
			if (g.getId() == null) {
				return;
			}
			List<Long> ids = new ArrayList<>();
			if (StringUtils.hasText(g.getUnitIds())) {
				for (String u : g.getUnitIds().split(",")) {
					String t = u.trim();
					if (isNumeric(t)) {
						try {
							ids.add(Long.valueOf(t));
						} catch (NumberFormatException ignore) {
							// 超范围按忽略
						}
					}
				}
			}
			groupMembers.put(g.getId(), ids);
		});
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String p : parts) {
			String t = p.trim();
			List<Long> members = null;
			if (isNumeric(t)) {
				try {
					members = groupMembers.get(Long.valueOf(t));
				} catch (NumberFormatException ignore) {
					// 非组 id（单位 id 或超范围），按原 token
				}
			}
			List<String> out = members != null ? new ArrayList<>(members.size()) : null;
			if (members != null) {
				members.forEach((m) -> out.add(String.valueOf(m)));
			}
			List<String> tokens = members != null ? out : java.util.Collections.singletonList(t);
			for (String tk : tokens) {
				if (!first) {
					sb.append(',');
				}
				sb.append(tk);
				first = false;
			}
		}
		return sb.toString();
	}

	private static boolean isNumeric(String s) {
		return s != null && !s.isEmpty() && s.chars().allMatch(Character::isDigit);
	}

	@Override
	public int seedIfAbsent() {
		Long c = groupMapper.selectCount(Wrappers.<WfOdocReceiveGroup>lambdaQuery()
			.eq(WfOdocReceiveGroup::getGroupName, "默认公共组"));
		if (c != null && c > 0) {
			return 0;
		}
		WfOdocReceiveGroup g = new WfOdocReceiveGroup();
		g.setGroupName("默认公共组");
		g.setShowOrder(1);
		g.setEnabled(1);
		g.setExtNote("示例：把常用收文单位加入本组，批量选择");
		groupMapper.insert(g);
		log.info("收发文公共组种子灌入完成, 新增 1 条");
		return 1;
	}
}
