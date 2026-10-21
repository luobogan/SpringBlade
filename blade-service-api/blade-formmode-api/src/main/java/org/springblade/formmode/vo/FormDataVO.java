package org.springblade.formmode.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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

    /** 字符串序列化：19 位雪花 ID 在前端 JS 解析会丢精度 */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "数据ID（雪花）")
    private Long id;

    /** 字符串序列化：19 位雪花 ID 在前端 JS 解析会丢精度 */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "模块ID（workflow_bill.id，雪花）")
    private Long modeid;

    @Schema(description = "主表字段值 key=fielddbname, value=字段值（含显示值）")
    private Map<String, Object> fieldValues;

    @Schema(description = "明细表数据")
    private Map<String, List<Map<String, Object>>> detailTables;

    /** 创建人是雪花用户ID：改 Long 避免 int 截断，且字符串序列化防 JS 丢精度 */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "创建人（雪花用户ID）")
    private Long creater;

    @Schema(description = "创建人名称")
    private String createrName;

    @Schema(description = "创建日期")
    private String createdate;

    @Schema(description = "创建时间")
    private String createtime;

    /** 修改人是雪花用户ID：改 Long 避免 int 截断，且字符串序列化防 JS 丢精度 */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "最后修改人（雪花用户ID）")
    private Long modifier;

    @Schema(description = "最后修改日期")
    private String modifydate;

    @Schema(description = "最后修改时间")
    private String modifytime;

}
