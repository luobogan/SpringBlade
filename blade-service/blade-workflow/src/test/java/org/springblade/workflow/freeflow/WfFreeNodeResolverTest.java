package org.springblade.workflow.freeflow;

import org.junit.jupiter.api.Test;
import org.springblade.workflow.entity.WfFreeNode;
import org.springblade.workflow.entity.WfProcessNode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 影子节点装饰：静态 key 不变、配置按键合并、角色标记判定。 */
class WfFreeNodeResolverTest {

    private final WfFreeNodeResolver resolver = new WfFreeNodeResolver();

    private WfProcessNode base() {
        WfProcessNode n = new WfProcessNode();
        n.setNodeKey("Free_2");
        n.setNodeName("通用审批2");
        n.setNodeType(1);
        n.setSignOrder(0);
        n.setExtJson("{\"settings\":{\"operateMenu\":{\"items\":[\"submit\",\"save\"]},"
            + "\"signOpinion\":{\"required\":true}},\"printSet\":{\"id\":7}}");
        return n;
    }

    private WfFreeNode step() {
        WfFreeNode s = new WfFreeNode();
        s.setNodeKey("Free_2");
        s.setFlowOrder(2);
        s.setNodeName("部门会签");
        s.setSignType(1);
        s.setIsFinalize(1);
        s.setMustTaohong(1);
        s.setExtJson("{\"settings\":{\"timeout\":{\"hours\":24}},\"customFlag\":true}");
        return s;
    }

    @Test
    void emptyContext_returnsSameInstance() {
        WfProcessNode base = base();
        assertThat(resolver.decorate(WfFreeNodeContext.empty(), base, "Free_2")).isSameAs(base);
    }

    @Test
    void decorate_overridesNameAndSignButKeepsStaticNodeKey() {
        WfFreeNodeContext ctx = WfFreeNodeContext.of(1L, List.of(step()));
        WfProcessNode base = base();
        WfProcessNode decorated = resolver.decorate(ctx, base, "Free_2");

        assertThat(decorated).isNotSameAs(base);
        assertThat(decorated.getNodeKey()).isEqualTo("Free_2");
        assertThat(decorated.getNodeName()).isEqualTo("部门会签");
        assertThat(decorated.getSignOrder()).isEqualTo(1);
        // 原对象不被污染（可能被缓存复用）
        assertThat(base.getNodeName()).isEqualTo("通用审批2");
    }

    @Test
    void decorate_mergesExtJsonWithoutDroppingStaticMenus() {
        WfFreeNodeContext ctx = WfFreeNodeContext.of(1L, List.of(step()));
        String merged = resolver.decorate(ctx, base(), "Free_2").getExtJson();

        assertThat(merged).contains("operateMenu").contains("signOpinion")
            .contains("printSet").contains("timeout").contains("customFlag");
    }

    @Test
    void noMatchingStep_returnsBase() {
        WfProcessNode base = base();
        WfFreeNodeContext ctx = WfFreeNodeContext.of(1L, List.of(step()));
        assertThat(resolver.decorate(ctx, base, "Free_9")).isSameAs(base);
    }

    @Test
    void roleMarkers_replaceStaticKeyComparison() {
        WfFreeNodeContext ctx = WfFreeNodeContext.of(1L, List.of(step()));
        assertThat(resolver.isFinalize(ctx, "Free_2")).isTrue();
        assertThat(resolver.mustTaohong(ctx, "Free_2")).isTrue();
        assertThat(resolver.isFinalize(ctx, "Free_1")).isFalse();
        assertThat(resolver.isFinalize(WfFreeNodeContext.empty(), "Free_2")).isFalse();
    }
}
