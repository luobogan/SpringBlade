package org.springblade.workflow.action;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.dto.StartProcessDTO;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 节点「前/后附加操作」与「子流程触发」的执行入口。
 *
 * <p>对应节点信息的 {@code settings.preOperate / postOperate / subflow}。泛微 E9 侧这些是
 * 真正的动作配置（接口调用 / DML / 字段赋值 / 触发子流程）。</p>
 *
 * <p><b>异常策略</b>（节点信息「流程异常处理」）：
 * {@code mode=stop} 时抛 {@link ServiceException} 中断流转；{@code continue}（默认）时仅告警跳过，
 * 保证附加操作/子流程失败不阻塞主干。</p>
 */
@Slf4j
@Component
public class NodeActionExecutor {

    /** 节点前附加操作 */
    public static final String PHASE_PRE = "pre";
    /** 节点后附加操作 */
    public static final String PHASE_POST = "post";
    /** 子流程触发时机：提交后 */
    public static final String TRIGGER_AFTER_SUBMIT = "afterSubmit";
    /** 子流程触发时机：归档后 */
    public static final String TRIGGER_AFTER_ARCHIVE = "afterArchive";

    /** 子流程嵌套层级上限（防自引用 / 无限递归） */
    private static final int MAX_SUBFLOW_DEPTH = 5;

    private final WfProcessDefinitionMapper defMapper;
    private final WfInstanceMapper instanceMapper;
    /** 附加操作实际执行器（http / sql / field 前缀分派） */
    private final WfActionExecutor actionExecutor;
    /**
     * 发起子流程需要实例服务；与 WfInstanceServiceImpl 存在构造期循环依赖，
     * 故用 {@link Lazy} 延迟到实际调用时再取代理。
     */
    private final IWfInstanceService instanceService;

    public NodeActionExecutor(WfProcessDefinitionMapper defMapper,
                              WfInstanceMapper instanceMapper,
                              WfActionExecutor actionExecutor,
                              @Lazy IWfInstanceService instanceService) {
        this.defMapper = defMapper;
        this.instanceMapper = instanceMapper;
        this.actionExecutor = actionExecutor;
        this.instanceService = instanceService;
    }

    /** 执行节点的前/后附加操作（非退回场景，见 {@link #execute(WfInstance, WfProcessNode, String, Long, boolean)}） */
    public void execute(WfInstance inst, WfProcessNode node, String phase, Long operator) {
        execute(inst, node, phase, operator, false);
    }

    /**
     * 执行节点的前/后附加操作（未配置则直接返回）。
     *
     * @param phase    {@link #PHASE_PRE} / {@link #PHASE_POST}
     * @param onReject 是否「退回」场景：true 时只执行勾选了「退回时触发」的条目
     *                 （读取前端派生的 settings 下 preOperate/postOperate 的 scriptOnReject）；
     *                 false 时执行全部启用条目（settings 下同级的 script）。
     */
    public void execute(WfInstance inst, WfProcessNode node, String phase, Long operator, boolean onReject) {
        if (node == null) {
            return;
        }
        boolean pre = PHASE_PRE.equals(phase);
        String script;
        if (onReject) {
            script = pre
                ? WfNodeSettingsUtil.preOperateRejectScript(node)
                : WfNodeSettingsUtil.postOperateRejectScript(node);
        } else {
            script = pre
                ? WfNodeSettingsUtil.preOperateScript(node)
                : WfNodeSettingsUtil.postOperateScript(node);
        }
        if (script == null || script.isBlank()) {
            return;
        }
        try {
            doExecute(inst, node, phase, script, operator);
        } catch (Exception e) {
            if ("stop".equals(WfNodeSettingsUtil.exceptionMode(node))) {
                throw new ServiceException("节点附加操作执行失败（流程异常处理=中断）：" + e.getMessage());
            }
            log.warn("[blade-workflow] 节点附加操作执行失败，按「继续」跳过. instId={}, nodeKey={}, phase={}",
                inst == null ? null : inst.getId(), node.getNodeKey(), phase, e);
        }
    }

    /**
     * 实际动作执行体：委派给 {@link WfActionExecutor} 按 {@code script} 前缀分派
     * （{@code http(s)://} / {@code sql:} / {@code field:}）。
     *
     * <p>支持**多行脚本**（对齐 E9：一个节点可挂多条附加操作）：每行一条命令，按顺序依次执行；
     * 空行与以 {@code #} / {@code //} 开头的注释行跳过。单行脚本（旧数据）行为不变。</p>
     */
    protected void doExecute(WfInstance inst, WfProcessNode node, String phase, String script, Long operator) {
        for (String line : script.split("\\R")) {
            String cmd = line.trim();
            if (cmd.isEmpty() || cmd.startsWith("#") || cmd.startsWith("//")) {
                continue;
            }
            actionExecutor.execute(inst, node, phase, cmd, operator);
        }
    }

