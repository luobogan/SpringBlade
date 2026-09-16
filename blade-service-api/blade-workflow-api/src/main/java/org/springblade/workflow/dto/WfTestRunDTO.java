package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 流程测试运行入参
 *
 * <p>指定一个「测试发起人」，从创建节点出发按出口条件走查路径，逐节点解析操作者，
 * 产出测试日志与结论。</p>
 */
@Data
@Schema(description = "流程测试运行入参")
public class WfTestRunDTO {

    @Schema(description = "流程定义ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long defId;

    @Schema(description = "测试发起人用户ID（须具备该流程的创建权限）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long testUserId;

    @Schema(description = "测试发起人姓名（冗余展示用）")
    private String testUserName;

    /**
     * 模拟表单数据：用于出口条件求值与「字段-人员」类操作者解析。
     *
     * <p>为空时按<b>全量走查</b>（忽略条件、遍历所有分支），用于验证连通性与死路。</p>
     */
    @Schema(description = "模拟表单数据（fieldName -> value）；为空则按全量走查")
    private Map<String, Object> formData;
}
