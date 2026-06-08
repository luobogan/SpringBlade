package org.springblade.formmode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 表单布局保存请求 DTO
 * 使用 String 类型的 formId 避免 JavaScript 大整数精度丢失问题
 */
@Data
@Schema(name = "表单布局保存请求", description = "用于接收前端保存表单布局的请求")
public class FormLayoutSaveDTO {

    @Schema(name = "布局ID")
    private Long id;

    @Schema(name = "表单ID（字符串格式，避免大整数精度丢失）")
    private String formId;

    @Schema(name = "布局名称")
    private String layoutName;

    @Schema(name = "布局JSON（兼容泛微E9格式）")
    private String layoutJson;

    @Schema(name = "布局配置JSON")
    private String layoutConfig;

    @Schema(name = "状态：1启用 0禁用")
    private Integer status;
}
