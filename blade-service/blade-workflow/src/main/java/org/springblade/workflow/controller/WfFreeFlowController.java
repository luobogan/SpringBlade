package org.springblade.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.dto.FreeFlowSaveDTO;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.freeflow.WfFreeFlowManager;
import org.springblade.workflow.freeflow.WfFreeNodeContext;
import org.springblade.workflow.mapper.WfFreeNodeMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.utils.WfAuthUtil;
import org.springblade.workflow.vo.NextNodeCandidateVO;
import org.springblade.workflow.vo.NextNodesVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自由流程：步骤编排读存、下一步候选、版本与池容量。
 *
 * <p>权限口径（WF-N9「条件 ≠ 权限」）：所有入口先做记录级鉴权
 * {@link WfAuthUtil#requireOperateTask}，候选节点由服务端下发（白名单），
 * 且 {@code doApprove} 提交时会对 nextNodeKey / 办理人做二次校验，
 * 不以出口条件求值作为门禁，也不信任前端传入的节点范围。</p>
 */
@Slf4j
@RestController
@RequestMapping("/free-flow")
@RequiredArgsConstructor
@PreAuth(WorkflowConstant.HAS_AUTH)
public class WfFreeFlowController {

    private final WfFreeFlowManager manager;
    private final WfFreeNodeMapper freeNodeMapper;
    /** 批次 1：按 BIZ_TASK_ID_ 反查 ACT（取代 taskMapper.selectById） */
    private final org.springblade.workflow.service.helper.WfTaskBizResolver bizResolver;
    private final WfInstanceMapper instanceMapper;
    private final WfProcessDefinitionMapper defMapper;

    /** 步骤编排回显（含 version 乐观锁与池容量） */
    @GetMapping("/steps")
    @Operation(summary = "查询实例的自由流程步骤")
    public R<Map<String, Object>> steps(@RequestParam Long instId) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            throw new ServiceException("流程实例不存在：" + instId);
        }
        WfAuthUtil.requireStarterOrAdmin(inst, "查看流转设定");
        Map<String, Object> data = new LinkedHashMap<>(4);
        data.put("steps", manager.view(instId));
        data.put("version", WfFreeNodeContext.load(freeNodeMapper, instId).version());
        data.put("poolSize", manager.poolSize(inst.getDefId()));
        return R.data(data);
    }

    /** 整包保存（单事务 + version 乐观锁 + saveToken 幂等） */
    @PostMapping("/save")
    @Operation(summary = "整包保存自由流程步骤")
    public R<Integer> save(@RequestBody FreeFlowSaveDTO dto) {
        if (dto.getInstId() == null) {
            throw new ServiceException("流程实例ID不能为空");
        }
        WfInstance inst = instanceMapper.selectById(dto.getInstId());
        if (inst == null) {
            throw new ServiceException("流程实例不存在：" + dto.getInstId());
        }
        WfAuthUtil.requireStarterOrAdmin(inst, "设置流转");
        return R.data(manager.saveSteps(dto, null), "保存成功");
    }

    /**
     * 下一步候选节点。非自由实例返回 freeFlow=false，前端回落既有指定流转 / 默认流向。
     *
     * <p>候选只来自预置池（服务端生成，不接受前端指定），并排除当前节点；
     * 记录级鉴权保证只有本任务办理人能查询。提交时 doApprove 会再次校验目标合法性。</p>
     */
    @GetMapping("/next-nodes")
    @Operation(summary = "下一步候选节点（含权限过滤）")
    public R<NextNodesVO> nextNodes(@RequestParam Long taskId) {
        // 批次 1：wf_task 已零写入，改按 BIZ_TASK_ID_ 反查 ACT
        WfTask task = bizResolver.resolve(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在：" + taskId);
        }
        WfAuthUtil.requireOperateTask(task, "查询下一步");
        WfInstance inst = instanceMapper.selectById(task.getInstId());

        NextNodesVO vo = new NextNodesVO();
        vo.setCurrentNodeKey(task.getNodeKey());
        vo.setAppointFlowMode(0);
        vo.setNodes(Collections.emptyList());
        if (inst == null || inst.getDefId() == null) {
            vo.setFreeFlow(false);
            vo.setPoolSize(WfFreeFlowManager.DEFAULT_POOL_SIZE);
            return R.data(vo);
        }
        WfProcessDefinition def = defMapper.selectById(inst.getDefId());
        boolean free = def != null && Integer.valueOf(1).equals(def.getIsFree());
        vo.setFreeFlow(free);
        int pool = manager.poolSize(inst.getDefId());
        vo.setPoolSize(pool);
        if (!free) {
            return R.data(vo);
        }
        List<NextNodeCandidateVO> nodes = new ArrayList<>(pool);
        for (int i = 1; i <= pool; i++) {
            String key = WfFreeFlowManager.POOL_NODE_PREFIX + i;
            if (key.equals(task.getNodeKey())) {
                continue;
            }
            NextNodeCandidateVO c = new NextNodeCandidateVO();
            c.setNodeKey(key);
            c.setNodeName("通用审批" + i);
            c.setSelectable(true);
            c.setAllowedUserIds(Collections.emptyList());
            c.setAssigneeEditable(true);
            c.setSignTypeDefault(0);
            nodes.add(c);
        }
        vo.setNodes(nodes);
        return R.data(vo);
    }
}
