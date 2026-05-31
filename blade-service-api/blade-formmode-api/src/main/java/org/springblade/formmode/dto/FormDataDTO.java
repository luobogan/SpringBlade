package org.springblade.formmode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 表单数据 DTO
 */
@Data
@Schema(description = "表单数据")
public class FormDataDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模块ID")
    private Long modeid;

    @Schema(description = "表单ID (billid)")
    private Integer billid;

    @Schema(description = "数据ID（编辑时传，新建不传）")
    private Long dataid;

    @Schema(description = "操作：save/submit/del")
    private String src;

    @Schema(description = "主表字段值 key=fielddbname, value=字段值")
    private Map<String, Object> fieldValues;

    @Schema(description = "明细表数据（明细表tablename -> 行列表）")
    private Map<String, List<Map<String, Object>>> detailTables;

    @Schema(description = "是否为新建")
    private Boolean iscreate;

    @Schema(description = "租户ID")
    private String tenantId;

}
