package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 表单服务端校验请求
 *
 * <p>对应 {@code POST /form/validate}：按<b>节点必填矩阵</b>复核，
 * 前端校验不可信（对齐文档 3.4-9）。同时校验明细表「必须新增」约束。</p>
 */
@Data
@Schema(description = "表单校验请求")
public class ValidateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程实例ID")
    private Long instanceId;

    @Schema(description = "节点Key")
    private String nodeKey;

    @Schema(description = "流程定义ID（未发起时用）")
    private Long defId;

    @Schema(description = "待校验表单数据（key 沿用 {sheetId}__{row}__{col} / dt{idx}__r{n}__...）")
    private Map<String, Object> formData;

    /**
     * 测试域「读草稿」标记：true=字段权限取 {@code wf_process_definition.bpmn_xml}（草稿）
     * 而非 {@code proc_def_id} 指向的已部署版本。
     *
     * <p>仅一键测试 / 测试页提交置 true：设计器改的是草稿，若测试仍按部署读，
     * 就会「改了必填却仍报必填」，逼用户每次先「发布」再测（2026-10-11 用户反馈不合理）。
     * 正式提交留空/false，仍按已部署版本判定。</p>
     */
    @Schema(description = "测试域：字段权限读草稿 BPMN（true=读草稿，不要求先发布）")
    private Boolean draftRead;

}
