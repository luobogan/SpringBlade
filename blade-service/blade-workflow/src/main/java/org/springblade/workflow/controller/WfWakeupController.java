package org.springblade.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.service.IWfWakeupService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * S4 通用等待唤醒入口（2026-11-18）。
 *
 * <p><b>内部入口</b>（{@code /wakeup/trigger}、{@code /wakeup/message}）：仅流程管理员，
 * 供前端/内部服务把等待态流程推进。</p>
 *
 * <p><b>外部入口</b>在 {@link WfWakeupExternalController}（独立 bean，默认不存在），本 Controller 只承载内部入口。</p>
 */
@RestController
@RequestMapping("/wakeup")
@RequiredArgsConstructor
@Tag(name = "等待唤醒", description = "ReceiveTask / 消息事件的唤醒入口")
public class WfWakeupController {

    private final IWfWakeupService wakeupService;

    /** 内部入口：ReceiveTask 唤醒（仅流程管理员） */
    @PostMapping("/trigger")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    @Operation(summary = "唤醒 ReceiveTask", description = "按引擎实例ID+节点Key触发等待的 ReceiveTask")
    public R<Boolean> trigger(@RequestBody WakeupRequest req) {
        return R.data(wakeupService.triggerReceive(req.getEngineInstId(), req.getNodeKey(), req.getVariables()));
    }

    /** 内部入口：消息事件唤醒（仅流程管理员） */
    @PostMapping("/message")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    @Operation(summary = "唤醒消息事件", description = "按引擎实例ID+消息名触发 messageEventReceived")
    public R<Boolean> message(@RequestBody WakeupRequest req) {
        return R.data(wakeupService.signalMessage(req.getEngineInstId(), req.getMessageName(), req.getVariables()));
    }

    /** 唤醒请求体 */
    @lombok.Data
    public static class WakeupRequest {
        /** 引擎流程实例ID */
        private String engineInstId;
        /** 节点 key（ReceiveTask 用） */
        private String nodeKey;
        /** 消息名（message 用） */
        private String messageName;
        /** 唤醒类型：trigger（默认）/ message（仅外部入口用） */
        private String type;
        /** 唤醒时写入的流程变量 */
        private Map<String, Object> variables;
    }
}
