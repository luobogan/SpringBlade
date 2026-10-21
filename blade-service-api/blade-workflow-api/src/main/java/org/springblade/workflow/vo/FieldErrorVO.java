package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 表单校验错误项（结构化）
 *
 * <p>用于把「必填校验 / 唯一性校验」的结果从<b>拼串异常</b>升级为<b>可按字段定位</b>的结构，
 * 前端可据此在对应控件上标红并展示提示，而不是只弹一句聚合文案。</p>
 *
 * <p><b>source 取值</b>：{@code node} 节点字段权限矩阵；{@code layout} 布局级必填配置；
 * {@code detail} 明细表「必须至少一条」；{@code unique} 唯一性校验。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "表单校验错误项")
public class FieldErrorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字段名（或明细表标识，如「明细表1（必须至少一条）」）。
     *
     * <p>与 formData 的 key 对齐，前端可直接定位控件。</p>
     */
    @Schema(description = "字段名 / 明细表标识（与 formData 的 key 对齐）")
    private String fieldName;

    /** 面向用户的提示文案 */
    @Schema(description = "提示文案")
    private String message;

    /** 错误来源：node / layout / detail / unique */
    @Schema(description = "错误来源：node=节点权限 layout=布局必填 detail=明细必须新增 unique=唯一性")
    private String source;

    public static FieldErrorVO of(String fieldName, String message, String source) {
        return new FieldErrorVO(fieldName, message, source);
    }
}
