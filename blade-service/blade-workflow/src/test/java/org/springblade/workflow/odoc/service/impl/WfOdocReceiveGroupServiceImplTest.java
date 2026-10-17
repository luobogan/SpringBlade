package org.springblade.workflow.odoc.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.odoc.entity.WfOdocReceiveGroup;
import org.springblade.workflow.odoc.mapper.WfOdocReceiveGroupMapper;

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
 * {@link WfOdocReceiveGroupServiceImpl} 纯逻辑单测（mock Mapper，不依赖 MySQL）。
 *
 * <p>锁定：列表/启用、按 id 新增更新、参数校验、删除、默认公共组种子幂等。</p>
 */
class WfOdocReceiveGroupServiceImplTest {

	private final WfOdocReceiveGroupMapper groupMapper = mock(WfOdocReceiveGroupMapper.class);
	private final WfOdocReceiveGroupServiceImpl service = new WfOdocReceiveGroupServiceImpl(groupMapper);

	@Test
	@DisplayName("列表 / 启用列表")
	void list() {
		when(groupMapper.selectList(any())).thenReturn(Collections.singletonList(new WfOdocReceiveGroup()));
		assertEquals(1, service.list().size());
		assertEquals(1, service.listEnabled().size());
	}

	@Test
	@DisplayName("save：无 id 新增，默认启用")
	void saveInsert() {
		WfOdocReceiveGroup g = new WfOdocReceiveGroup();
		g.setGroupName("抄送组A");
		g.setUnitIds("1,2,3");
		WfOdocReceiveGroup saved = service.save(g);
		assertEquals(1, saved.getEnabled());
		verify(groupMapper, times(1)).insert(g);
	}

	@Test
	@DisplayName("save：有 id 更新")
	void saveUpdate() {
		WfOdocReceiveGroup g = new WfOdocReceiveGroup();
		g.setId(9L);
		g.setGroupName("抄送组A");
		service.save(g);
		verify(groupMapper, times(1)).updateById(g);
		verify(groupMapper, never()).insert(any(WfOdocReceiveGroup.class));
	}

	@Test
	@DisplayName("save：名称为空 → 抛异常")
	void saveValidation() {
		assertThrows(IllegalArgumentException.class, () -> service.save(new WfOdocReceiveGroup()));
	}

	@Test
	@DisplayName("remove：删除返回影响行数>0")
	void remove() {
		when(groupMapper.deleteById(9L)).thenReturn(1);
		assertEquals(true, service.remove(9L));
	}

	@Test
	@DisplayName("种子：无默认公共组时灌入 1 条")
	void seedWhenAbsent() {
		when(groupMapper.selectCount(any())).thenReturn(0L);
		assertEquals(1, service.seedIfAbsent());
		verify(groupMapper, times(1)).insert(any(WfOdocReceiveGroup.class));
	}

	@Test
	@DisplayName("种子：已有默认公共组则跳过")
	void seedWhenExist() {
		when(groupMapper.selectCount(any())).thenReturn(1L);
		assertEquals(0, service.seedIfAbsent());
		verify(groupMapper, never()).insert(any(WfOdocReceiveGroup.class));
	}

	private WfOdocReceiveGroup group(Long id, String unitIds) {
		WfOdocReceiveGroup g = new WfOdocReceiveGroup();
		g.setId(id);
		g.setGroupName("组" + id);
		g.setUnitIds(unitIds);
		return g;
	}

	@Test
	@DisplayName("expandToUnitIds：公共组 id 展开为成员单位 id")
	void expandGroup() {
		when(groupMapper.selectList(any())).thenReturn(Collections.singletonList(group(100L, "1,2")));
		assertEquals("1,2", service.expandToUnitIds("100"));
	}

	@Test
	@DisplayName("expandToUnitIds：非组 id（单位 id）原样保留")
	void expandKeepsUnitId() {
		when(groupMapper.selectList(any())).thenReturn(Collections.singletonList(group(100L, "1,2")));
		assertEquals("999", service.expandToUnitIds("999"));
	}

	@Test
	@DisplayName("expandToUnitIds：组 id 与单位 id 混存")
	void expandMixed() {
		when(groupMapper.selectList(any())).thenReturn(Collections.singletonList(group(100L, "1,2")));
		assertEquals("1,2,999", service.expandToUnitIds("100,999"));
	}

	@Test
	@DisplayName("expandToUnitIds：存量名称串原样返回，不查库")
	void expandKeepsNames() {
		assertEquals("甲,乙", service.expandToUnitIds("甲,乙"));
		verify(groupMapper, never()).selectList(any());
	}

	@Test
	@DisplayName("expandToUnitIds：空值原样返回")
	void expandBlank() {
		assertNull(service.expandToUnitIds(null));
		assertEquals("", service.expandToUnitIds(""));
	}
}
