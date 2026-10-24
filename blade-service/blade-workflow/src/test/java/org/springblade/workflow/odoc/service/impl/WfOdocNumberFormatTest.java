package org.springblade.workflow.odoc.service.impl;

import org.junit.jupiter.api.Test;
import org.springblade.workflow.odoc.entity.WfOdocDict;
import org.springblade.workflow.odoc.entity.WfOdocNumber;
import org.springblade.workflow.odoc.mapper.WfOdocDictMapper;
import org.springblade.workflow.odoc.mapper.WfOdocNumberMapper;
import org.springblade.workflow.odoc.service.IWfOdocNumberReservedService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link WfOdocNumberServiceImpl} 取号代字解析回归：机构代字独立建模后，
 * 取号应优先用 {@code orgAbbrId} 关联的字典 name，未关联/字典缺失时回退自由文本 {@code prefix}。
 *
 * <p>纯单测（Mockito 桩），通过 {@code preview(defId)} 触达 {@code format()} → {@code resolvePrefix()}。</p>
 */
class WfOdocNumberFormatTest {

	private WfOdocNumber rule(boolean withAbbr, Long abbrId, String prefix) {
		WfOdocNumber r = new WfOdocNumber();
		r.setId(100L);
		r.setIsOpen(1);
		r.setShowYear(0);            // 不含年份，便于断言代字本身
		r.setPrefix(prefix);
		if (withAbbr) {
			r.setOrgAbbrId(abbrId);
		}
		return r;
	}

	private WfOdocNumberServiceImpl svc(WfOdocNumber rule, WfOdocDict abbrDict) {
		WfOdocNumberMapper numberMapper = mock(WfOdocNumberMapper.class);
		WfOdocDictMapper dictMapper = mock(WfOdocDictMapper.class);
		IWfOdocNumberReservedService reserved = mock(IWfOdocNumberReservedService.class);
		when(numberMapper.selectList(any())).thenReturn(List.of(rule));
		if (abbrDict != null) {
			when(dictMapper.selectById(abbrDict.getId())).thenReturn(abbrDict);
		}
		return new WfOdocNumberServiceImpl(numberMapper, reserved, dictMapper);
	}

	@Test
	void preview_usesOrgAbbrDictName_whenLinked() {
		WfOdocDict abbr = new WfOdocDict();
		abbr.setId(900L);
		abbr.setName("演示政发");
		WfOdocNumber r = rule(true, 900L, "X政发(旧)");

		String number = svc(r, abbr).preview(1L);

		assertThat(number).startsWith("演示政发");
		assertThat(number).doesNotContain("X政发(旧)"); // 字典优先，旧自由文本不出现
	}

	@Test
	void preview_fallsBackToPrefix_whenNoAbbrLinked() {
		WfOdocNumber r = rule(false, null, "X政发");

		String number = svc(r, null).preview(1L);

		assertThat(number).startsWith("X政发");
	}

	@Test
	void preview_fallsBackToPrefix_whenAbbrDictMissing() {
		// 关联了 orgAbbrId，但字典查不到（被删/跨租户不可见）→ 回退 prefix
		WfOdocNumber r = rule(true, 999L, "X政发");

		String number = svc(r, null).preview(1L);

		assertThat(number).startsWith("X政发");
	}
}
