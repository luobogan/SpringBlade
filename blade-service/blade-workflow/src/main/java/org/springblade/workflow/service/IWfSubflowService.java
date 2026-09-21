package org.springblade.workflow.service;

import org.springblade.workflow.entity.WfProcessNode;

/**
 * 子流程高级设置语义服务。
 *
 * <p>对齐 ecology 子流程「全部归档才能提交 / 数据汇总 / 全部归档后提醒 / 归档后自动流转」：
 * 触发子流程时登记 {@code wf_subflow_request}，子流程归档时回写状态并据此阻塞主流程归档、
 * 汇总数据、发送提醒、自动推进主流程。</p>
 */
public interface IWfSubflowService {

    /**
     * 登记一条主/子流程关系（节点触发子流程时调用）。
     *
     * @param mainInstId 主流程实例ID
     * @param subInstId  子流程实例ID
     * @param subDefId   子流程定义ID
     * @param node       触发子流程的主流程节点（读取其 subflow 高级设置快照）
     */
    void record(Long mainInstId, Long subInstId, Long subDefId, WfProcessNode node);

    /**
     * 子流程归档后回调：更新关系状态，并在「本节点全部子流程归档」时执行
     * 数据汇总 / 提醒 / 自动推进主流程。
     *
     * @param subInstId 刚归档的子流程实例ID
     */
    void onSubflowArchived(Long subInstId);

    /**
     * 主流程是否因「全部归档才能提交」而被阻塞（存在未归档的子流程请求）。
     * 推进实例归档前调用：返回 true 时主流程保持进行中，不归档。
     *
     * @param mainInstId 主流程实例ID
     */
    boolean holdForSubflow(Long mainInstId);

}
