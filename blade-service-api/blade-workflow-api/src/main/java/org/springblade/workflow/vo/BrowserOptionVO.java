package org.springblade.workflow.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 浏览框选项（对齐 ecology BrowserBean 的返回值结构）。
 *
 * <p>后端按浏览框类型（如 wftype）查询后，以统一结构 {value, label, description} 返回，
 * 前端 {@code BrowserBox} 渲染选择弹窗。value 通常为被引用记录的主键。</p>
 */
@Data
@Schema(description = "浏览框选项")
public class BrowserOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "值（通常为被引用记录的主键）")
    private Object value;

    @Schema(description = "显示文案")
    private String label;

    @Schema(description = "附加描述")
    private String description;

}
