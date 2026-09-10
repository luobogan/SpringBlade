package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 表单条件（condition）描述。
 *
 * <p>后端下发表单字段清单，前端据此动态渲染；对齐 ecology 的 conditioninfo 结构。</p>
 */
@Data
@Schema(description = "表单条件描述（condition 驱动）")
public class FormConditionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "场景：add=新建 edit=编辑")
    private String method;

    @Schema(description = "表单标题")
    private String title;

    @Schema(description = "字段清单")
    private List<FormFieldVO> fields;

}
