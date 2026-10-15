package org.springblade.workflow.matrix.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.matrix.entity.WfMatrix;
import org.springblade.workflow.matrix.entity.WfMatrixData;
import org.springblade.workflow.matrix.entity.WfMatrixField;
import org.springblade.workflow.matrix.mapper.WfMatrixDataMapper;
import org.springblade.workflow.matrix.mapper.WfMatrixFieldMapper;
import org.springblade.workflow.matrix.mapper.WfMatrixMapper;
import org.springblade.workflow.matrix.service.IWfMatrixService;
import org.springblade.workflow.matrix.vo.MatrixMatchVO;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 矩阵服务实现
 *
 * <p>所有写操作均 {@link Transactional}（E9 教训 #9：数据行保存/批量/删除全无事务）。</p>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class WfMatrixServiceImpl implements IWfMatrixService {

	/** 字段Key白名单：首字符必须字母，仅允许字母数字下划线（对齐 E9 fieldNameCheck） */
	private static final Pattern FIELD_KEY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");

	/** 字段Key/显示名长度上限（对齐 E9 SPA 侧 length:25 并适当放宽） */
	private static final int FIELD_KEY_MAX_LEN = 50;
	private static final int DISPLAY_NAME_MAX_LEN = 100;

	/**
	 * 保留字黑名单：含 SQL 关键字 + 本表固定列名。
	 *
	 * <p>E9 把字段名直接拼进 {@code create table}，导致保留字冲突与注入
	 * （{@code AddMatrixDesignCmd.java:188-265}）。本实现不拼 DDL，但仍拦截，
	 * 避免与 data_json 的固定键及后续可能的物理列冲突。</p>
	 */
	private static final Set<String> RESERVED_KEYS = Set.of(
		"id", "uuid", "dataorder", "data_order", "tenant_id", "tenantid",
		"is_deleted", "isdeleted", "status", "create_time", "create_user", "create_dept",
		"update_time", "update_user", "select", "insert", "update", "delete", "from",
		"where", "table", "order", "group", "by", "and", "or", "null", "key", "index",
		"drop", "alter", "create", "join", "union", "values", "set", "into", "on"
	);

	private final WfMatrixMapper matrixMapper;
	private final WfMatrixFieldMapper matrixFieldMapper;
	private final WfMatrixDataMapper matrixDataMapper;
	private final WfProcessDefinitionMapper definitionMapper;

	/**
	 * 显式填充租户与创建审计字段。
	 *
	 * <p>本环境 {@code TenantEntity} 的 {@code tenant_id} 未被 MetaObjectHandler 自动填充，
	 * 而库表 {@code tenant_id NOT NULL} 无默认值，必须显式写入，否则 INSERT 报
	 * "Field 'tenant_id' doesn't have a default value"。创建人/时间一并填充，保持与其它模块一致。</p>
	 */
	private void fillCreate(TenantEntity e) {
		e.setTenantId(SecureUtil.getTenantId());
		e.setCreateUser(SecureUtil.getUserId());
		e.setCreateTime(new Date());
		// 逻辑删除字段：MP 不会在 INSERT 时自动填充，必须显式置 0，否则 is_deleted=NULL
		// 会被查询的 is_deleted=0 过滤条件静默吞掉（表现为「矩阵不存在」/匹配空）。
		e.setIsDeleted(0);
	}

	// ===================== 矩阵主表 =====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public WfMatrix createMatrix(String matrixName, String matrixDesc) {
		if (matrixName == null || matrixName.isBlank()) {
			throw new ServiceException("矩阵名称不能为空");
		}
		String name = matrixName.trim();
		if (name.length() > DISPLAY_NAME_MAX_LEN) {
			throw new ServiceException("矩阵名称长度不能超过 " + DISPLAY_NAME_MAX_LEN);
		}
		// 同租户内重名拒绝（E9 教训：新增矩阵名称无校验、无重名校验）
		Long dup = matrixMapper.selectCount(Wrappers.<WfMatrix>lambdaQuery()
			.eq(WfMatrix::getMatrixName, name));
		if (dup != null && dup > 0) {
			throw new ServiceException("矩阵名称已存在: " + name);
		}
		WfMatrix m = new WfMatrix();
		m.setMatrixName(name);
		m.setMatrixDesc(matrixDesc == null ? null : matrixDesc.trim());
		m.setIsSystem(0);
		m.setStatus(1);
		fillCreate(m);
		matrixMapper.insert(m);
		log.info("[matrix] 新建矩阵 matrixId={}, name={}", m.getId(), name);
		return m;
	}

	@Override
	public WfMatrix getMatrix(Long matrixId) {
		if (matrixId == null) {
			return null;
		}
		return matrixMapper.selectById(matrixId);
	}

	@Override
	public List<WfMatrix> listMatrix() {
		return matrixMapper.selectList(Wrappers.<WfMatrix>lambdaQuery()
			.eq(WfMatrix::getStatus, 1)
			.orderByDesc(WfMatrix::getId));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeMatrix(Long matrixId) {
		WfMatrix m = matrixMapper.selectById(matrixId);
		if (m == null) {
			throw new ServiceException("矩阵不存在: " + matrixId);
		}
		// 强校验：BPMN 中仍引用则拒绝删除
		// ⚠️ 采用「matrixId 出现在 bpmn_xml 中即视为引用」的宽松匹配：雪花ID全局唯一，
		//    宁可误报（拦住删除）也不漏报（留下悬空引用导致运行时静默无操作者）。
		Long refs = definitionMapper.selectCount(Wrappers.<WfProcessDefinition>lambdaQuery()
			.like(WfProcessDefinition::getBpmnXml, String.valueOf(matrixId)));
		if (refs != null && refs > 0) {
			throw new ServiceException("矩阵「" + m.getMatrixName() + "」已被 " + refs
				+ " 个流程定义引用，请先在流程设计器中移除矩阵操作者配置后再删除");
		}
		matrixFieldMapper.delete(Wrappers.<WfMatrixField>lambdaQuery().eq(WfMatrixField::getMatrixId, matrixId));
		matrixDataMapper.delete(Wrappers.<WfMatrixData>lambdaQuery().eq(WfMatrixData::getMatrixId, matrixId));
		matrixMapper.deleteById(matrixId);
		log.info("[matrix] 删除矩阵 matrixId={}, name={}", matrixId, m.getMatrixName());
	}

	// ===================== 字段定义 =====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public List<WfMatrixField> saveFields(Long matrixId, List<WfMatrixField> fields) {
		WfMatrix m = matrixMapper.selectById(matrixId);
		if (m == null) {
			throw new ServiceException("矩阵不存在: " + matrixId);
		}
		if (fields == null || fields.isEmpty()) {
			throw new ServiceException("字段定义不能为空");
		}
		// E9 教训 #11：结果维度可被删到 0 个（前端删除保护遍历错数组）→ 后端必须强校验
		long condCount = fields.stream().filter(f -> Objects.equals(0, f.getFieldType())).count();
		long valueCount = fields.stream().filter(f -> Objects.equals(1, f.getFieldType())).count();
		if (condCount < 1) {
			throw new ServiceException("至少需要 1 个条件维度（fieldType=0）");
		}
		if (valueCount < 1) {
			throw new ServiceException("至少需要 1 个结果维度（fieldType=1）");
		}

		Set<String> seen = new LinkedHashSet<>();
		List<WfMatrixField> toSave = new ArrayList<>();
		for (WfMatrixField f : fields) {
			validateField(f);
			if (!seen.add(f.getFieldKey())) {
				throw new ServiceException("字段Key重复: " + f.getFieldKey());
			}
			f.setMatrixId(matrixId);
			toSave.add(f);
		}

		// 覆盖式：先物理删除后插入（同一事务，失败整体回滚）。
		// 必须物理删除：WfMatrixField 继承 TenantEntity → @TableLogic 让 delete() 变软删除，
		// 而 uk_wf_matrix_field_key 不含 is_deleted，软删除行仍占唯一键 → INSERT 撞键。
		matrixFieldMapper.physicalDeleteByMatrixId(matrixId);
		for (WfMatrixField f : toSave) {
			f.setId(null);
			fillCreate(f);
			matrixFieldMapper.insert(f);
		}
		log.info("[matrix] 保存字段定义 matrixId={}, 条件维度={}, 结果维度={}", matrixId, condCount, valueCount);
		return listFields(matrixId);
	}

	@Override
	public List<WfMatrixField> listFields(Long matrixId) {
		return matrixFieldMapper.selectList(Wrappers.<WfMatrixField>lambdaQuery()
			.eq(WfMatrixField::getMatrixId, matrixId)
			.orderByAsc(WfMatrixField::getFieldType)
			.orderByAsc(WfMatrixField::getFieldOrder)
			.orderByAsc(WfMatrixField::getId));
	}

	/** 字段定义强校验（E9 教训 #6：字段名无校验 → 注入/保留字/同名列） */
	private void validateField(WfMatrixField f) {
		if (f.getDisplayName() == null || f.getDisplayName().isBlank()) {
			throw new ServiceException("字段显示名不能为空");
		}
		if (f.getDisplayName().trim().length() > DISPLAY_NAME_MAX_LEN) {
			throw new ServiceException("字段显示名「" + f.getDisplayName() + "」长度不能超过 " + DISPLAY_NAME_MAX_LEN);
		}
		String key = f.getFieldKey();
		if (key == null || key.isBlank()) {
			throw new ServiceException("字段Key不能为空");
		}
		key = key.trim();
		if (key.length() > FIELD_KEY_MAX_LEN) {
			throw new ServiceException("字段Key「" + key + "」长度不能超过 " + FIELD_KEY_MAX_LEN);
		}
		if (!FIELD_KEY_PATTERN.matcher(key).matches()) {
			throw new ServiceException("字段Key「" + key + "」不合法：首字符须为字母，仅允许字母、数字、下划线");
		}
		if (RESERVED_KEYS.contains(key.toLowerCase())) {
			throw new ServiceException("字段Key「" + key + "」为保留字，请更换");
		}
		if (!Objects.equals(0, f.getFieldType()) && !Objects.equals(1, f.getFieldType())) {
			throw new ServiceException("字段「" + key + "」类型非法：fieldType 只能是 0(条件维度) 或 1(结果维度)");
		}
	}

	// ===================== 数据行 =====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public WfMatrixData saveDataRow(Long matrixId, BigDecimal dataOrder, Map<String, Object> values) {
		List<WfMatrixField> fields = listFields(matrixId);
		if (fields.isEmpty()) {
			throw new ServiceException("请先保存字段定义，再维护数据");
		}
		if (dataOrder == null) {
			throw new ServiceException("行序 dataOrder 不能为空");
		}
		Map<String, WfMatrixField> byKey = fields.stream()
			.collect(Collectors.toMap(WfMatrixField::getFieldKey, f -> f, (a, b) -> a, LinkedHashMap::new));

		// key 必须已定义
		if (values != null) {
			for (String k : values.keySet()) {
				if (!byKey.containsKey(k)) {
					throw new ServiceException("未知字段Key: " + k + "（请先在字段定义中添加）");
				}
			}
		}

		// E9 教训：每行条件维度至少一个有值（E9 前端后端均未校验）
		boolean anyCond = false;
		for (WfMatrixField f : fields) {
			if (Objects.equals(0, f.getFieldType())) {
				String v = values == null ? null : str(values.get(f.getFieldKey()));
				if (v != null && !v.isBlank()) {
					anyCond = true;
					break;
				}
			}
		}
		if (!anyCond) {
			throw new ServiceException("每行至少需要一个条件维度有值，否则无法按维度定位办理人");
		}

		// 结果维度值必须是数字ID串（人员ID），避免脏数据
		for (WfMatrixField f : fields) {
			if (Objects.equals(1, f.getFieldType())) {
				String v = str(values == null ? null : values.get(f.getFieldKey()));
				if (v != null && !v.isBlank()) {
					for (String tok : splitTokens(v)) {
						if (!tok.chars().allMatch(Character::isDigit)) {
							throw new ServiceException("结果维度「" + f.getDisplayName() + "」的值必须是人员ID（数字），当前值: " + v);
						}
					}
				}
			}
		}

		// 行序唯一：(matrix_id, data_order) 有唯一索引且不含 is_deleted。
		// @TableLogic 让软删除行仍占用唯一键 → 先拒绝重复的活跃行序，再物理清理软删除残留，避免重复键。
		Long activeSameOrder = matrixDataMapper.selectCount(Wrappers.<WfMatrixData>lambdaQuery()
			.eq(WfMatrixData::getMatrixId, matrixId)
			.eq(WfMatrixData::getDataOrder, dataOrder));
		if (activeSameOrder != null && activeSameOrder > 0) {
			throw new ServiceException("行序 " + dataOrder + " 已存在，请更换行序");
		}
		matrixDataMapper.physicalDeleteByOrder(matrixId, dataOrder);

		WfMatrixData row = new WfMatrixData();
		row.setMatrixId(matrixId);
		row.setDataOrder(dataOrder);
		row.setDataJson(values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values));
		fillCreate(row);
		matrixDataMapper.insert(row);
		log.info("[matrix] 保存数据行 matrixId={}, dataId={}, dataOrder={}", matrixId, row.getId(), dataOrder);
		return row;
	}

	@Override
	public List<WfMatrixData> listData(Long matrixId) {
		return matrixDataMapper.selectList(Wrappers.<WfMatrixData>lambdaQuery()
			.eq(WfMatrixData::getMatrixId, matrixId)
			.orderByAsc(WfMatrixData::getDataOrder));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeDataRow(Long dataId) {
		WfMatrixData row = matrixDataMapper.selectById(dataId);
		if (row == null) {
			throw new ServiceException("数据行不存在: " + dataId);
		}
		// 物理删除：软删除会残留并占用 uk_wf_matrix_data_order 唯一键，导致复用该行序重报重复键
		matrixDataMapper.physicalDeleteById(dataId);
		log.info("[matrix] 删除数据行 dataId={}, matrixId={}", dataId, row.getMatrixId());
	}

	@Override
	public BigDecimal nextDataOrder(Long matrixId) {
		List<WfMatrixData> rows = matrixDataMapper.selectList(Wrappers.<WfMatrixData>lambdaQuery()
			.select(WfMatrixData::getDataOrder)
			.eq(WfMatrixData::getMatrixId, matrixId)
			.orderByDesc(WfMatrixData::getDataOrder)
			.last("LIMIT 1"));
		if (rows.isEmpty() || rows.get(0).getDataOrder() == null) {
			return BigDecimal.ONE;
		}
		// E9 用 max+1 且无唯一约束导致并发冲突；这里 (matrix_id, data_order) 有唯一索引兜底，
		// 调用方遇到重复键应重新取值重试。
		return rows.get(0).getDataOrder().add(BigDecimal.ONE);
	}

	// ===================== 运行时取人 =====================

	@Override
	public MatrixMatchVO matchUsers(Long matrixId, String valueFieldKey, Map<String, String> condValues) {
		MatrixMatchVO result = new MatrixMatchVO(matrixId, valueFieldKey, condValues);
		if (matrixId == null || valueFieldKey == null || valueFieldKey.isBlank()) {
			log.warn("[matrix] 缺少矩阵ID或结果维度字段，按不匹配处理. matrixId={}, valueField={}", matrixId, valueFieldKey);
			return result;
		}

		WfMatrix m = matrixMapper.selectById(matrixId);
		if (m == null) {
			// E9 教训 #3/#5：矩阵被删后运行时静默无操作者 → 这里必须 warn 可定位
			log.warn("[matrix] 矩阵不存在（可能已删除），按不匹配处理. matrixId={}, valueField={}", matrixId, valueFieldKey);
			return result;
		}
		result.setMatrixName(m.getMatrixName());

		// E9 教训 #1：条件集合为空 = 不匹配，绝不退化为全表匹配
		if (condValues == null || condValues.isEmpty()) {
			log.warn("[matrix] 矩阵未配置任何条件维度，按不匹配处理（禁止退化为全表匹配）. matrixId={}, name={}",
				matrixId, m.getMatrixName());
			return result;
		}

		// valueField 必须是「结果维度」
		WfMatrixField valueField = matrixFieldMapper.selectOne(Wrappers.<WfMatrixField>lambdaQuery()
			.eq(WfMatrixField::getMatrixId, matrixId)
			.eq(WfMatrixField::getFieldKey, valueFieldKey)
			.last("LIMIT 1"));
		if (valueField == null || !Objects.equals(1, valueField.getFieldType())) {
			log.warn("[matrix] 结果维度字段不存在或类型不是结果维度. matrixId={}, valueField={}", matrixId, valueFieldKey);
			return result;
		}

		// E9 对齐：空值条件**不跳过**，按"匹配空行"处理（等价 col is null or col in ('')）；
		// 仅"完全未配置条件"（上面 condValues 为空）才不匹配。避免空值条件被忽略导致多派。
		Map<String, String> effective = new LinkedHashMap<>();
		for (Map.Entry<String, String> e : condValues.entrySet()) {
			String v = e.getValue();
			effective.put(e.getKey(), v == null ? "" : v.trim());
		}
		result.setEffectiveConditions(effective);

		// 加载矩阵行（E9 教训 #3：必须 ORDER BY + 去重）
		List<WfMatrixData> rows = matrixDataMapper.selectList(Wrappers.<WfMatrixData>lambdaQuery()
			.eq(WfMatrixData::getMatrixId, matrixId)
			.orderByAsc(WfMatrixData::getDataOrder));

		Set<Long> users = new LinkedHashSet<>();
		int hit = 0;
		for (WfMatrixData row : rows) {
			if (rowMatches(row, effective)) {
				hit++;
				Map<String, Object> data = row.getDataJson();
				if (data != null) {
					for (String tok : splitTokens(str(data.get(valueFieldKey)))) {
						try {
							users.add(Long.valueOf(tok));
						} catch (NumberFormatException ex) {
							log.warn("[matrix] 结果维度值非数字，已忽略. matrixId={}, dataId={}, value={}",
								matrixId, row.getId(), tok);
						}
					}
				}
			}
		}
		result.setHitRows(hit);
		result.setUserIds(new ArrayList<>(users));

		if (hit == 0) {
			// 带完整上下文，便于排查（E9 缺失）
			log.warn("[matrix] 未命中任何数据行. {}", result.describe());
		} else {
			log.debug("[matrix] 匹配完成. {}", result.describe());
		}
		return result;
	}

	/** 单行是否满足全部生效条件（AND） */
	private boolean rowMatches(WfMatrixData row, Map<String, String> effective) {
		Map<String, Object> data = row.getDataJson();
		if (data == null || data.isEmpty()) {
			return false;
		}
		for (Map.Entry<String, String> cond : effective.entrySet()) {
			Set<String> rowVals = splitTokens(str(data.get(cond.getKey())));
			Set<String> condVals = splitTokens(cond.getValue());
			if (condVals.isEmpty()) {
				// 空值条件：仅匹配行内该维度也为空的行（E9: col is null or col in ('')）
				if (!rowVals.isEmpty()) {
					return false;
				}
			} else {
				if (rowVals.isEmpty()) {
					return false;
				}
				// 任一命中即满足该条件（E9 的 in / like '%,v,%' 语义），全部条件都满足才算命中
				boolean any = condVals.stream().anyMatch(rowVals::contains);
				if (!any) {
					return false;
				}
			}
		}
		return true;
	}

	private static String str(Object o) {
		return o == null ? null : String.valueOf(o);
	}

	/** 逗号/分号/空白分隔取值集合（保序去重） */
	private static Set<String> splitTokens(String s) {
		if (s == null || s.isBlank()) {
			return Set.of();
		}
		return Arrays.stream(s.split("[,，;；\\s]+"))
			.map(String::trim)
			.filter(x -> !x.isEmpty())
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
