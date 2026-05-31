package org.springblade.formmode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 字段定义 DTO
 */
@Data
@Schema(description = "字段定义")
public class FieldDefinitionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "字段ID")
    private Integer id;

    @Schema(description = "字段名")
    private String fieldname;

    @Schema(description = "数据库列名")
    private String fielddbname;

    @Schema(description = "HTML字段类型")
    private Integer fieldhtmltype;

    @Schema(description = "字段类型")
    private Integer fieldtype;

    @Schema(description = "字段长度")
    private Integer fieldlen;

    @Schema(description = "小数点位数")
    private Integer decimaldigit;

    @Schema(description = "是否必填")
    private Boolean required;

    @Schema(description = "默认值")
    private String defaultvalue;

    @Schema(description = "显示顺序")
    private Integer dsporder;

    @Schema(description = "选项列表（下拉框/单选/复选）")
    private List<String> options;

    @Schema(description = "浏览框类型")
    private Integer browtype;

    @Schema(description = "租户ID")
    private String tenantId;

}
