package org.springblade.formmode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 表单模块 DTO
 */
@Data
@Schema(description = "表单模块DTO")
public class FormModeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模块名称")
    private String modename;

    @Schema(description = "模块描述")
    private String modedescription;

    @Schema(description = "表单名称")
    private String billname;

    @Schema(description = "应用分类ID")
    private Long modeTreeField;

    @Schema(description = "字段定义列表")
    private List<FieldDefinitionDTO> fields;

    @Schema(description = "状态：1启用 0禁用")
    private Integer status;

}
