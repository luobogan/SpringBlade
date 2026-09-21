package org.springblade.workflow.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 实例「界面新鲜度」复检结果（页面过期识别规则）。
 *
 * <p>解决的问题：流程回退、被他人流转、归档/撤回/删除之后，浏览器里已打开的页面
 * 仍显示原节点与原状态（界面未刷新），此时继续保存/提交会把过期数据写回，甚至
 * 在已删除的草稿上「复活」出一条新流程。</p>
 *
 * <p>本 VO 是「界面与后端实例状态是否一致」这一规则的<b>唯一权威判定出口</b>：
 * 前端在打开页面、每次办理动作前、定时/切回标签页时调用
 * {@code GET /instance/{id}/fresh}，用 {@link #stale} 决定是否拦截操作，
 * 用 {@link #staleReason} 直接向用户展示原因（已成人话，无需前端再拼装）。</p>
 *
 * <p>判定口径（{@link #nodeActive} 的关键）：</p>
 * <ul>
 *   <li>实例不存在 → 过期（可能已删除）；</li>
 *   <li>实例已归档/不通过/撤销 → 过期；</li>
 *   <li>界面所在节点既不是实例当前节点、该节点上也没有待办任务 → 过期
 *       （即「流程已不在当前节点但界面仍显示原节点」，典型为回退或被他人流转）。</li>
 * </ul>
 *
 * <p>⚠️ 不能只用「界面节点 == 实例当前节点」判定：并行网关分叉后
 * {@code wf_instance.current_node_key} 只记录其中一条分支的活动节点，
 * 另一条分支上的合法办理会被误判为过期。因此额外叠加「该节点仍有待办任务」，
 * 保证并行分支、会签、依次审批场景下不误报。</p>
 */
@Data
@Schema(description = "实例界面新鲜度复检结果（页面是否已过期）")
public class InstanceFreshVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程实例ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long instanceId;

    @Schema(description = "流程实例是否存在（false=已被删除）")
    private Boolean exists;

    @Schema(description = "流程实例状态 0运行中 1通过 2不通过 3撤销 4暂停 5草稿")
    private Integer instanceStatus;

    @Schema(description = "实例当前节点Key")
    private String currentNodeKey;

    @Schema(description = "实例当前节点名称（取不到时回退为节点Key）")
    private String currentNodeName;

    @Schema(description = "界面所在节点Key（本次复检比对的节点）")
    private String nodeKey;

    @Schema(description = "界面所在节点名称（取不到时回退为节点Key）")
    private String nodeName;

    @Schema(description = "界面所在节点是否仍是该实例的活动节点（并行分支安全）")
    private Boolean nodeActive;

    @Schema(description = "界面是否已过期：true 时前端必须拦截保存/提交等所有写操作并提示刷新")
    private Boolean stale;

    @Schema(description = "过期原因（已成人话，可直接展示给用户）")
    private String staleReason;

}
