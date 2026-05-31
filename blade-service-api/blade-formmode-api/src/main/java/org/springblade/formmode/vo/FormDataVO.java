package org.springblade.formmode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 表单数据 VO
 */
@Data
@Schema(description = "表单数据视图")
public class FormDataVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "数据ID")
    private Long id;

    @Schema(description = "模块ID")
    private Long modeid;

    @Schema(description = "主表字段值 key=fielddbname, value=字段值（含显示值）")
    private Map<String, Object> fieldValues;

    @Schema(description = "明细表数据")
    private Map<String, List<Map<String, Object>>> detailTables;

    @Schema(description = "创建人")
    private Integer creater;

    @Schema(description = "创建人名称")
    private String createrName;

    @Schema(description = "创建日期")
    private String createdate;

    @Schema(description = "创建时间")
    private String createtime;

    @Schema(description = "最后修改人")
    private Integer modifier;

    @Schema(description = "最后修改日期")
    private String modifydate;

    @Schema(description = "最后修改时间")
    private String modifytime;

}
