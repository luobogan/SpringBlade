package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建节点字段权限「归一」结果报告。
 *
 * <p><b>背景</b>：「去 wf_表」回填把存量 {@code wf_node_field_perm} 原样搬进 BPMN
 * {@code wf:node.fieldPerm}；历史数据里创建节点（nodeType=0）可能带有 {@code perm=1/is_editable=0}
 * （只读）配置。运行期字段权限只读 BPMN（perm-from-bpmn + retirement），
 * 于是创建节点被误判为整表只读——「表单字段不可编辑、浏览/附件弹窗按钮不出现」。</p>
 *
 * <p>创建节点由发起人自行填单，本无「权限收紧」语义：本作业把创建节点 fieldPerm/detailPerm 中
 * 只读（可见、不可编辑、非必填）的条目归一到「可编辑」，改 BPMN 源并重新部署。</p>
 */
@Data
@Schema(description = "创建节点字段权限归一结果报告")
public class PermNormalizeReportVO {

    @Schema(description = "扫描的定义数（有 BPMN 的定义）")
    private int total;

    @Schema(description = "发生归一（至少一个字段只读→可编辑）的定义数")
    private int normalized;

    @Schema(description = "无需归一的定义数（创建节点无只读字段权限）")
    private int unchanged;

    @Schema(description = "已重新部署到引擎的定义数")
    private int deployed;

    @Schema(description = "仅归一源 BPMN、未部署的定义数（草稿等未发布态，发布后生效）")
    private int sourceOnly;

    @Schema(description = "归一的字段权限条目总数")
    private int fieldsNormalized;

    @Schema(description = "失败数")
    private int failed;

    @Schema(description = "失败明细（defId / procKey / 原因）")
    private List<String> failures = new ArrayList<>();

    @Schema(description = "结果说明")
    private String message;
}
