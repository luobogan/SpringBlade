package org.springblade.formmode.service.impl;

import org.junit.Test;
import org.springblade.formmode.entity.ModeDataShareRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 行级数据范围规则匹配的单测（聚焦纯函数 {@link ModeDataShareRuleServiceImpl#matchRule}）。
 *
 * <p>objtype 编码（2026-10-20 已与 moderightinfo/modefieldauthorize 统一）：
 * <b>1角色 2部门 3岗位 4人员</b>。</p>
 */
public class ModeDataShareRuleServiceImplTest {

    private static ModeDataShareRule rule(int objtype, long objid) {
        ModeDataShareRule r = new ModeDataShareRule();
        r.setObjtype(objtype);
        r.setObjid(objid);
        return r;
    }

    @Test
    public void personMatch() {
        // 人员 objtype=4
        assertTrue(ModeDataShareRuleServiceImpl.matchRule(rule(4, 100L), 100L, null, null, null));
        assertFalse(ModeDataShareRuleServiceImpl.matchRule(rule(4, 100L), 200L, null, null, null));
    }

    @Test
    public void deptMatch() {
        // 部门 objtype=2
        assertTrue(ModeDataShareRuleServiceImpl.matchRule(rule(2, 5L), 1L, 5L, null, null));
        assertFalse(ModeDataShareRuleServiceImpl.matchRule(rule(2, 5L), 1L, 6L, null, null));
    }

    @Test
    public void roleMatch() {
        // 角色 objtype=1
        List<Long> roles = Arrays.asList(7L, 8L);
        assertTrue(ModeDataShareRuleServiceImpl.matchRule(rule(1, 7L), 1L, null, roles, null));
        assertFalse(ModeDataShareRuleServiceImpl.matchRule(rule(1, 7L), 1L, null, Collections.singletonList(9L), null));
    }

    @Test
    public void jobMatch() {
        // 岗位 objtype=3
        List<Long> jobs = Arrays.asList(11L, 12L);
        assertTrue(ModeDataShareRuleServiceImpl.matchRule(rule(3, 11L), 1L, null, null, jobs));
        assertFalse(ModeDataShareRuleServiceImpl.matchRule(rule(3, 11L), 1L, null, null, Collections.singletonList(99L)));
    }

    @Test
    public void nullSafe() {
        assertFalse(ModeDataShareRuleServiceImpl.matchRule(null, 1L, null, null, null));
        assertFalse(ModeDataShareRuleServiceImpl.matchRule(rule(9, 1L), 1L, null, null, null));
        assertFalse(ModeDataShareRuleServiceImpl.matchRule(rule(4, 100L), null, null, null, null));
    }
}
