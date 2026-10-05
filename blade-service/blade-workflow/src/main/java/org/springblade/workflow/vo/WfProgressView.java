package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程实例进度图（四色，纯 ACT_HI）视图。
 *
 * <p>对齐前端「ACT_HI 四色节点落到 BPMN/SIMPLE 双 Viewer」方案：后端只读 Flowable 历史表
 * （{@code ACT_HI_ACTINST} / {@code ACT_HI_TASKINST} / {@code ACT_HI_PROCINST}），一次返回
 * 实例绑定的 BPMN 与四个 activityId 集合 + 实例状态。<b>前端只 addMarker，不自行猜测</b>。</p>
 *
 * <p>四个集合语义（与前端 {@code buildActivityColors} 对应）：</p>
 * <ul>
 *   <li>{@code unfinishedActivityIds}   进行中（蓝/primary）：ACT_HI_ACTINST 中 endTime == null 的活动；</li>
 *   <li>{@code finishedActivityIds}     已完成活动（绿/success，已剔除仍在进行的节点）；</li>
 *   <li>{@code finishedSequenceFlowIds} 已走连线（绿/success）：条件网关实际走的分支；</li>
 *   <li>{@code rejectedActivityIds}     拒绝点（红/danger）：仅实例为「不通过(2)」时＝最后一条退回日志的节点；</li>
 * </ul>
 *
 * <p>{@code cancelledActivityIds} 本条不主动填——取消/拒绝态的 EndEvent 去绿由前端按
 * {@code instanceStatus}（2/3）在 BPMN Viewer 内按元素类型 EndEvent 强制改灰完成（文章规则）。</p>
 *
 * <p>BPMN 取数：用实例实际使用的流程定义（{@code wf_instance.def_id} → {@code wf_definition.bpmn_xml}），
 * 即发起时的那一版，<b>不是</b>按 procKey 取最新，避免改图后老单高亮错位。</p>
 */
@Data
@Schema(description = "流程实例进度图（四色，纯 ACT_HI）")
public class WfProgressView {

    @Schema(description = "实例绑定的 BPMN XML（原始 XML，非 base64；用于 BPMN Viewer 渲染）")
    private String bpmnXml;

    @Schema(description = "进行中节点 activityId（蓝/primary）：ACT_HI_ACTINST 中 endTime == null")
    private List<String> unfinishedActivityIds = new ArrayList<>();

    @Schema(description = "已完成活动 activityId（绿/success，已剔除仍在进行中的节点）")
    private List<String> finishedActivityIds = new ArrayList<>();

    @Schema(description = "已走连线 activityId（绿/success）：条件网关实际走的分支")
    private List<String> finishedSequenceFlowIds = new ArrayList<>();

    @Schema(description = "拒绝点 activityId（红/danger）：仅实例为「不通过(2)」时填最后退回节点")
    private List<String> rejectedActivityIds = new ArrayList<>();

    @Schema(description = "取消态 EndEvent activityId（灰/cancel）：本条不主动填，前端按 instanceStatus 纠偏")
    private List<String> cancelledActivityIds = new ArrayList<>();

    @Schema(description = "实例状态 0运行中 1通过 2不通过 3撤销 4暂停（前端据其纠偏 EndEvent 去绿）")
    private Integer instanceStatus;
}
