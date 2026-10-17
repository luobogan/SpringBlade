package org.springblade.workflow.odoc.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.odoc.entity.WfOdocReceiveGroup;
import org.springblade.workflow.odoc.entity.WfOdocReceiveUnit;
import org.springblade.workflow.odoc.entity.WfOdocSecretRule;
import org.springblade.workflow.odoc.mapper.WfOdocReceiveUnitMapper;
import org.springblade.workflow.odoc.mapper.WfOdocSecretRuleMapper;
import org.springblade.workflow.odoc.service.IWfOdocReceiveGroupService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WfOdocSecretRuleServiceImpl} 纯逻辑单测（mock Mapper，不依赖 MySQL）。
 *
 * <p>锁定：密级越限才拒、无规则/密级不可判/无收文单位均放行、单位名解析、save 校验、删除。</p>
 */
class WfOdocSecretRuleServiceImplTest {

	private final WfOdocSecretRuleMapper ruleMapper = mock(WfOdocSecretRuleMapper.class);
	private final WfOdocReceiveUnitMapper unitMapper = mock(WfOdocReceiveUnitMapper.class);
	private final IWfOdocReceiveGroupService receiveGroupService = mock(IWfOdocReceiveGroupService.class);
	private final WfOdocSecretRuleServiceImpl service =
		new WfOdocSecretRuleServiceImpl(ruleMapper, unitMapper, receiveGroupService);

	private WfOdocSecretRule rule(Long targetId, int from, int to) {
		WfOdocSecretRule r = new WfOdocSecretRule();
		r.setId(targetId);
		r.setTargetType(WfOdocSecretRule.TARGET_UNIT);
		r.setTargetId(targetId);
		r.setSecLevelFrom(from);
		r.setSecLevelTo(to);
		r.setEnabled(1);
		return r;
	}

	private WfOdocSecretRule groupRule(Long targetId, int from, int to) {
		WfOdocSecretRule r = rule(targetId, from, to);
		r.setTargetType(WfOdocSecretRule.TARGET_GROUP);
		return r;
	}

	private WfOdocReceiveGroup group(Long id, String unitIds) {
		WfOdocReceiveGroup g = new WfOdocReceiveGroup();
		g.setId(id);
		g.setGroupName("机密组");
		g.setUnitIds(unitIds);
		return g;
	}

	@Test
	@DisplayName("violationMessage：无规则 → 放行")
	void noRules() {
		when(ruleMapper.selectList(any())).thenReturn(Collections.emptyList());
		assertNull(service.violationMessage("绝密", "5"));
	}

	@Test
	@DisplayName("violationMessage：密级在允许范围 → 放行")
	void withinRange() {
		when(ruleMapper.selectList(any())).thenReturn(Collections.singletonList(rule(5L, 0, 3)));
		assertNull(service.violationMessage("机密", "5"));
	}

	@Test
	@DisplayName("violationMessage：越限 → 返回含单位名的拒绝原因")
	void outOfRange() {
		when(ruleMapper.selectList(any())).thenReturn(Collections.singletonList(rule(5L, 2, 3)));
		WfOdocReceiveUnit u = new WfOdocReceiveUnit();
		u.setId(5L);
		u.setUnitName("外部甲");
		when(unitMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(u));
		String msg = service.violationMessage("公开", "5");
		assertNotNull(msg);
		assertTrue(msg.contains("外部甲"));
		assertTrue(msg.contains("公开"));
	}

	@Test
	@DisplayName("violationMessage：密级不可判定 → 放行（不误伤）")
	void unknownSecret() {
		when(ruleMapper.selectList(any())).thenReturn(Collections.singletonList(rule(5L, 2, 3)));
		assertNull(service.violationMessage("未知密级", "5"));
	}

	@Test
	@DisplayName("violationMessage：无收文单位(或全是名称) → 放行")
	void noUnits() {
		when(ruleMapper.selectList(any())).thenReturn(Collections.singletonList(rule(5L, 2, 3)));
		assertNull(service.violationMessage("绝密", ""));
		assertNull(service.violationMessage("绝密", "外部甲,外部乙"));
	}

