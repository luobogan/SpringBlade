package org.springblade.formmode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 按主体保存字段权限时的单个字段项。 */
@Data
@Schema(description = "主体字段权限项")
public class FieldSubjectItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "字段ID（workflow_billfield.id）")
    private Long fieldId;

    /** 0隐藏 1可见 2可编辑 */
    @Schema(description = "权限值：0隐藏 1可见 2可编辑")
    private Integer rightvalue;
}
