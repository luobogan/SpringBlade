package org.springblade.workflow.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 流程测试结果
 *
 * <p>对齐 ecology 流程测试的产物：逐节点经过次数 + 流转路径 + 逐行测试日志 + 结论。</p>
 */
@Data
@Schema(description = "流程测试结果")
public class WfTestResultVO {

    /** 字符串序列化：19 位雪花 ID 在前端 JS 解析会丢精度 */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "测试日志ID（wf_test_log.id）")
    private Long logId;

    @Schema(description = "测试结论 0未通过 1通过 2异常中断")
    private Integer testStatus;

    @Schema(description = "是否走到归档节点")
    private Boolean reachedEnd;

    @Schema(description = "参与校验节点数")
    private Integer nodeTotal;

    @Schema(description = "走通节点数")
    private Integer nodePassed;

    @Schema(description = "耗时（毫秒）")
    private Long costMs;

    @Schema(description = "结论摘要")
    private String summary;

    @Schema(description = "节点经过次数：nodeKey -> 次数")
    private Map<String, Integer> nodeTimes;

    @Schema(description = "逐节点测试结果")
    private List<TestNodeVO> nodes;

    @Schema(description = "实际走到的流转路径（按经过顺序）")
    private List<TestStepVO> path;

    @Schema(description = "测试日志（逐行）")
    private List<String> log;

    @Schema(description = "出口（连线）总数")
    private Integer linkTotal;

    @Schema(description = "被真实走过的出口数")
    private Integer linkPassed;

    @Schema(description = "逐条出口的覆盖情况（是否真被引擎走到）")
    private List<TestLinkVO> links;

    @Schema(description = "本次实际执行的场景数（开启分支覆盖时 >1）")
    private Integer scenarioCount;

    @Schema(description = "每个场景的执行概要")
    private List<TestScenarioVO> scenarios;

    @Data
    @Schema(description = "单个节点的测试结果")
    public static class TestNodeVO {
        @Schema(description = "节点Key")
        private String nodeKey;
        @Schema(description = "节点名称")
        private String nodeName;
        @Schema(description = "节点类型 0创建 1审批 2提交 3归档 5等待 6自动处理")
        private Integer nodeType;
        @Schema(description = "经过次数")
        private Integer passTimes;
        @Schema(description = "结果 0未走到 1走通 2走不通")
        private Integer status;
        @Schema(description = "说明（走不通时指出原因）")
        private String message;
        @Schema(description = "该节点解析出的操作者")
        private List<TestOperatorVO> operators;
    }

    @Data
    @Schema(description = "节点操作者")
    public static class TestOperatorVO {
        @Schema(description = "用户ID")
        private Long userId;
        @Schema(description = "姓名")
        private String userName;
        @Schema(description = "来源描述（如「人员」「部门」）")
        private String source;
    }

    @Data
    @Schema(description = "流转路径中的一段")
    public static class TestStepVO {
        @Schema(description = "源节点Key")
        private String fromNodeKey;
        @Schema(description = "目标节点Key")
        private String toNodeKey;
        @Schema(description = "所走出口条件中文（无则为空）")
        private String conditionCn;
    }

    @Data
    @Schema(description = "单条出口（连线）的覆盖情况")
    public static class TestLinkVO {
        @Schema(description = "源节点Key")
        private String fromNodeKey;
        @Schema(description = "目标节点Key")
        private String toNodeKey;
        @Schema(description = "源节点名称")
        private String fromNodeName;
        @Schema(description = "目标节点名称")
        private String toNodeName;
        @Schema(description = "出口条件表达式")
        private String conditionExpr;
        @Schema(description = "出口条件中文")
        private String conditionCn;
        @Schema(description = "被引擎真实走过的次数")
        private Integer passTimes;
        @Schema(description = "结果 1走过 0未走过")
        private Integer status;
    }

    @Data
    @Schema(description = "单个测试场景的执行概要")
    public static class TestScenarioVO {
        @Schema(description = "场景序号（从 1 开始）")
        private Integer index;
        @Schema(description = "场景说明（如「分支：金额>1000」）")
        private String label;
        @Schema(description = "该场景使用的表单变量")
        private Map<String, Object> formData;
        @Schema(description = "该场景结论 0未通过 1通过 2中断")
        private Integer testStatus;
        @Schema(description = "是否走到归档")
        private Boolean reachedEnd;
        @Schema(description = "该场景实际经过的节点序列")
        private List<String> path;
        @Schema(description = "摘要")
        private String summary;
    }
}
