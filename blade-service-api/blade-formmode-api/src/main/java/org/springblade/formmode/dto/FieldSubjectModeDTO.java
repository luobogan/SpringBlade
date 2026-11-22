package org.springblade.formmode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** 按主体保存字段权限时的单个模块/表单字段集合。 */
@Data
@Schema(description = "主体字段权限-模块集合")
public class FieldSubjectModeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模块ID或表单ID兜底（字符串避免雪花ID精度丢失）")
    private String modeId;

    @Schema(description = "表单ID（仅用于回显，字符串）")
    private String formId;

    @Schema(description = "该模块下提交的字段权限")
    private List<FieldSubjectItemDTO> fields;
}
