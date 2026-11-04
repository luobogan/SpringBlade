package org.springblade.workflow.freeflow;

import org.springblade.workflow.dto.ApproveDTO;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfTask;

import java.util.Map;

/**
 * 自由流程推进器（{@code doApprove} 分叉的唯一入口）。
 */
public interface IWfFreeFlowDriver {

    /**
     * 特性开关 + 流程定义 isFree 判定；false 时 doApprove 完全走原有链路（零回归）。
     */
    boolean isEnabled(WfInstance inst);

    /**
     * 构建请求级步骤快照（一次查库，供装饰与推进复用）。非自由实例返回空上下文。
     */
    WfFreeNodeContext context(WfInstance inst);

    /**
     * 提交当前步骤：推进到下一步或结束流程。
     *
     * @param inst    实例
     * @param task    当前待办（含 engineTaskId）
     * @param dto     提交请求（含自选办理人）
     * @param operator 当前操作人
     * @param vars    流程变量（会被追加多人集合变量）
     * @param context 步骤快照
     */
    FreeAdvanceResult submit(WfInstance inst, WfTask task, ApproveDTO dto, Long operator,
                             Map<String, Object> vars, WfFreeNodeContext context);

    /** 退回到目标步骤后重置步骤状态，避免重新提交仍按 flowOrder 一路前进 */
    void onReject(Long instId, String targetNodeKey);
}
