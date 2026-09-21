package org.springblade.workflow.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
    @JsonSerialize(using = ToStringSerializer.class)
    private Long instanceId;

    @Schema(description = "流程定义ID（wf_process_definition.id，19位雪花ID需 ToString 保精度）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long defId;

    @Schema(description = "当前任务ID（若为待办渲染）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;

    @Schema(description = "当前节点Key")
    private String nodeKey;

    @Schema(description = "当前节点名称")
    private String nodeName;

    @Schema(description = "表单ID（workflow_bill.id）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long formId;

    @Schema(description = "业务数据ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long dataId;

    @Schema(description = "流程实例状态 0运行中 1通过 2不通过 3撤销 4暂停")
    private Integer instanceStatus;

    @Schema(description = "布局ID（form_layout.id）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long layoutId;

    @Schema(description = "布局 JSON（Univer 结构：sheetOrder/sheets/detailTables）")
    private String layoutJson;

    @Schema(description = "业务数据（主表 + 明细表，key 沿用 {sheetId}__{row}__{col} / dt{idx}__r{n}__...）")
    private Map<String, Object> dataJson;

    @Schema(description = "节点字段权限矩阵")
    private List<FieldPermVO> fieldPerms;

    @Schema(description = "节点明细表权限")
    private List<DetailPermVO> detailPerms;

    /**
     * 节点明细表「显示时」字段筛选规则（来自 {@code wf_node_detail_filter} modeType=1）。
     * 审批态渲染据此隐藏不满足规则的明细行（对齐 ecology「明细表数据根据操作者筛选显示」）。
     */
    @Schema(description = "节点明细表显示时字段筛选规则（运行期据此过滤明细行）")
    private List<DetailFilterVO> detailFilters;

    @Schema(description = "是否只读（无办理权限时为 true，前端整体降级为只读）")
    private Boolean readonly;

    /**
     * 当前节点「操作菜单」配置允许的操作：submit/reject/forward/sign/opinion/attach/print/urge。
     * null = 未配置（不限制，按系统默认全部可用）；空数组 = 配置过但一个都不允许。
     */
    @Schema(description = "当前节点可用操作（来自节点信息「操作菜单」；null=不限制）")
    private List<String> allowMenus;

    /** 当前节点签字意见是否必填（旧口径：等价于 mustInput=all） */
    @Schema(description = "签字意见是否必填（旧口径）")
    private Boolean opinionRequired;

    /** 签字意见必填模式：never / all / byOperation（缺省 never） */
    @Schema(description = "签字意见必填模式")
    private String opinionMustInput;

    /** byOperation 模式下必填的操作类型（缺省 退回+征询回复） */
    @Schema(description = "必填操作类型（byOperation 模式生效）")
    private List<String> opinionMustInputOperations;

    /** 意见输入框是否不显示 */
    @Schema(description = "意见输入框不显示")
    private Boolean opinionHideInput;

    /** 意见区域是否整体不显示（输入框+历史意见一并隐藏） */
    @Schema(description = "意见区域整体不显示")
    private Boolean opinionHideArea;

    /** 意见显示范围模式：all / none / list */
    @Schema(description = "意见显示范围模式")
    private String opinionViewMode;

    /** 意见显示范围：list 模式下的节点Key集合 */
    @Schema(description = "意见显示范围节点集合")
    private List<String> opinionViewNodeKeys;

    /** 同节点办理人互不可见彼此意见 */
    @Schema(description = "同节点互不可见意见")
    private Boolean opinionNotSeeEachOther;

    /** 意见反馈（回写/反馈给上游节点） */
    @Schema(description = "意见反馈")
    private Boolean opinionFeedback;

    /** 意见为空时不反馈（需配合 feedback） */
    @Schema(description = "意见为空不反馈")
    private Boolean opinionNullNotFeedback;

    /** 当前节点「打印内容设置」（来自节点信息 → 表单内容 → 打印模板 → 打印内容设置） */
    @Schema(description = "节点打印内容设置")
    private PrintSetVO printSet;

    /** 当前节点「签字意见显示设置」（来自节点信息 → 表单内容 → 节点意见 → 意见显示设置） */
    @Schema(description = "节点签字意见显示设置")
    private OpinionDisplayVO opinionDisplay;

}
