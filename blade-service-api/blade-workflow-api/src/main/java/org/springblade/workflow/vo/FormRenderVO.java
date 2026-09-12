package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 审批态渲染包
 *
 * <p>对应文档 §6.4 {@code GET /form/render?instanceId=&taskId=}：
 * 一次返回「布局 + 业务数据 + 节点字段权限 + 明细权限」，避免前端多次往返，
 * 同时把表单侧（布局/字段/业务数据）与流程侧（权限）的耦合收敛在该接口内。</p>
 *
 * <p>设计要点：布局与数据解耦——layoutJson 指向当前布局，dataJson 为本次渲染的数据，
 * 历史实例可结合 {@code wf_form_snapshot} 原样重现。</p>
 */
@Data
@Schema(description = "审批态渲染包")
public class FormRenderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程实例ID")
    private Long instanceId;

    @Schema(description = "当前任务ID（若为待办渲染）")
    private Long taskId;

    @Schema(description = "当前节点Key")
    private String nodeKey;

    @Schema(description = "当前节点名称")
    private String nodeName;

    @Schema(description = "表单ID（workflow_bill.id）")
    private Long formId;

    @Schema(description = "业务数据ID")
    private Long dataId;

    @Schema(description = "流程实例状态 0运行中 1通过 2不通过 3撤销 4暂停")
    private Integer instanceStatus;

    @Schema(description = "布局ID（form_layout.id）")
    private Long layoutId;

    @Schema(description = "布局 JSON（Univer 结构：sheetOrder/sheets/detailTables）")
    private String layoutJson;

    @Schema(description = "业务数据（主表 + 明细表，key 沿用 {sheetId}__{row}__{col} / dt{idx}__r{n}__...）")
    private Map<String, Object> dataJson;

    @Schema(description = "节点字段权限矩阵")
    private List<FieldPermVO> fieldPerms;

    @Schema(description = "节点明细表权限")
    private List<DetailPermVO> detailPerms;

    @Schema(description = "是否只读（无办理权限时为 true，前端整体降级为只读）")
    private Boolean readonly;

    /**
     * 当前节点「操作菜单」配置允许的操作：submit/reject/forward/sign/opinion/attach/print/urge。
     * null = 未配置（不限制，按系统默认全部可用）；空数组 = 配置过但一个都不允许。
     */
    @Schema(description = "当前节点可用操作（来自节点信息「操作菜单」；null=不限制）")
    private List<String> allowMenus;

    /** 当前节点签字意见是否必填（来自节点信息「签字意见设置」） */
    @Schema(description = "签字意见是否必填")
    private Boolean opinionRequired;

}