	@Test
	@DisplayName("violationMessage：单位无对应规则 → 放行")
	void unitWithoutRule() {
		when(ruleMapper.selectList(any())).thenReturn(Collections.singletonList(rule(5L, 2, 3)));
		assertNull(service.violationMessage("绝密", "7"));
	}

	@Test
	@DisplayName("save：目标类型非法 → 抛异常")
	void saveBadTarget() {
		WfOdocSecretRule r = new WfOdocSecretRule();
		r.setTargetType(9);
		r.setTargetId(1L);
		assertThrows(IllegalArgumentException.class, () -> service.save(r));
	}

	@Test
	@DisplayName("save：密级起>至 → 抛异常")
	void saveBadRange() {
		WfOdocSecretRule r = new WfOdocSecretRule();
		r.setTargetType(WfOdocSecretRule.TARGET_UNIT);
		r.setTargetId(1L);
		r.setSecLevelFrom(3);
		r.setSecLevelTo(1);
		assertThrows(IllegalArgumentException.class, () -> service.save(r));
	}

	@Test
	@DisplayName("save：合法 → insert，默认 enabled=1")
	void saveInsert() {
		WfOdocSecretRule r = new WfOdocSecretRule();
		r.setTargetType(WfOdocSecretRule.TARGET_UNIT);
		r.setTargetId(5L);
		r.setSecLevelFrom(1);
		r.setSecLevelTo(3);
		service.save(r);
		assertEquals(1, r.getEnabled());
		verify(ruleMapper, times(1)).insert(r);
	}

	@Test
	@DisplayName("remove：删除返回影响行数>0")
	void remove() {
		when(ruleMapper.deleteById(5L)).thenReturn(1);
		assertEquals(true, service.remove(5L));
	}

	@Test
	@DisplayName("越限但不查单位名之外多余调用（未越限不查名）")
	void noNameLookupWhenOk() {
		when(ruleMapper.selectList(any())).thenReturn(Collections.singletonList(rule(5L, 0, 3)));
		service.violationMessage("公开", "5");
		verify(unitMapper, never()).selectBatchIds(any());
	}

	@Test
	@DisplayName("save null 目标 → 抛异常")
	void saveNullTarget() {
		assertThrows(IllegalArgumentException.class, () -> service.save(new WfOdocSecretRule()));
	}

	@Test
	@DisplayName("remove false 当影响 0 行")
	void removeFalse() {
		when(ruleMapper.deleteById(5L)).thenReturn(0);
		assertFalse(service.remove(5L));
	}

	@Test
	@DisplayName("violationMessage：公共组越限(收文单位命中组成员) → 拒")
	void groupOutOfRange() {
		when(ruleMapper.selectList(any())).thenReturn(Collections.singletonList(groupRule(200L, 2, 3)));
		when(receiveGroupService.list()).thenReturn(Collections.singletonList(group(200L, "5,6")));
		String msg = service.violationMessage("公开", "5");
		assertNotNull(msg);
		assertTrue(msg.contains("机密组"));
	}

	@Test
	@DisplayName("violationMessage：公共组在范围内 → 放行")
	void groupWithinRange() {
		when(ruleMapper.selectList(any())).thenReturn(Collections.singletonList(groupRule(200L, 0, 3)));
		when(receiveGroupService.list()).thenReturn(Collections.singletonList(group(200L, "5,6")));
		assertNull(service.violationMessage("绝密", "5"));
	}

	@Test
	@DisplayName("violationMessage：收文单位不在公共组内 → 组规则不生效")
	void groupNoHit() {
		when(ruleMapper.selectList(any())).thenReturn(Collections.singletonList(groupRule(200L, 2, 3)));
		when(receiveGroupService.list()).thenReturn(Collections.singletonList(group(200L, "5,6")));
		assertNull(service.violationMessage("公开", "9"));
	}
}
