package org.springblade.workflow.compat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.service.helper.WfEngineMiGate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会签灰度门单测：验证「白名单空=等同现状 / 名单内走引擎 / 名单外走自研」与全局开关优先级。
 */
class WfEngineMiGateTest {

    private WfEngineMiGate gate(boolean globalEnabled, WfFlowtableCompatProperties compat) {
        WfEngineMiGate g = new WfEngineMiGate(compat);
        ReflectionTestUtils.setField(g, "multiInstanceEnabled", globalEnabled);
        return g;
    }

    private WfInstance inst(Long defId) {
        WfInstance i = new WfInstance();
        i.setDefId(defId);
        return i;
    }

    private WfProcessNode node(Integer nodeType) {
        WfProcessNode n = new WfProcessNode();
        n.setNodeType(nodeType);
        return n;
    }

    @Test
    void globalOff_neverEngine_无论白名单() {
        WfFlowtableCompatProperties compat = new WfFlowtableCompatProperties();
        compat.setEngineMiGrayDefIds(List.of(100L));
        WfEngineMiGate g = gate(false, compat);
        assertThat(g.useEngineMi(inst(100L), node(1), "Free_2")).isFalse();
        assertThat(g.useEngineMiForInstance(inst(100L))).isFalse();
    }

    @Test
    void globalOn_grayEmpty_等同现状_全部走引擎() {
        WfFlowtableCompatProperties compat = new WfFlowtableCompatProperties(); // 白名单默认空
        WfEngineMiGate g = gate(true, compat);
        assertThat(g.useEngineMi(inst(999L), node(1), "n")).isTrue();
        assertThat(g.useEngineMi(inst(999L), node(2), "n")).isTrue();
        assertThat(g.useEngineMiForInstance(inst(999L))).isTrue();
    }

    @Test
    void globalOn_grayNonEmpty_仅名单内走引擎() {
        WfFlowtableCompatProperties compat = new WfFlowtableCompatProperties();
        compat.setEngineMiGrayDefIds(List.of(100L, 200L));
        WfEngineMiGate g = gate(true, compat);
        assertThat(g.useEngineMi(inst(100L), node(1), "n")).isTrue();
        assertThat(g.useEngineMi(inst(200L), node(1), "n")).isTrue();
        assertThat(g.useEngineMi(inst(300L), node(1), "n")).isFalse();  // 名单外→自研
        assertThat(g.useEngineMiForInstance(inst(300L))).isFalse();
    }

    @Test
    void nodeTypeConstraint_仅审批提交走引擎() {
        WfFlowtableCompatProperties compat = new WfFlowtableCompatProperties();
        WfEngineMiGate g = gate(true, compat);
        assertThat(g.useEngineMi(inst(1L), node(0), "n")).isFalse(); // 创建
        assertThat(g.useEngineMi(inst(1L), node(3), "n")).isFalse(); // 归档
        assertThat(g.useEngineMi(inst(1L), node(7), "n")).isFalse(); // 网关
        assertThat(g.useEngineMi(inst(1L), node(1), "n")).isTrue();  // 审批
        assertThat(g.useEngineMi(inst(1L), node(2), "n")).isTrue();  // 提交
    }

    @Test
    void nullSafety_空实例与空节点不抛异常() {
        WfFlowtableCompatProperties compat = new WfFlowtableCompatProperties();
        WfEngineMiGate g = gate(true, compat);
        assertThat(g.useEngineMi(null, node(1), "n")).isFalse();   // inst 空守卫（与旧轨等价）
        assertThat(g.useEngineMi(inst(1L), null, "n")).isFalse();  // node 空守卫
        // 白名单空时 globalEnabled 不看 inst（与旧轨 injectMiCollectionVars 只判全局开关等价）→ true
        assertThat(g.useEngineMiForInstance(null)).isTrue();
    }
}
