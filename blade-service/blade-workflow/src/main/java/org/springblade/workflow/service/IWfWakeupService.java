package org.springblade.workflow.service;

import java.util.Map;

/**
 * S4 通用等待唤醒（2026-11-18）：统一收口 ReceiveTask.trigger 与 messageEventReceived 两种引擎唤醒。
 *
 * <p>对齐文档《全量BPMN元素启用与原有流程语义兼容架构分析》§4.2「ReceiveTask/消息」：
 * 外部系统通过该服务把等待态流程唤醒，token 继续往下流。</p>
 */
public interface IWfWakeupService {

    /**
     * 按业务实例 ID 唤醒等待节点（ReceiveTask）。
     *
     * <p>定位：用 {@code ACT_RU_EXECUTION} 按 PROC_INST_ID_ + ACT_ID_ 找等待 execution，
     * 然后 {@code runtimeService.trigger(executionId, variables)}。</p>
     *
     * @param engineInstId 引擎流程实例ID
     * @param nodeKey      等待节点 key（ReceiveTask id）；为空时取首个等待 execution
     * @param variables    唤醒时写入的流程变量（可为 null）
     * @return true=成功唤醒（有等待 execution 且 trigger 成功）；false=实例无等待点
     * @throws org.springblade.core.log.exception.ServiceException 引擎实例不存在 / 无权限 / trigger 失败
     */
    boolean triggerReceive(String engineInstId, String nodeKey, Map<String, Object> variables);

    /**
     * 按消息名唤醒消息捕获事件（messageEventReceived）。
     *
     * <p>Flowable 8.1 签名为 {@code messageEventReceived(messageName, executionId, variables)}——
     * messageName 在前。内部先按 PROC_INST_ID_ + 消息事件定位等待 execution。</p>
     *
     * @param engineInstId 引擎流程实例ID
     * @param messageName  消息名（BPMN messageEventDefinition 的 messageRef name）
     * @param variables    唤醒时写入的流程变量（可为 null）
     * @return true=成功唤醒；false=实例无匹配的等待 execution
     * @throws org.springblade.core.log.exception.ServiceException 引擎实例不存在 / 无权限 / 唤醒失败
     */
    boolean signalMessage(String engineInstId, String messageName, Map<String, Object> variables);
}
