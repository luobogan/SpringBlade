package org.springblade.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.listener.WfEngineEventListener;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.service.IWfTaskService;
import org.springblade.workflow.service.helper.WfStateProjector;
import org.springblade.workflow.vo.InstanceVO;
import org.springblade.workflow.vo.MonitorBatchResult;
import org.springblade.workflow.vo.MonitorInstanceQuery;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.workflow.utils.WfAuthUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程运维控制器
 *
 * <p>对应文档 §6.5：待办/已办计数（当前直查 DB，后续可接入 Redis 缓存，
 * 任务状态变更时失效缓存）。</p>
 *
 * <p><b>鉴权</b>：{@code /count} 是<b>顶栏待办角标</b>的数据源，全体员工都要用，
 * 因此只要求「已登录」（{@code HAS_AUTH}）；{@code assignee} 入参对非管理员
 * 一律收敛为当前登录人，避免查到别人的待办数。
 * {@code /health/live}、{@code /health/ready} 为基础设施探针，无用户上下文，保持放行。</p>
 */
@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
@Tag(name = "流程运维", description = "待办计数与健康检查")
public class WfMonitorController {

    private final IWfTaskService taskService;
    /** 方案C 影子投影器（始终存在）；其差异计数用于阶段 1 验收 */
    private final WfStateProjector projector;
    /** 台账事件监听：受 blade.workflow.ledger-listener.enabled 控制，开关关闭时 bean 不存在 */
    private final ObjectProvider<WfEngineEventListener> ledgerListenerProvider;
    /** 流程实例语义服务：监控列表与监管动作 */
    private final IWfInstanceService instanceService;

    @GetMapping("/count")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "待办/已办计数")
    public R<Map<String, Long>> count(
        @Parameter(description = "办理人，默认当前登录人；非流程管理员只能查自己") @RequestParam(value = "assignee", required = false) Long assignee) {
        return R.data(taskService.count(WfAuthUtil.resolveSelfIfNotAdmin(assignee)));
    }

    @GetMapping("/health/live")
    @Operation(summary = "存活探针")
    public R<String> live() {
        return R.data("UP", "存活");
    }

    @GetMapping("/health/ready")
    @Operation(summary = "就绪探针")
    public R<String> ready() {
        return R.data("UP", "就绪");
    }

    /**
     * 方案C 阶段1「影子模式」验收出口（只读）。
     *
     * <p>项目无日志文件、控制台日志不便捞取，故把 {@link WfStateProjector} 的内存差异计数
     * 暴露为只读快照：跑一批真实流程后请求一次即可判断是否可进入阶段 2。</p>
     *
     * <p><b>判读</b>：{@code listenerRegistered=true} 且 {@code totalDiff=0} → 映射正确，可进阶段2；
     * {@code listenerRegistered=false} → 开关未开，计数恒为 0 但<b>不代表</b>映射正确。</p>
     *
     * <p><b>鉴权取 HAS_AUTH（登录即可）而非角色门</b>：本端点只暴露只读的差异计数，无业务数据；
     * 而排查漂移的人未必是流程管理员（实测普通账号被角色门挡下，拿不到验收数据）。
     * 与 {@code /monitor/count} 同口径，且网关层仍要求有效令牌。</p>
     */
    @GetMapping("/ledger-shadow")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "方案C 影子模式差异计数（阶段1 验收用）",
        description = "返回按事件类型分桶的「引擎事件→期望 wf_* 状态」差异计数，以及台账监听是否已注册。"
            + "全部为 0 且监听已注册，才说明映射正确、可进入阶段 2 开启真实反写。计数为内存值，重启清零。")
    public R<Map<String, Object>> ledgerShadow() {
        Map<String, Long> diffs = projector.diffSnapshot();
        long total = diffs.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("listenerRegistered", ledgerListenerProvider.getIfAvailable() != null);
        data.put("totalDiff", total);
        data.put("diffs", diffs);
        return R.data(data);
    }

    /**
     * 流程监控「异常」聚合（异常 Tab 数据源）。
     *
     * <p>实例维度计数由 {@code monitorAnomalySummary()} 给出；引擎对账差异（方案C 影子模式，
     * 内存计数、重启清零）在此与 {@code /monitor/ledger-shadow} 同源合并，供监控页一张卡片展示。</p>
     */
    @GetMapping("/anomaly-summary")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "流程监控异常聚合计数",
        description = "超时 / 业务行缺失 / requestId未回填 / 部署漂移 / 测试实例 计数，并合并 ledger-shadow 引擎对账差异")
    public R<Map<String, Object>> anomalySummary() {
        Map<String, Object> data = instanceService.monitorAnomalySummary();
        Map<String, Long> diffs = projector.diffSnapshot();
        data.put("ledgerTotalDiff", diffs.values().stream().mapToLong(Long::longValue).sum());
        data.put("ledgerListenerRegistered", ledgerListenerProvider.getIfAvailable() != null);
        return R.data(data);
    }

    /**
     * 流程监控：管理员视角的全租户实例分页检索。
     *
     * <p>筛选条件由 {@link MonitorInstanceQuery} 从请求参数绑定；current/pageSize 走分页。</p>
     */
    @GetMapping("/instances")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "流程监控列表",
        description = "流程管理员看全租户；被委托用户(wf_monitor_scope)只看授权范围；无委托的普通用户返回空。")
    public R<IPage<InstanceVO>> instances(
        @RequestParam(required = false, defaultValue = "1") Long current,
        @RequestParam(required = false, defaultValue = "20") Long pageSize,
        MonitorInstanceQuery q) {
        return R.data(instanceService.monitorList(current, pageSize, q));
    }

    @PostMapping("/instances/{id}/force-recall")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    @Operation(summary = "强制收回(收回重填)",
        description = "方案A 原地回退：把引擎 token 移回目标节点（缺省=创建节点），实例不终止、历史连续；targetNodeKey 可指定其它节点")
    public R<Boolean> forceRecall(@PathVariable Long id,
                                  @RequestParam(required = false) String targetNodeKey,
                                  @RequestParam(required = false) String opinion) {
        return R.data(instanceService.forceRecallTo(id, targetNodeKey, opinion));
    }

    @PostMapping("/instances/{id}/force-archive")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    @Operation(summary = "强制归档(强制办结)")
    public R<Boolean> forceArchive(@PathVariable Long id, @RequestParam(required = false) String opinion) {
        return R.data(instanceService.forceArchive(id, opinion));
    }

    @DeleteMapping("/instances/{id}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    @Operation(summary = "强制删除实例")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.data(instanceService.forceDelete(id));
    }

    @PostMapping("/instances/batch")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    @Operation(summary = "批量监管动作(stop/resume/archive/recall/delete)")
    public R<MonitorBatchResult> batch(
        @RequestParam String action,
        @RequestParam List<Long> ids) {
        return R.data(instanceService.batchMonitorAction(ids, action));
    }

}
