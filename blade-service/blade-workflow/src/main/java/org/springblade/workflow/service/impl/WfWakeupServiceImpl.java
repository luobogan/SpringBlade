package org.springblade.workflow.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.service.IWfWakeupService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * S4 通用等待唤醒实现（2026-11-18）。
 *
 * <p>两种唤醒共用「查等待 execution → 调引擎 API」范式，区别只在最终调用：</p>
 * <ul>
 *   <li>ReceiveTask → {@code runtimeService.trigger(executionId, variables)}</li>
 *   <li>message 捕获事件 → {@code runtimeService.messageEventReceived(messageName, executionId, variables)}
 *       （注意 Flowable 8.1 签名 messageName 在前）</li>
 * </ul>
 *
 * <p><b>安全护栏</b>：独立开关 {@code blade.workflow.compat.s4-wakeup-enabled} 默认关；
 * 唤醒是「推进流程」的写操作，内部入口走 HAS_AUTH，外部入口（若启用）需 HMAC 验签 + 幂等。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfWakeupServiceImpl implements IWfWakeupService {

    private final RuntimeService runtimeService;
    private final JdbcTemplate jdbcTemplate;
    private final WfFlowtableCompatProperties compat;

    @Override
    public boolean triggerReceive(String engineInstId, String nodeKey, Map<String, Object> variables) {
        ensureEnabled();
        requireInstId(engineInstId);
        String executionId = findWaitingExecution(engineInstId, nodeKey);
        if (executionId == null) {
            log.info("[s4-wakeup] 无等待 execution，跳过 trigger. procInstId={}, nodeKey={}", engineInstId, nodeKey);
            return false;
        }
        try {
            runtimeService.trigger(executionId, variables == null ? Collections.emptyMap() : variables);
            log.info("[s4-wakeup] ReceiveTask 已唤醒. procInstId={}, executionId={}, nodeKey={}",
                engineInstId, executionId, nodeKey);
            return true;
        } catch (Exception e) {
            throw new ServiceException("ReceiveTask 唤醒失败. executionId=" + executionId + ", err=" + e.getMessage());
        }
    }

    @Override
    public boolean signalMessage(String engineInstId, String messageName, Map<String, Object> variables) {
        ensureEnabled();
        requireInstId(engineInstId);
        if (messageName == null || messageName.isBlank()) {
            throw new ServiceException("消息名为空，无法唤醒");
        }
        String executionId = findMessageExecution(engineInstId, messageName);
        if (executionId == null) {
            log.info("[s4-wakeup] 无匹配消息等待 execution，跳过. procInstId={}, message={}", engineInstId, messageName);
            return false;
        }
        try {
            // Flowable 8.1 签名：messageEventReceived(messageName, executionId, variables) —— messageName 在前
            runtimeService.messageEventReceived(messageName, executionId,
                variables == null ? Collections.emptyMap() : variables);
            log.info("[s4-wakeup] 消息事件已唤醒. procInstId={}, executionId={}, message={}",
                engineInstId, executionId, messageName);
            return true;
        } catch (Exception e) {
            throw new ServiceException("消息唤醒失败. message=" + messageName + ", err=" + e.getMessage());
        }
    }

    private void ensureEnabled() {
        if (!compat.isS4WakeupEnabled()) {
            throw new ServiceException("S4 唤醒能力未开启（blade.workflow.compat.s4-wakeup-enabled=false）");
        }
    }

    private static void requireInstId(String engineInstId) {
        if (engineInstId == null || engineInstId.isBlank()) {
            throw new ServiceException("引擎流程实例ID为空");
        }
    }

    /** 查 ReceiveTask 等待 execution：ACT_RU_EXECUTION 中 ACT_ID_ 匹配节点且非流程根 execution */
    private String findWaitingExecution(String procInstId, String nodeKey) {
        String sql;
        Object[] args;
        if (nodeKey != null && !nodeKey.isBlank()) {
            sql = "SELECT ID_ FROM ACT_RU_EXECUTION WHERE PROC_INST_ID_ = ? AND ACT_ID_ = ? "
                + "AND IS_ACTIVE_ = TRUE ORDER BY ID_ LIMIT 1";
            args = new Object[]{procInstId, nodeKey};
        } else {
            // 不指定节点：取第一个有 ACT_ID_ 的非根 execution（根 execution 的 ACT_ID_ 通常为空或等于流程key）
            sql = "SELECT ID_ FROM ACT_RU_EXECUTION WHERE PROC_INST_ID_ = ? "
                + "AND ACT_ID_ IS NOT NULL AND ACT_ID_ <> ? AND IS_ACTIVE_ = TRUE "
                + "ORDER BY ID_ LIMIT 1";
            args = new Object[]{procInstId, procInstId};
        }
        List<String> ids = jdbcTemplate.queryForList(sql, String.class, args);
        return ids.isEmpty() ? null : ids.get(0);
    }

    /**
     * 查消息事件等待 execution。
     *
     * <p>消息名存在 ACT_RU_EXECUTION 的 ACT_ID_（元素 id）或 handler_cfg 中；这里用保守口径：
     * 按 messageName 作为 ACT_ID_ 精确匹配，或由调用方直接传元素 id 作为 messageName。</p>
     */
    private String findMessageExecution(String procInstId, String messageName) {
        String sql = "SELECT ID_ FROM ACT_RU_EXECUTION WHERE PROC_INST_ID_ = ? "
            + "AND (ACT_ID_ = ? OR ACT_ID_ LIKE ?) AND IS_ACTIVE_ = TRUE ORDER BY ID_ LIMIT 1";
        List<String> ids = jdbcTemplate.queryForList(sql, String.class, procInstId, messageName, "%" + messageName + "%");
        return ids.isEmpty() ? null : ids.get(0);
    }
}
