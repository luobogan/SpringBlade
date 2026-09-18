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

    /**
     * 是否开启「分支覆盖」：按排他网关各分支的条件<b>反推变量取值</b>，为每个分支额外跑一次
     * 真实引擎实例，合并覆盖率。
     *
     * <p>不开启时一组表单数据只能走通网关的一条分支，其余分支节点会被判「未走到」，
     * 属于假阴性；开启后「每条线」都有机会被真实走到。</p>
     */
    @Schema(description = "是否按网关分支枚举多场景以覆盖到每条出口")
    private Boolean coverBranches;
}
