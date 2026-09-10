package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 节点字段权限项（前端渲染用）
 *
 * <p>对齐 {@code data-excelp-scope}（{@code main} / {@code dt{idx}}）与
 * {@code data-excelp-field} 定位锚点，perm 语义同 ecology fieldattr。</p>
 */
@Data
@Schema(description = "节点字段权限项")
public class FieldPermVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "作用域：main | dt{idx} | dt{idx}_r{row}（与 data-excelp-scope 对齐；dt{idx}_r{row} 为 B5 行级）")
    private String scope;

    @Schema(description = "字段名（与 data-excelp-field 对齐）")
    private String fieldName;

    @Schema(description = "0隐藏 1只读 2可编辑 3必填")
    private Integer perm;

    public FieldPermVO() {
    }

    public FieldPermVO(String scope, String fieldName, Integer perm) {
        this.scope = scope;
        this.fieldName = fieldName;
        this.perm = perm;
    }

}
