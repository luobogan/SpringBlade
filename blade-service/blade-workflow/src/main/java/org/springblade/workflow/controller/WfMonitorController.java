package org.springblade.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.service.IWfTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 流程运维控制器
 *
 * <p>对应文档 §6.5：待办/已办计数（当前直查 DB，后续可接入 Redis 缓存，
 * 任务状态变更时失效缓存）。</p>
 *
 * <p><b>鉴权</b>：业务接口（{@code /count}）要求 {@code administrator} 角色（与其它
 * 流程控制器对齐）；{@code /health/live}、{@code /health/ready} 为基础设施探针，
 * 无用户上下文，<b>保持放行</b>，不纳入角色门禁。</p>
 */
@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
@Tag(name = "流程运维", description = "待办计数与健康检查")
public class WfMonitorController {

    private final IWfTaskService taskService;

    @GetMapping("/count")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    @Operation(summary = "待办/已办计数")
    public R<Map<String, Long>> count(
        @Parameter(description = "办理人，默认当前登录人") @RequestParam(value = "assignee", required = false) Long assignee) {
        Long userId = (assignee != null) ? assignee : SecureUtil.getUserId();
        return R.data(taskService.count(userId));
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

}
