package org.springblade.workflow.matrix.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.matrix.entity.WfMatrix;
import org.springblade.workflow.matrix.entity.WfMatrixData;
import org.springblade.workflow.matrix.entity.WfMatrixField;
import org.springblade.workflow.matrix.mapper.WfMatrixDataMapper;
import org.springblade.workflow.matrix.mapper.WfMatrixFieldMapper;
import org.springblade.workflow.matrix.mapper.WfMatrixMapper;
import org.springblade.workflow.matrix.vo.MatrixMatchVO;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * WfMatrixServiceImpl.matchUsers 纯逻辑单测（mock Mapper，不依赖 MySQL）。
 *
 * <p>锁定 E9 缺陷清单转化的硬性要求：</p>
 * <ul>
 *   <li>#1 条件集合为空 = 不匹配（返回空），绝不退化为全表匹配</li>
 *   <li>#2 条件值为空 → 该条件不参与匹配（禁止等价于 is null）</li>
 *   <li>#3 按 dataOrder 升序 + 去重保序</li>
 * </ul>
 */
class WfMatrixServiceImplTest {

	private final WfMatrixMapper matrixMapper = mock(WfMatrixMapper.class);
	private final WfMatrixFieldMapper matrixFieldMapper = mock(WfMatrixFieldMapper.class);
	private final WfMatrixDataMapper matrixDataMapper = mock(WfMatrixDataMapper.class);
	private final WfProcessDefinitionMapper definitionMapper = mock(WfProcessDefinitionMapper.class);

	private WfMatrixServiceImpl service() {
		return new WfMatrixServiceImpl(matrixMapper, matrixFieldMapper, matrixDataMapper, definitionMapper);
	}

	private void stubMatrix(Long matrixId) {
		WfMatrix m = new WfMatrix();
		m.setId(matrixId);
		m.setMatrixName("测试矩阵");
		when(matrixMapper.selectById(matrixId)).thenReturn(m);
	}

	private void stubValueField(Long matrixId, String fieldKey) {
		WfMatrixField f = new WfMatrixField();
		f.setFieldKey(fieldKey);
		f.setFieldType(1);
		when(matrixFieldMapper.selectOne(any())).thenReturn(f);
	}

	private WfMatrixData row(BigDecimal order, Map<String, Object> data) {
		WfMatrixData r = new WfMatrixData();
		r.setId(order.longValue());
		r.setMatrixId(1L);
		r.setDataOrder(order);
		Map<String, Object> map = new LinkedHashMap<>();
		map.putAll(data);
		r.setDataJson(map);
		return r;
	}

	@Test
	@DisplayName("E9#1 条件集合为空 = 不匹配（禁止退化为全表匹配）")
	void emptyConditionsNoMatch() {
		Long mid = 1L;
		stubMatrix(mid);
		stubValueField(mid, "owner");
		when(matrixDataMapper.selectList(any()))
			.thenReturn(List.of(row(BigDecimal.ONE, Map.of("dept", "10", "owner", "100"))));

		// 不传任何条件
		MatrixMatchVO vo = service().matchUsers(mid, "owner", Map.of());
		assertTrue(vo.isEmpty(), "无条件下必须返回空，不可退化为全表匹配");
	}

	@Test
	@DisplayName("E9#2 条件值为空 → 该条件不参与匹配（禁止等价于 is null），其余有效条件仍生效")
	void emptyConditionValueSkipped() {
		Long mid = 1L;
		stubMatrix(mid);
		stubValueField(mid, "owner");
		// 行：dept=10 命中，dept=99 不命中（type 维度不参与数据，仅用于验证空值跳过）
		when(matrixDataMapper.selectList(any())).thenReturn(List.of(
			row(BigDecimal.ONE, Map.of("dept", "10", "owner", "100")),
			row(BigDecimal.valueOf(2), Map.of("dept", "99", "owner", "200"))));

		// 有效条件 dept=10 + 空值条件 type=""，空值条件被跳过，仅按 dept=10 匹配
		MatrixMatchVO vo = service().matchUsers(mid, "owner", Map.of("dept", "10", "type", ""));
		assertFalse(vo.isEmpty(), "有效条件 dept=10 应命中");
		assertEquals(List.of(100L), vo.getUserIds(), "空值条件 type 被跳过，仅按 dept=10 匹配");
	}

	@Test
	@DisplayName("E9#3 AND 多条件匹配 + dataOrder 升序 + 去重保序")
	void andMatchOrderedDedup() {
		Long mid = 1L;
		stubMatrix(mid);
		stubValueField(mid, "owner");
		// 三行：仅行1、行3 同时满足 dept=10 且 type=A；行1/行3 的 owner 含重复 100
		when(matrixDataMapper.selectList(any())).thenReturn(List.of(
			row(BigDecimal.ONE, Map.of("dept", "10", "type", "A", "owner", "100,200")),
			row(BigDecimal.valueOf(2), Map.of("dept", "10", "type", "B", "owner", "300")),
			row(BigDecimal.valueOf(3), Map.of("dept", "10", "type", "A", "owner", "100,400"))));

		MatrixMatchVO vo = service().matchUsers(mid, "owner", Map.of("dept", "10", "type", "A"));
		assertFalse(vo.isEmpty());
		// 升序 + 去重：行1(100,200) 早于 行3(100,400) → 100,200,400
		assertEquals(List.of(100L, 200L, 400L), vo.getUserIds());
		assertEquals(2, vo.getHitRows(), "应命中 2 行（行1、行3）");
	}

	@Test
	@DisplayName("结果维度字段不存在或非结果维度 → 不匹配")
	void valueFieldNotResultDimension() {
		Long mid = 1L;
		stubMatrix(mid);
		// valueField 查出来 fieldType=0（条件维度），非结果维度
		WfMatrixField f = new WfMatrixField();
		f.setFieldKey("owner");
		f.setFieldType(0);
		when(matrixFieldMapper.selectOne(any())).thenReturn(f);

		MatrixMatchVO vo = service().matchUsers(mid, "owner", Map.of("dept", "10"));
		assertTrue(vo.isEmpty(), "valueField 不是结果维度时必须不匹配");
	}

	@Test
	@DisplayName("矩阵不存在 → 安全返回空（避免运行时静默无操作者无上下文）")
	void matrixNotExist() {
		when(matrixMapper.selectById(999L)).thenReturn(null);
		MatrixMatchVO vo = service().matchUsers(999L, "owner", Map.of("dept", "10"));
		assertTrue(vo.isEmpty());
	}
}
