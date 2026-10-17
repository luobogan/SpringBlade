package org.springblade.workflow.odoc.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.odoc.entity.WfOdocDict;
import org.springblade.workflow.odoc.mapper.WfOdocDictMapper;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WfOdocDictServiceImpl} 纯逻辑单测（mock Mapper，不依赖 MySQL）。
 *
 * <p>锁定：标准字典种子灌入与幂等、按类型/编码查询、nameOf 存量兼容、
 * 按 (dictType,code) upsert、参数校验、删除。</p>
 */
class WfOdocDictServiceImplTest {

	private final WfOdocDictMapper dictMapper = mock(WfOdocDictMapper.class);
	private final WfOdocDictServiceImpl service = new WfOdocDictServiceImpl(dictMapper);

	@Test
	@DisplayName("种子：全部缺失时灌入 22 条标准字典(秘密4+紧急3+文种15)")
	void seedInsertsStandardWhenAbsent() {
		when(dictMapper.selectOne(any())).thenReturn(null);
		int n = service.seedIfAbsent();
		assertEquals(22, n);
		verify(dictMapper, times(22)).insert(any(WfOdocDict.class));
	}

	@Test
	@DisplayName("种子：已存在则跳过，不重复灌入")
	void seedSkipsWhenAllExist() {
		WfOdocDict exist = new WfOdocDict();
		exist.setId(1L);
		when(dictMapper.selectOne(any())).thenReturn(exist);
		int n = service.seedIfAbsent();
		assertEquals(0, n);
		verify(dictMapper, never()).insert(any(WfOdocDict.class));
	}

	@Test
	@DisplayName("按类型列表 / 按编码查询")
	void listAndGetByCode() {
		WfOdocDict a = new WfOdocDict();
		a.setCode("机密");
		when(dictMapper.selectList(any())).thenReturn(Arrays.asList(a));
		assertEquals(1, service.listByType("secret_level").size());

		when(dictMapper.selectOne(any())).thenReturn(a);
		assertSame(a, service.getByCode("secret_level", "机密"));
	}

	@Test
	@DisplayName("nameOf：命中返回显示名，未命中原样返回 code（存量兼容）")
	void nameOf() {
		WfOdocDict d = new WfOdocDict();
		d.setCode("机密");
		d.setName("机密");
		when(dictMapper.selectOne(any())).thenReturn(d);
		assertEquals("机密", service.nameOf("secret_level", "机密"));

		when(dictMapper.selectOne(any())).thenReturn(null);
		assertEquals("未知值", service.nameOf("secret_level", "未知值"));
	}

	@Test
	@DisplayName("save：不存在则 insert")
	void saveInsert() {
		when(dictMapper.selectOne(any())).thenReturn(null);
		WfOdocDict d = new WfOdocDict();
		d.setDictType("topic_type");
		d.setCode("T1");
		d.setName("主题一");
		service.save(d);
		verify(dictMapper, times(1)).insert(d);
	}

	@Test
	@DisplayName("save：已存在则按 (dictType,code) update，并回填 id")
	void saveUpsert() {
		WfOdocDict exist = new WfOdocDict();
		exist.setId(99L);
		when(dictMapper.selectOne(any())).thenReturn(exist);
		WfOdocDict d = new WfOdocDict();
		d.setDictType("topic_type");
		d.setCode("T1");
		d.setName("主题一(改)");
		WfOdocDict saved = service.save(d);
		assertEquals(99L, saved.getId());
		verify(dictMapper, times(1)).updateById(d);
		verify(dictMapper, never()).insert(any(WfOdocDict.class));
	}

	@Test
	@DisplayName("save：缺编码/显示名抛参数异常")
	void saveValidation() {
		assertThrows(IllegalArgumentException.class, () -> service.save(new WfOdocDict()));
	}

	@Test
	@DisplayName("remove：删除返回影响行数>0")
	void remove() {
		when(dictMapper.deleteById(5L)).thenReturn(1);
		assertEquals(true, service.remove(5L));
	}

	@Test
	@DisplayName("空类型列表返回空集合而非 null")
	void emptyTypeReturnsEmpty() {
		when(dictMapper.selectList(any())).thenReturn(Collections.emptyList());
		assertEquals(0, service.listByType("").size());
	}
}
