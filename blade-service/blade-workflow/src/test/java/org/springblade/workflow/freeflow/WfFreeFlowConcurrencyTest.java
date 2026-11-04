package org.springblade.workflow.freeflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.dto.FreeFlowSaveDTO;
import org.springblade.workflow.dto.FreeFlowStepDTO;
import org.springblade.workflow.entity.WfFreeNode;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.mapper.WfFreeLinkMapper;
import org.springblade.workflow.mapper.WfFreeNodeMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfNodeLinkMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 并发与生命周期：version 乐观锁、池容量上限、整包物理删除、退回状态重置。 */
@ExtendWith(MockitoExtension.class)
class WfFreeFlowConcurrencyTest {

    @Mock private WfFreeNodeMapper nodeMapper;
    @Mock private WfFreeLinkMapper linkMapper;
    @Mock private WfNodeLinkMapper nodeLinkMapper;
    @Mock private WfProcessDefinitionMapper defMapper;
    @Mock private WfInstanceMapper instanceMapper;

    @InjectMocks private WfFreeFlowManager manager;

    private WfInstance instance() {
        WfInstance inst = new WfInstance();
        inst.setId(100L);
        inst.setDefId(200L);
        inst.setCurrentNodeKey("UserTask_1");
        return inst;
    }

    private WfFreeNode existing(int order, int version, int status) {
        WfFreeNode n = new WfFreeNode();
        n.setInstId(100L);
        n.setNodeKey("Free_" + order);
        n.setFlowOrder(order);
        n.setNodeName("步骤" + order);
        n.setOperatorIds("11");
        n.setVersion(version);
        n.setNodeStatus(status);
        return n;
    }

    private FreeFlowStepDTO step(int order) {
        FreeFlowStepDTO s = new FreeFlowStepDTO();
        s.setFlowOrder(order);
        s.setNodeName("步骤" + order);
        s.setOperatorIds(List.of(11L));
        return s;
    }

    @Test
    void staleVersion_blocksSaveWithoutDeleting() {
        when(instanceMapper.selectById(100L)).thenReturn(instance());
        when(nodeMapper.selectList(any())).thenReturn(List.of(existing(1, 3, 1)));
        FreeFlowSaveDTO dto = new FreeFlowSaveDTO();
        dto.setInstId(100L);
        dto.setSteps(List.of(step(1)));
        dto.setVersion(1);

        assertThatThrownBy(() -> manager.saveSteps(dto, 9L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("已被他人修改");
        verify(nodeMapper, never()).physicalDeleteByInstId(any());
    }

    @Test
    void stepsBeyondPoolSize_areRejectedWithActionableMessage() {
        when(instanceMapper.selectById(100L)).thenReturn(instance());
        List<FreeFlowStepDTO> six = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            six.add(step(i));
        }
        FreeFlowSaveDTO dto = new FreeFlowSaveDTO();
        dto.setInstId(100L);
        dto.setSteps(six);

        assertThatThrownBy(() -> manager.saveSteps(dto, 9L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("重新发布");
    }

    @Test
    void cleanup_physicallyDeletesBothTables() {
        manager.cleanup(100L);
        verify(linkMapper).physicalDeleteByInstId(100L);
        verify(nodeMapper).physicalDeleteByInstId(100L);
    }

    @Test
    void resetFrom_reopensTargetAndLaterSteps() {
        WfFreeNode s1 = existing(1, 1, 1);
        WfFreeNode s2 = existing(2, 1, 1);
        WfFreeNode s3 = existing(3, 1, 1);
        when(nodeMapper.selectList(any())).thenReturn(List.of(s1, s2, s3));

        manager.resetFrom(100L, "Free_2");

        assertThat(s1.getNodeStatus()).isEqualTo(1);
        assertThat(s2.getNodeStatus()).isZero();
        assertThat(s3.getNodeStatus()).isZero();
        verify(nodeMapper).updateById(s2);
        verify(nodeMapper).updateById(s3);
        verify(nodeMapper, never()).updateById(s1);
    }

    @Test
    void parseIds_trimsBlankAndKeepsSnowflakeAsLong() {
        assertThat(WfFreeFlowManager.parseIds(" 11, 22 ,,33 ")).containsExactly(11L, 22L, 33L);
        assertThat(WfFreeFlowManager.parseIds(null)).isEmpty();
        assertThat(WfFreeFlowManager.parseIds("   ")).isEmpty();
    }
}
