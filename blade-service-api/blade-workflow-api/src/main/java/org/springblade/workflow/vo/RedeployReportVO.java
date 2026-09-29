package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量重部署结果报告（会签/或签下沉多实例用）。
 *
 * <p>多实例（{@code multiInstanceLoopCharacteristics}）只在<b>部署期</b>注入，仅改开关不会改写
 * 已部署定义，必须重新部署。本 VO 用于回传批量重部署的执行结果，便于运维逐条核对。</p>
 */
@Data
@Schema(description = "批量重部署结果报告")
public class RedeployReportVO {

    @Schema(description = "命中的定义数（已发布且有 BPMN）")
    private int total;

    @Schema(description = "实际尝试重部署数")
    private int attempted;

    @Schema(description = "成功数")
    private int success;

    @Schema(description = "失败数")
    private int failed;

    @Schema(description = "因引擎侧已注入多实例而跳过的数量")
    private int skipped;

    @Schema(description = "多实例总开关是否开启（关闭时重部署不会注入 MI）")
    private boolean miEnabled;

    @Schema(description = "失败明细（defId / procKey / 原因）")
    private List<String> failures = new ArrayList<>();

    @Schema(description = "结果说明")
    private String message;
}
