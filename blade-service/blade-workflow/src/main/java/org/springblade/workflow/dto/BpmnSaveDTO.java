package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 保存 BPMN（画布产出）请求体。
 *
 * <p>对应 {@code PUT /definition/{id}/bpmn}。</p>
 *
 * <p>⚠️ 这里刻意用<b>实体类</b>而不是 {@code Map<String, String>}：
 * 本运行环境下 {@code @RequestBody} 的根类型只要是泛型容器（{@code Map} / {@code List}）就会
 * 丢掉泛型信息、绑定出空值或抛出
 * {@code Cannot deserialize value of type java.util.LinkedHashMap<String,Object>}，
 * 导致保存画布固定返回「BPMN 内容不能为空」。实体类（其内部字段可用泛型）不受影响。</p>
 */
@Data
@Schema(description = "保存 BPMN 请求")
public class BpmnSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "BPMN 内容不能为空")
    @Schema(description = "bpmn-js 导出的 BPMN 2.0 XML")
    private String bpmnXml;

}
