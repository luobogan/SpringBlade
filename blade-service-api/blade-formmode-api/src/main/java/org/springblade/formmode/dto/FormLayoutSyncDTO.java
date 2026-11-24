package org.springblade.formmode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** 表单布局同步请求（同表单其他组 / 同组其他布局类型） */
@Data
@Schema(description = "表单布局同步请求")
public class FormLayoutSyncDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "表单ID（字符串避免雪花ID精度丢失）")
    private String formId;

    @Schema(description = "源布局nodeKey（授权对象组groupKey；空=表单级）")
    private String sourceNodeKey;

    @Schema(description = "源布局类型")
    private Integer sourceLayoutType;

    @Schema(description = "目标nodeKey列表（同步到其他组时使用）")
    private List<String> targetNodeKeys;

    @Schema(description = "目标布局类型列表（同步到其他布局类型时使用）")
    private List<Integer> targetLayoutTypes;

    @Schema(description = "目标已有布局时是否覆盖")
    private Boolean overwrite;
}
