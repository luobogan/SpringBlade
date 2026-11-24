package org.springblade.formmode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 表单布局类型显示名保存请求 */
@Data
@Schema(description = "表单布局类型显示名保存请求")
public class FormLayoutTypeNameDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "表单ID（字符串避免雪花ID精度丢失）")
    private String formId;

    @Schema(description = "布局类型：0编辑/新增 1显示 3监控 4打印")
    private Integer layoutType;

    @Schema(description = "显示名")
    private String layoutName;
}