    /**
     * 触发子流程（{@code settings.subflow}）。
     *
     * <p>仅当节点配置的子流程触发时机与入参 {@code trigger} 一致时执行：
     * 按 {@code flowKey} 定位同租户下已发布的流程定义，发起子实例并回填 {@code parentId}。</p>
     *
     * <p><b>防环</b>：① 子流程 Key 不能等于本流程 procKey（防自引用）；
     * ② 沿 {@code parentId} 的嵌套层级不超过 {@link #MAX_SUBFLOW_DEPTH}（防无限递归）。</p>
     */
    public void triggerSubflow(WfInstance inst, WfProcessNode node, String trigger, Long operator) {
        if (inst == null || node == null) {
            return;
        }
        String flowKey = WfNodeSettingsUtil.subflowKey(node);
        if (flowKey == null || flowKey.isBlank()) {
            return;
        }
        if (!WfNodeSettingsUtil.subflowTrigger(node).equals(trigger)) {
            return;
        }
        try {
            doTriggerSubflow(inst, node, flowKey, operator);
        } catch (Exception e) {
            if ("stop".equals(WfNodeSettingsUtil.exceptionMode(node))) {
                throw new ServiceException("子流程触发失败（流程异常处理=中断）：" + e.getMessage());
            }
            log.warn("[blade-workflow] 子流程触发失败，按「继续」跳过. nodeKey={}", node.getNodeKey(), e);
        }
    }

    private void doTriggerSubflow(WfInstance inst, WfProcessNode node, String flowKey, Long operator) {
        // ① 防自引用：子流程 Key 不能是本流程自身
        WfProcessDefinition parentDef = (inst.getDefId() == null) ? null : defMapper.selectById(inst.getDefId());
        if (parentDef != null && flowKey.equals(parentDef.getProcKey())) {
            log.warn("[blade-workflow] 子流程 Key 与本流程相同，跳过以防自引用. instId={}, procKey={}",
                inst.getId(), flowKey);
            return;
        }
        // ② 防深递归：沿 parentId 计数
        int depth = parentDepth(inst);
        if (depth >= MAX_SUBFLOW_DEPTH) {
            log.warn("[blade-workflow] 子流程嵌套层级达上限({})，跳过. instId={}, flowKey={}",
                MAX_SUBFLOW_DEPTH, inst.getId(), flowKey);
            return;
        }
        // ③ 定位同租户下最新已发布的子流程定义
        WfProcessDefinition subDef = defMapper.selectOne(Wrappers.<WfProcessDefinition>lambdaQuery()
            .eq(WfProcessDefinition::getProcKey, flowKey)
            .eq(WfProcessDefinition::getStatus, 1)
            .orderByDesc(WfProcessDefinition::getVersion)
            .last("LIMIT 1"));
        if (subDef == null) {
            log.warn("[blade-workflow] 未找到已发布的子流程定义，跳过. procKey={}", flowKey);
            return;
        }
        StartProcessDTO dto = new StartProcessDTO();
        dto.setDefId(subDef.getId());
        dto.setParentId(inst.getId());
        dto.setFormId(inst.getFormId());
        dto.setDataId(inst.getDataId());
        dto.setStarter(operator);
        dto.setUrgency(inst.getUrgency());
        dto.setTitle((inst.getTitle() == null ? "" : inst.getTitle()) + "-子流程");
        Long subInstId = instanceService.start(dto);
        log.info("[blade-workflow] 子流程已发起. parentInstId={}, subInstId={}, subProcKey={}, nodeKey={}, depth={}",
            inst.getId(), subInstId, flowKey, node.getNodeKey(), depth);
    }

    /** 沿 parentId 上溯计算当前实例的嵌套深度（0 = 顶层） */
    private int parentDepth(WfInstance inst) {
        int depth = 0;
        Long pid = inst.getParentId();
        while (pid != null && pid > 0 && depth <= MAX_SUBFLOW_DEPTH) {
            depth++;
            WfInstance parent = instanceMapper.selectById(pid);
            pid = (parent == null) ? null : parent.getParentId();
        }
        return depth;
    }
}
