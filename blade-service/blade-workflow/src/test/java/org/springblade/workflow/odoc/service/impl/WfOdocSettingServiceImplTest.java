package org.springblade.workflow.odoc.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.odoc.entity.WfOdocSetting;
import org.springblade.workflow.odoc.mapper.WfOdocSettingMapper;

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
 * {@link WfOdocSettingServiceImpl} 单例设置纯逻辑单测（mock Mapper，不依赖 MySQL）。
 */
class WfOdocSettingServiceImplTest {

	private final WfOdocSettingMapper settingMapper = mock(WfOdocSettingMapper.class);
	private final WfOdocSettingServiceImpl service = new WfOdocSettingServiceImpl(settingMapper);

	@Test
	@DisplayName("get：无行时返回默认值（不落库）")
	void getDefaultsWhenAbsent() {
		when(settingMapper.selectList(any())).thenReturn(Collections.emptyList());
		WfOdocSetting s = service.get();
		assertEquals(0, s.getIsOpenOfd());
		assertEquals(0, s.getDefaultLayoutFormat());
		assertEquals(1, s.getDefaultMustTaohong());
		assertEquals(0, s.getDefaultSealRequired());
		verify(settingMapper, never()).insert(any(WfOdocSetting.class));
	}

	@Test
	@DisplayName("get：有行时返回落库值")
	void getExisting() {
		WfOdocSetting row = new WfOdocSetting();
		row.setId(1L);
		row.setIsOpenOfd(1);
		when(settingMapper.selectList(any())).thenReturn(Collections.singletonList(row));
		assertEquals(1, service.get().getIsOpenOfd());
	}

	@Test
	@DisplayName("save：无行则 insert")
	void saveInsert() {
		when(settingMapper.selectList(any())).thenReturn(Collections.emptyList());
		WfOdocSetting s = new WfOdocSetting();
		s.setIsOpenOfd(1);
		service.save(s);
		verify(settingMapper, times(1)).insert(s);
	}

	@Test
	@DisplayName("save：有行则 update 并回填 id")
	void saveUpdate() {
		WfOdocSetting row = new WfOdocSetting();
		row.setId(7L);
		when(settingMapper.selectList(any())).thenReturn(Collections.singletonList(row));
		WfOdocSetting s = new WfOdocSetting();
		s.setIsOpenOfd(1);
		WfOdocSetting saved = service.save(s);
		assertEquals(7L, saved.getId());
		verify(settingMapper, times(1)).updateById(s);
		verify(settingMapper, never()).insert(any(WfOdocSetting.class));
	}

	@Test
	@DisplayName("save：null 抛异常")
	void saveNull() {
		assertThrows(IllegalArgumentException.class, () -> service.save(null));
	}

	@Test
	@DisplayName("seed：无行灌入 1 条，有行跳过")
	void seed() {
		when(settingMapper.selectList(any())).thenReturn(Collections.emptyList());
		assertEquals(1, service.seedIfAbsent());
		verify(settingMapper, times(1)).insert(any(WfOdocSetting.class));

		WfOdocSettingMapper m2 = mock(WfOdocSettingMapper.class);
		when(m2.selectList(any())).thenReturn(Collections.singletonList(new WfOdocSetting()));
		assertEquals(0, new WfOdocSettingServiceImpl(m2).seedIfAbsent());
		verify(m2, never()).insert(any(WfOdocSetting.class));
	}
}
