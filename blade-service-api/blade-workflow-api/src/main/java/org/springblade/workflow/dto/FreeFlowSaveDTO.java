package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 自由流程步骤整包保存请求。
 *
 * <p>服务端在一个事务内「删旧 + 建新 + 复制出口」整包替换，避免两人并发编辑时
 * 出现「A 删掉 B 刚插入的步骤」。{@code version} 与 {@code saveToken} 分别为
 * 乐观锁与幂等键（见落地分析 §7.7）。</p>
 *
 * <p>不引入 Bean Validation 注解：本模块与 {@code RejectDTO} 等既有 DTO 同口径，
 * 必填校验统一由 Service 入口显式判定并抛 {@code ServiceException}。</p>
 */
@Data
@Schema(description = "自由流程步骤整包保存请求")
public class FreeFlowSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程实例ID（必填，服务端校验）")
    private Long instId;

    @Schema(description = "步骤列表（按 flowOrder 重排后整包替换）")
    private List<FreeFlowStepDTO> steps;

    @Schema(description = "客户端持有的版本号（乐观锁；首次保存传 0 或 null）")
    private Integer version;

    @Schema(description = "保存幂等键（防重复提交 / 页面重放）")
    private String saveToken;

    @Schema(description = "另存为模板时的模板名；为空表示仅保存实例步骤")
    private String templateName;

}
