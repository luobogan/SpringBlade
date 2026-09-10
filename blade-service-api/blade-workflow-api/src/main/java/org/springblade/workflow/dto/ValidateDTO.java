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

}
