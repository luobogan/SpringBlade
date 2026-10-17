package org.springblade.workflow.odoc.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.odoc.entity.WfOdocReceiveUnit;
import org.springblade.workflow.odoc.mapper.WfOdocReceiveUnitMapper;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WfOdocReceiveUnitServiceImpl} 纯逻辑单测（mock Mapper，不依赖 MySQL）。
 *
 * <p>锁定：列表/下级/按ID、按 id 新增更新、上级不能为自身的自环护栏、参数校验、删除、种子幂等。</p>
 */
class WfOdocReceiveUnitServiceImplTest {

	private final WfOdocReceiveUnitMapper unitMapper = mock(WfOdocReceiveUnitMapper.class);
	private final WfOdocReceiveUnitServiceImpl service = new WfOdocReceiveUnitServiceImpl(unitMapper);

	@Test
	@DisplayName("列表 / 启用列表")
	void list() {
		when(unitMapper.selectList(any())).thenReturn(Collections.singletonList(new WfOdocReceiveUnit()));
		assertEquals(1, service.list().size());
		assertEquals(1, service.listEnabled().size());
	}

	@Test
	@DisplayName("按ID取：命中返回，null 不查库")
	void get() {
		WfOdocReceiveUnit u = new WfOdocReceiveUnit();
		u.setId(8L);
		when(unitMapper.selectById(8L)).thenReturn(u);
		assertEquals(8L, service.get(8L).getId());
		assertNull(service.get(null));
		verify(unitMapper, never()).selectById(null);
	}

	@Test
	@DisplayName("下级单位：按 superiorId 查")
	void children() {
		when(unitMapper.selectList(any())).thenReturn(Collections.singletonList(new WfOdocReceiveUnit()));
		assertEquals(1, service.listChildren(1L).size());
	}

	@Test
	@DisplayName("save：无 id 新增，默认内部/启用")
	void saveInsert() {
		WfOdocReceiveUnit u = new WfOdocReceiveUnit();
		u.setUnitName("部门甲");
		WfOdocReceiveUnit saved = service.save(u);
		assertEquals(WfOdocReceiveUnit.COMPANY_INTERNAL, saved.getCompanyType());
		assertEquals(1, saved.getEnabled());
		verify(unitMapper, times(1)).insert(u);
	}

	@Test
	@DisplayName("save：有 id 更新")
	void saveUpdate() {
		WfOdocReceiveUnit u = new WfOdocReceiveUnit();
		u.setId(5L);
		u.setUnitName("部门甲");
		service.save(u);
		verify(unitMapper, times(1)).updateById(u);
		verify(unitMapper, never()).insert(any(WfOdocReceiveUnit.class));
	}

	@Test
	@DisplayName("save：上级设为自身 → 抛异常（自环护栏）")
	void saveSelfCycle() {
		WfOdocReceiveUnit u = new WfOdocReceiveUnit();
		u.setId(5L);
		u.setUnitName("部门甲");
		u.setSuperiorId(5L);
		assertThrows(IllegalArgumentException.class, () -> service.save(u));
	}

	@Test
	@DisplayName("save：名称为空 → 抛异常")
	void saveValidation() {
		assertThrows(IllegalArgumentException.class, () -> service.save(new WfOdocReceiveUnit()));
	}

	@Test
	@DisplayName("remove：删除返回影响行数>0")
	void remove() {
		when(unitMapper.deleteById(5L)).thenReturn(1);
		assertEquals(true, service.remove(5L));
	}

	@Test
	@DisplayName("种子：名称均缺失时灌入根单位+示例外部单位(2条)")
	void seedWhenAbsent() {
		when(unitMapper.selectCount(any())).thenReturn(0L);
		assertEquals(2, service.seedIfAbsent());
		verify(unitMapper, times(2)).insert(any(WfOdocReceiveUnit.class));
	}

	@Test
	@DisplayName("种子：已存在则跳过")
	void seedWhenExist() {
		when(unitMapper.selectCount(any())).thenReturn(1L);
		assertEquals(0, service.seedIfAbsent());
		verify(unitMapper, never()).insert(any(WfOdocReceiveUnit.class));
	}

	@Test
	@DisplayName("namesOfCsv：单位 id 串解析为名称串")
	void namesOfCsvResolvesIds() {
		WfOdocReceiveUnit u1 = new WfOdocReceiveUnit();
		u1.setId(1L);
		u1.setUnitName("本单位");
		WfOdocReceiveUnit u2 = new WfOdocReceiveUnit();
		u2.setId(2L);
		u2.setUnitName("示例外部单位");
		when(unitMapper.selectList(any())).thenReturn(java.util.Arrays.asList(u1, u2));
		assertEquals("本单位,示例外部单位", service.namesOfCsv("1,2"));
	}

	@Test
	@DisplayName("namesOfCsv：存量名称串原样返回，不查库")
	void namesOfCsvKeepsNames() {
		assertEquals("甲,乙", service.namesOfCsv("甲,乙"));
		verify(unitMapper, never()).selectList(any());
	}

	@Test
	@DisplayName("namesOfCsv：id 与名称混存，分别解析/保留")
	void namesOfCsvMixed() {
		WfOdocReceiveUnit u1 = new WfOdocReceiveUnit();
		u1.setId(1L);
		u1.setUnitName("本单位");
		when(unitMapper.selectList(any())).thenReturn(Collections.singletonList(u1));
		assertEquals("本单位,甲", service.namesOfCsv("1,甲"));
	}

	@Test
	@DisplayName("namesOfCsv：空值原样返回")
	void namesOfCsvBlank() {
		assertNull(service.namesOfCsv(null));
		assertEquals("", service.namesOfCsv(""));
	}
}
