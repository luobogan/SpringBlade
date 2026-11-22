package org.springblade.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springblade.workflow.action.NodeActionExecutor;
import org.springblade.workflow.action.WfActionExecutor;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfNodeLinkMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.service.IWfSubflowService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 子流程防递归单测（2026-11-18，S3 子流程「业务层为主、补验证」）。
 *
 * <p>覆盖 NodeActionExecutor.triggerSubflow 的两道防环：① 自引用（flowKey == 本流程 procKey）；
 * ② 嵌套层级达 MAX_SUBFLOW_DEPTH(5)。两道都应跳过发起（instanceService.start 不被调用）。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WfSubflowRecursionTest {

    private static final Long INST_ID = 1001L;
    private static final Long DEF_ID = 3001L;

    @Mock private WfProcessDefinitionMapper defMapper;
    @Mock private WfInstanceMapper instanceMapper;
    @Mock private WfProcessNodeMapper nodeMapper;
    @Mock private WfNodeLinkMapper linkMapper;
    @Mock private WfActionExecutor actionExecutor;
    @Mock private IWfInstanceService instanceService;
    @Mock private IWfSubflowService subflowService;

    @Test
    void 自引用_子流程Key等于本流程_跳过发起() {
        NodeActionExecutor executor = executor();
        WfInstance inst = inst(null);
        WfProcessDefinition def = new WfProcessDefinition();
        def.setProcKey("sameFlow");
        when(defMapper.selectById(DEF_ID)).thenReturn(def);

        executor.triggerSubflow(inst, node("sameFlow"), NodeActionExecutor.TRIGGER_AFTER_SUBMIT, 676L);

        verify(instanceService, never()).start(any());
    }

    @Test
    void 深递归_嵌套达上限_跳过发起() {
        NodeActionExecutor executor = executor();
        WfInstance inst = inst(1000L); // parentId 非空，下面 mock 5 层祖先
        WfProcessDefinition parentDef = new WfProcessDefinition();
        parentDef.setProcKey("mainFlow");
        when(defMapper.selectById(DEF_ID)).thenReturn(parentDef);
        // parentDepth 沿 parentId 上溯，5 层祖先 → depth=5 达上限
        for (long i = 1000; i >= 996; i--) {
            WfInstance ancestor = new WfInstance();
            ancestor.setId(i);
            ancestor.setParentId(i - 1);
            when(instanceMapper.selectById(i)).thenReturn(ancestor);
        }

        executor.triggerSubflow(inst, node("childFlow"), NodeActionExecutor.TRIGGER_AFTER_SUBMIT, 676L);

        verify(instanceService, never()).start(any());
    }

    @Test
    void 正常_非自引用且层级浅_发起子流程() {
        NodeActionExecutor executor = executor();
        WfInstance inst = inst(null);
        WfProcessDefinition parentDef = new WfProcessDefinition();
        parentDef.setProcKey("mainFlow");
        WfProcessDefinition subDef = new WfProcessDefinition();
        subDef.setId(4001L);
        subDef.setProcKey("childFlow");
        subDef.setStatus(1);
        subDef.setVersion(1);
        when(defMapper.selectById(DEF_ID)).thenReturn(parentDef);
        when(defMapper.selectOne(any())).thenReturn(subDef);
        when(instanceService.start(any())).thenReturn(5001L);

        executor.triggerSubflow(inst, node("childFlow"), NodeActionExecutor.TRIGGER_AFTER_SUBMIT, 676L);

        verify(instanceService).start(any());
        verify(subflowService).record(eq(INST_ID), eq(5001L), eq(4001L), any());
    }

    private NodeActionExecutor executor() {
        return new NodeActionExecutor(defMapper, instanceMapper, nodeMapper, linkMapper,
            actionExecutor, instanceService, subflowService);
    }

    private static WfInstance inst(Long parentId) {
        WfInstance i = new WfInstance();
        i.setId(INST_ID);
        i.setDefId(DEF_ID);
        i.setParentId(parentId);
        i.setFormId(1L);
        i.setDataId(2L);
        return i;
    }

    private static WfProcessNode node(String flowKey) {
        WfProcessNode n = new WfProcessNode();
        n.setNodeKey("Task_Subflow");
        n.setExtJson("{\"settings\":{\"subflow\":{\"flowKey\":\"" + flowKey
            + "\",\"trigger\":\"afterSubmit\"}}}");
        return n;
    }
}
