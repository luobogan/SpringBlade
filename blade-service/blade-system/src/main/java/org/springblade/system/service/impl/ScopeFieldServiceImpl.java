package org.springblade.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.system.dto.ScopeFieldBatch;
import org.springblade.system.entity.ScopeField;
import org.springblade.system.mapper.ScopeFieldMapper;
import org.springblade.system.service.IScopeFieldService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 数据权限可见字段配置服务实现
 */
@Service
public class ScopeFieldServiceImpl extends BaseServiceImpl<ScopeFieldMapper, ScopeField>
	implements IScopeFieldService {

	/** fieldName 会间接对应 SQL SELECT 列，必须是合法标识符，禁止注入字符 */
	private static final Pattern SAFE_FIELD_NAME = Pattern.compile("^[A-Za-z_][A-Za-z0-9_$]*$");

	@Override
	public List<ScopeField> listByScope(Long scopeId, Long menuId, String resourceCode) {
		LambdaQueryWrapper<ScopeField> query = new LambdaQueryWrapper<>();
		if (scopeId != null) {
			query.eq(ScopeField::getScopeId, scopeId);
		}
		if (menuId != null) {
			query.eq(ScopeField::getMenuId, menuId);
		}
		if (StringUtil.isNotBlank(resourceCode)) {
			query.eq(ScopeField::getResourceCode, resourceCode);
		}
		query.orderByAsc(ScopeField::getSort).orderByAsc(ScopeField::getId);
		return list(query);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean saveBatchConfig(ScopeFieldBatch batch) {
		if (batch == null || batch.getMenuId() == null) {
			return false;
		}
		Long scopeId = batch.getScopeId() == null ? 0L : batch.getScopeId();
		Long menuId = batch.getMenuId();
		String resourceCode = batch.getResourceCode();

		// 同名字段去重（保序），并校验合法性
		Map<String, ScopeField> incoming = new LinkedHashMap<>();
		if (Func.isNotEmpty(batch.getFields())) {
			for (ScopeField item : batch.getFields()) {
				if (item == null || StringUtil.isBlank(item.getFieldName())) {
					continue;
				}
				String name = item.getFieldName().trim();
				if (!SAFE_FIELD_NAME.matcher(name).matches()) {
					throw new IllegalArgumentException("非法字段名：" + name);
				}
				ScopeField normalized = new ScopeField();
				normalized.setScopeId(scopeId);
				normalized.setMenuId(menuId);
				normalized.setResourceCode(resourceCode);
				normalized.setFieldId(item.getFieldId());
				normalized.setFieldName(name);
				normalized.setFieldLabel(item.getFieldLabel());
				normalized.setFieldDbType(item.getFieldDbType());
				normalized.setFieldHtmlType(item.getFieldHtmlType());
				normalized.setFieldType(item.getFieldType());
				normalized.setDescription(item.getDescription());
				normalized.setIsVisible(item.getIsVisible() == null ? 1 : item.getIsVisible());
				normalized.setIsEditable(item.getIsEditable() == null ? 0 : item.getIsEditable());
				normalized.setSort(item.getSort() == null ? incoming.size() : item.getSort());
				normalized.setWidth(item.getWidth());
				normalized.setAlign(item.getAlign());
				incoming.put(name, normalized);
			}
		}

		// 已有记录（@TableLogic 自动过滤已逻辑删除行）
		List<ScopeField> existing = list(new LambdaQueryWrapper<ScopeField>()
			.eq(ScopeField::getScopeId, scopeId)
			.eq(ScopeField::getMenuId, menuId));
		Map<String, ScopeField> byName = new LinkedHashMap<>();
		for (ScopeField row : existing) {
			byName.put(row.getFieldName(), row);
		}

		List<ScopeField> toInsert = new ArrayList<>();
		for (ScopeField item : incoming.values()) {
			ScopeField old = byName.get(item.getFieldName());
			if (old != null) {
				item.setId(old.getId());
				updateById(item);
				byName.remove(item.getFieldName());
			} else {
				toInsert.add(item);
			}
		}
		if (!toInsert.isEmpty()) {
			saveBatch(toInsert);
		}
		// 本次未提交的字段逻辑删除
		for (ScopeField stale : byName.values()) {
			removeById(stale.getId());
		}
		return true;
	}
}
