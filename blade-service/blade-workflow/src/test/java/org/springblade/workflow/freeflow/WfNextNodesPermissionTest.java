package org.springblade.workflow.freeflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.controller.WfFreeFlowController;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfFreeNodeMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.utils.WfAuthUtil;
import org.springblade.workflow.vo.NextNodesVO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * 下一步候选的权限口径（WF-N9「条件 ≠ 权限」）：候选只由服务端白名单下发，
 * 非自由实例不进入新链路，任务不存在直接拒绝。
 */
@ExtendWith(MockitoExtension.class)
class WfNextNodesPermissionTest {

    @Mock private WfFreeFlowManager manager;
    @Mock private WfFreeNodeMapper freeNodeMapper;
    @Mock private WfTaskMapper taskMapper;
    @Mock private WfInstanceMapper instanceMapper;
    @Mock private WfProcessDefinitionMapper defMapper;

    @InjectMocks private WfFreeFlowController controller;

    private WfTask task(String nodeKey) {
        WfTask t = new WfTask();
        t.setId(300L);
        t.setInstId(100L);
        t.setNodeKey(nodeKey);
        return t;
    }

    private void givenInstance(int isFree) {
        WfInstance inst = new WfInstance();
        inst.setId(100L);
        inst.setDefId(200L);
        when(instanceMapper.selectById(100L)).thenReturn(inst);
        WfProcessDefinition def = new WfProcessDefinition();
        def.setIsFree(isFree);
        when(defMapper.selectById(200L)).thenReturn(def);
    }

    @Test
    void missingTask_isRejected() {
        when(taskMapper.selectById(999L)).thenReturn(null);
        assertThatThrownBy(() -> controller.nextNodes(999L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("任务不存在");
    }

    @Test
    void nonFreeDefinition_returnsEmptyCandidatesAndZeroRegression() {
        when(taskMapper.selectById(300L)).thenReturn(task("UserTask_1"));
        givenInstance(0);
        when(manager.poolSize(200L)).thenReturn(5);
        try (MockedStatic<WfAuthUtil> auth = mockStatic(WfAuthUtil.class)) {
            NextNodesVO vo = controller.nextNodes(300L).getData();
            assertThat(vo.getFreeFlow()).isFalse();
            assertThat(vo.getNodes()).isEmpty();
            assertThat(vo.getPoolSize()).isEqualTo(5);
        }
    }

    @Test
    void freeDefinition_returnsOnlyPoolNodesExcludingCurrent() {
        when(taskMapper.selectById(300L)).thenReturn(task("Free_2"));
        givenInstance(1);
        when(manager.poolSize(200L)).thenReturn(3);
        try (MockedStatic<WfAuthUtil> auth = mockStatic(WfAuthUtil.class)) {
            NextNodesVO vo = controller.nextNodes(300L).getData();
            assertThat(vo.getFreeFlow()).isTrue();
            assertThat(vo.getNodes()).hasSize(2);
            assertThat(vo.getNodes()).extracting("nodeKey")
                .containsExactly("Free_1", "Free_3");
            assertThat(vo.getNodes()).allSatisfy(n -> {
                assertThat(n.getSelectable()).isTrue();
                assertThat(n.getAllowedUserIds()).isEmpty();
            });
        }
    }

    @Test
    void candidatesAreServerGeneratedNotClientSupplied() {
        // 接口签名只有 taskId，前端无法传入自定义节点 key；候选恒为 Free_1..Free_N。
        when(taskMapper.selectById(300L)).thenReturn(task("Free_1"));
        givenInstance(1);
        when(manager.poolSize(200L)).thenReturn(2);
        try (MockedStatic<WfAuthUtil> auth = mockStatic(WfAuthUtil.class)) {
            List<String> keys = controller.nextNodes(300L).getData().getNodes()
                .stream().map(n -> n.getNodeKey()).toList();
            assertThat(keys).containsExactly("Free_2");
        }
    }
}
