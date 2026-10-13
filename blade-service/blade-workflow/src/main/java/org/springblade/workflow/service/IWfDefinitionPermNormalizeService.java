package org.springblade.workflow.service;

import org.springblade.workflow.vo.PermNormalizeReportVO;

/**
 * 创建节点字段权限「归一」服务。
 *
 * <p>把创建节点（nodeType=0）BPMN {@code wf:node.fieldPerm / detailPerm} 中「只读」的条目
 * 归一到「可编辑」——创建节点由发起人填单，不应被节点字段权限收紧成只读
 * （否则表现为表单字段不可编辑、浏览/附件弹窗按钮不出现）。改 BPMN 源后按定义状态重新部署，
 * 让运行期（只读 BPMN）立即生效。</p>
 */
public interface IWfDefinitionPermNormalizeService {

    /**
     * 归一创建节点字段权限。
     *
     * @param targetDefId 指定则仅处理该定义；为空则处理全部有 BPMN 的定义
     * @return 执行报告（逐条成败 + 汇总）
     */
    PermNormalizeReportVO normalizeCreateNodeFieldPerm(Long targetDefId);
}
