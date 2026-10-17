package org.springblade.workflow.odoc.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.odoc.entity.WfOdocOpinion;
import org.springblade.workflow.odoc.mapper.WfOdocOpinionMapper;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WfOdocOpinionServiceImpl} 纯逻辑单测（mock Mapper，不依赖 MySQL）。
 *
 * <p>锁定：全局常用批示语种子灌入与幂等、列表、按 id 新增/更新、参数校验、删除。</p>
 */
class WfOdocOpinionServiceImplTest {

	private final WfOdocOpinionMapper opinionMapper = mock(WfOdocOpinionMapper.class);
	private final WfOdocOpinionServiceImpl service = new WfOdocOpinionServiceImpl(opinionMapper);

	@Test
	@DisplayName("种子：无全局模板时灌入 5 条常用批示语")
	void seedInsertsWhenAbsent() {
		when(opinionMapper.selectCount(any())).thenReturn(0L);
		int n = service.seedIfAbsent();
		assertEquals(5, n);
		verify(opinionMapper, times(5)).insert(any(WfOdocOpinion.class));
	}

	@Test
	@DisplayName("种子：已有全局模板则跳过")
	void seedSkipsWhenExist() {
		when(opinionMapper.selectCount(any())).thenReturn(1L);
		int n = service.seedIfAbsent();
		assertEquals(0, n);
		verify(opinionMapper, never()).insert(any(WfOdocOpinion.class));
	}

	@Test
	@DisplayName("列表：全局 + 指定流程")
	void list() {
		when(opinionMapper.selectList(any())).thenReturn(Collections.singletonList(new WfOdocOpinion()));
		assertEquals(1, service.list(1L).size());
		assertEquals(1, service.list(null).size());
	}

	@Test
	@DisplayName("save：无 id 新增")
	void saveInsert() {
		WfOdocOpinion o = new WfOdocOpinion();
		o.setPhrase("同意。");
		service.save(o);
		verify(opinionMapper, times(1)).insert(o);
	}

	@Test
	@DisplayName("save：有 id 更新")
	void saveUpdate() {
		WfOdocOpinion o = new WfOdocOpinion();
		o.setId(7L);
		o.setPhrase("已阅。");
		service.save(o);
		verify(opinionMapper, times(1)).updateById(o);
		verify(opinionMapper, never()).insert(any(WfOdocOpinion.class));
	}

	@Test
	@DisplayName("save：意见内容为空抛参数异常")
	void saveValidation() {
		assertThrows(IllegalArgumentException.class, () -> service.save(new WfOdocOpinion()));
	}

	@Test
	@DisplayName("remove：删除返回影响行数>0")
	void remove() {
		when(opinionMapper.deleteById(3L)).thenReturn(1);
		assertEquals(true, service.remove(3L));
	}
}
