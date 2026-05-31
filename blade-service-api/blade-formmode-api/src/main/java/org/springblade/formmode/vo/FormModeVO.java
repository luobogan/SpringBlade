package org.springblade.formmode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.springblade.formmode.dto.FieldDefinitionDTO;

/**
 * 表单模块 VO
 */
@Data
@Schema(description = "表单模块视图")
public class FormModeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模块ID")
    private Long id;

    @Schema(description = "模块名称")
    private String modename;

    @Schema(description = "模块描述")
    private String modedescription;

    @Schema(description = "表单ID")
    private Integer billid;

    @Schema(description = "物理表名")
    private String tablename;

    @Schema(description = "明细表列表")
    private List<String> detailTableNames;

    @Schema(description = "字段定义列表")
    private List<FieldDefinitionDTO> fields;

    @Schema(description = "权限信息")
    private Map<String, Object> rights;

    @Schema(description = "自定义搜索列表")
    private List<Map<String, Object>> customSearches;

    @Schema(description = "状态：1启用 0禁用")
    private Integer status;

}
