package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 节点明细表字段筛选项（前端渲染 / 保存用）
 */
@Data
@Schema(description = "节点明细表字段筛选项")
public class DetailFilterVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "明细表序号")
    private Integer dtIndex;

    @Schema(description = "比较字段（明细列的 fieldName）")
    private String fieldName;

    @Schema(description = "比较方式 1等于 2不等于 3包含 4不包含")
    private Integer compareType;

    @Schema(description = "比较值（多值用逗号分隔）")
    private String compareValue;

    @Schema(description = "过滤后要求至少一条 1=是 0=否")
    private Integer isRequired;

}
