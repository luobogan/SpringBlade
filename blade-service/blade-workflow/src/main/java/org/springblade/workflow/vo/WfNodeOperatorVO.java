package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 节点操作者情况（流程图节点悬浮「操作者」面板）。
 *
 * <p>对齐 ecology 流程图：鼠标悬浮节点弹出的「操作者」分组面板，按
 * <b>已操作 / 已查看 / 未操作</b> 三组列出该节点的办理人。</p>
 *
 * <p>分组口径：</p>
 * <ul>
 *   <li><b>已操作</b>：该节点已有办理动作的人——{@code wf_task} 状态为
 *       已办(2)/办结(4)/自动提交(6)/协办(7)，或 {@code wf_approval_log} 里有其操作记录
 *       （加签、转办等不一定留下本人 task 行）；开始节点取发起人。</li>
 *   <li><b>已查看</b>：仍是待办(0) 但已打开过（{@code wf_task.view_time} 非空）——看了没办。</li>
 *   <li><b>未操作</b>：仍是待办(0) 且从未打开过。</li>
 * </ul>
 *
 * <p>⚠️ 只回传用户ID（不回姓名）：姓名统一由前端人员字典解析，与「操作人」姓名同源，
 * 避免后端逐个远程查姓名造成 N 次调用。</p>
 */
@Data
@Schema(description = "节点操作者情况（已操作/已查看/未操作）")
public class WfNodeOperatorVO {

    @Schema(description = "已操作人员ID（按发生时间升序、去重）")
    private List<Long> handled = new ArrayList<>();

    @Schema(description = "已查看未操作人员ID（待办已打开过）")
    private List<Long> viewed = new ArrayList<>();

    @Schema(description = "未操作人员ID（待办尚未打开）")
    private List<Long> todo = new ArrayList<>();
}
