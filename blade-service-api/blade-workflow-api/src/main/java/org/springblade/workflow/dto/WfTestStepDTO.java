package org.springblade.workflow.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 流程测试-单步推进入参
 *
 * <p>对应 {@code POST /test/step}。交互式测试下，「开始自动测试」由前端循环调用本接口
 * 逐节点推进（可随时暂停）；「手动测试」由用户填写表单后点「提交」调用一次。</p>
 */
@Data
@Schema(description = "流程测试-单步推进入参")
public class WfTestStepDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 字符串序列化：19 位雪花 ID 在前端 JS 解析会丢精度 */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "测试态实例ID（wf_instance.id，is_test=1）")
    private Long instId;

    @Schema(description = "签字意见（为空时用默认「流程测试提交」）")
    private String opinion;

    /**
     * 当前节点表单值（手动测试可修改后提交）。
     * <p>非空时会作为<b>流程变量</b>下发引擎，用于驱动后续排他网关按真实条件选分支，
     * 并先落一份表单快照；为空时取实例最新快照值。</p>
     */
    @Schema(description = "当前节点表单值（手动测试可修改后提交；作为流程变量驱动网关）")
    private Map<String, Object> formData;

}
