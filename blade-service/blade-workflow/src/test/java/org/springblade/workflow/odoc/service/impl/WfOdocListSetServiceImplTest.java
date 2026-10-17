package org.springblade.workflow.odoc.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.odoc.entity.WfOdocListSet;
import org.springblade.workflow.odoc.mapper.WfOdocListSetMapper;

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
 * {@link WfOdocListSetServiceImpl} 纯逻辑单测（mock Mapper，不依赖 MySQL）。
 */
class WfOdocListSetServiceImplTest {

	private final WfOdocListSetMapper listSetMapper = mock(WfOdocListSetMapper.class);
	private final WfOdocListSetServiceImpl service = new WfOdocListSetServiceImpl(listSetMapper);

	@Test
	@DisplayName("getByCode：空编码不查库返回 null")
	void getBlank() {
		assertNull(service.getByCode(""));
		assertNull(service.getByCode(null));
		verify(listSetMapper, never()).selectOne(any());
	}

	@Test
	@DisplayName("getByCode：命中返回")
	void getHit() {
		WfOdocListSet s = new WfOdocListSet();
		s.setListCode("odoc_todo");
		when(listSetMapper.selectOne(any())).thenReturn(s);
		assertEquals("odoc_todo", service.getByCode("odoc_todo").getListCode());
	}

	@Test
	@DisplayName("save：无行则 insert，默认 enabled=1")
	void saveInsert() {
		when(listSetMapper.selectOne(any())).thenReturn(null);
		WfOdocListSet s = new WfOdocListSet();
		s.setListCode("odoc_todo");
		s.setHiddenKeys("title,urgency");
		service.save(s);
		assertEquals(1, s.getEnabled());
		verify(listSetMapper, times(1)).insert(s);
	}

	@Test
	@DisplayName("save：有行则 update 并回填 id")
	void saveUpdate() {
		WfOdocListSet exist = new WfOdocListSet();
		exist.setId(9L);
		exist.setListCode("odoc_todo");
		when(listSetMapper.selectOne(any())).thenReturn(exist);
		WfOdocListSet s = new WfOdocListSet();
		s.setListCode("odoc_todo");
		s.setHiddenKeys("title");
		WfOdocListSet saved = service.save(s);
		assertEquals(9L, saved.getId());
		verify(listSetMapper, times(1)).updateById(s);
	}

	@Test
	@DisplayName("save：空列表编码抛异常")
	void saveValidation() {
		assertThrows(IllegalArgumentException.class, () -> service.save(new WfOdocListSet()));
	}
}
