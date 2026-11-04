package org.springblade.workflow.freeflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.dto.ApproveDTO;
import org.springblade.workflow.entity.WfFreeNode;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfFreeNodeMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.IWfInstanceService;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 自由流程推进器：开关、末步归档、中间步迁移、缺步骤与退回状态回退。 */
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class WfFreeFlowDriverTest {

    @Mock private WfFreeFlowManager manager;
    @Mock private WfFreeNodeResolver resolver;
    @Mock private WfFreeNodeMapper freeNodeMapper;
    @Mock private WfProcessDefinitionMapper defMapper;
    @Mock private IProcessService processService;
    @Mock private IWfInstanceService instanceService;
    @Mock private WfTaskMapper taskMapper;

    @InjectMocks private WfFreeFlowDriver driver;

    @BeforeEach
    void enableSwitch() throws Exception {
        Field f = WfFreeFlowDriver.class.getDeclaredField("enabled");
        f.setAccessible(true);
        f.set(driver, true);
    }

    private WfInstance instance(int isFree, int isTest) {
        WfInstance inst = new WfInstance();
        inst.setId(100L);
        inst.setDefId(200L);
        inst.setEngineInstId("ENG_1");
        inst.setIsTest(isTest);
        WfProcessDefinition def = new WfProcessDefinition();
        def.setIsFree(isFree);
        when(defMapper.selectById(200L)).thenReturn(def);
        return inst;
    }

    private WfTask task(String nodeKey) {
        WfTask t = new WfTask();
        t.setId(300L);
        t.setInstId(100L);
        t.setNodeKey(nodeKey);
        t.setEngineTaskId("TASK_1");
        return t;
    }

    private WfFreeNode step(int order, String nodeKey, String operators) {
        WfFreeNode n = new WfFreeNode();
        n.setFlowOrder(order);
        n.setNodeKey(nodeKey);
        n.setNodeName("步骤" + order);
        n.setOperatorIds(operators);
        n.setSignType(0);
        n.setIsFinalize(0);
        n.setMustTaohong(0);
        n.setNodeStatus(0);
        return n;
    }

    @Test
    void switchOff_disablesFreeFlow() {
        WfFreeFlowDriver off = new WfFreeFlowDriver(manager, resolver, freeNodeMapper,
            defMapper, processService, instanceService, taskMapper);
        WfInstance inst = new WfInstance();
        inst.setDefId(200L);
        assertThat(off.isEnabled(inst)).isFalse();
    }

    @Test
    void testInstance_neverEntersFreeFlow() {
        WfInstance inst = new WfInstance();
        inst.setDefId(200L);
        inst.setIsTest(1);
        assertThat(driver.isEnabled(inst)).isFalse();
    }

    @Test
    void normalDefinition_disablesFreeFlow() {
        WfInstance inst = instance(0, 0);
        assertThat(driver.isEnabled(inst)).isFalse();
        assertThat(driver.context(inst).isEmpty()).isTrue();
    }

    @Test
    void middleStep_movesActivityAndAdvances() {
        WfInstance inst = instance(1, 0);
        WfTask t = task("Free_1");
        WfFreeNodeContext ctx = WfFreeNodeContext.of(100L,
            List.of(step(1, "Free_1", "11"), step(2, "Free_2", "12")));
        Map<String, Object> vars = new HashMap<>();

        FreeAdvanceResult r = driver.submit(inst, t, new ApproveDTO(), 9L, vars, ctx);

        assertThat(r.isAdvanced()).isTrue();
        assertThat(r.getNextNodeKey()).isEqualTo("Free_2");
        verify(processService).moveActivity(eq("ENG_1"), eq("Free_1"), eq("Free_2"), any());
        verify(instanceService).advance(100L, 9L, "Free_2", 12L);
        verify(processService, never()).completeTask(any(), any());
        assertThat(vars).containsKey("wfMiAssignees_Free_2");
    }

    @Test
    void lastStep_completesTaskAndArchives() {
        WfInstance inst = instance(1, 0);
        WfTask t = task("Free_2");
        WfFreeNodeContext ctx = WfFreeNodeContext.of(100L,
            List.of(step(1, "Free_1", "11"), step(2, "Free_2", "12")));

        FreeAdvanceResult r = driver.submit(inst, t, new ApproveDTO(), 9L, new HashMap<>(), ctx);

        assertThat(r.isFinished()).isTrue();
        verify(processService).completeTask(eq("TASK_1"), any());
        verify(instanceService).advance(100L, 9L);
        verify(processService, never()).moveActivity(any(), any(), any(), any());
    }

    @Test
    void emptySteps_throwsInsteadOfSilentSkip() {
        WfInstance inst = instance(1, 0);
        assertThatThrownBy(() -> driver.submit(inst, task("Free_1"), new ApproveDTO(),
            9L, new HashMap<>(), WfFreeNodeContext.empty()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("流转设定");
    }

    @Test
    void nextStepWithoutAssignee_blocks() {
        WfInstance inst = instance(1, 0);
        WfFreeNodeContext ctx = WfFreeNodeContext.of(100L,
            List.of(step(1, "Free_1", "11"), step(2, "Free_2", "")));
        assertThatThrownBy(() -> driver.submit(inst, task("Free_1"), new ApproveDTO(),
            9L, new HashMap<>(), ctx))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("未指定办理人");
    }

    @Test
    void onReject_resetsStepStatus() {
        driver.onReject(100L, "Free_1");
        verify(manager).resetFrom(100L, "Free_1");
    }
}
