package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 表单绑定情况（流程设计 → 对应表单的占用情况）。
 *
 * <p>供 blade-formmode 删除表单前校验：若表单已被流程定义绑定（或已产生流程实例），
 * 则不允许直接删除，避免流程画布渲染表单时取不到布局/字段。</p>
 */
@Data
@Schema(description = "表单绑定情况")
public class FormBindingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否存在绑定（流程定义或流程实例）")
    private Boolean bound = Boolean.FALSE;

    @Schema(description = "绑定的流程定义数量（含草稿/停用/历史版本）")
    private Integer definitionCount = 0;

    @Schema(description = "绑定的流程实例数量")
    private Integer instanceCount = 0;

    @Schema(description = "绑定的流程名称（含版本，最多前 10 个）")
    private List<String> definitionNames = new ArrayList<>();

    /** 雪花ID以字符串下发，避免前端 JS 解析丢精度 */
    @Schema(description = "绑定的流程定义ID（字符串）")
    private List<String> definitionIds = new ArrayList<>();

    /**
     * 绑定校验失败标记：跨服务调用异常时置 true，调用方据此拒绝删除（宁可拦截，不产生脏数据）。
     */
    @Schema(description = "绑定校验是否失败（流程服务不可用）")
    private Boolean checkFailed = Boolean.FALSE;

    @Schema(description = "校验失败原因")
    private String failReason;

}
