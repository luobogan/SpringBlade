package org.springblade.workflow.service;

import org.springblade.workflow.dto.WfTestRunDTO;
import org.springblade.workflow.entity.WfTestLog;
import org.springblade.workflow.vo.WfTestResultVO;

import java.util.List;

/**
 * 流程测试服务（设计期校验）
 *
 * <p><b>实现方式：真实引擎 + 真实表单 + 测试态标记</b>（对齐 ecology
 * {@code workflow_requestbase.deleted=1}）：草稿流程临时部署到 Flowable 真实发起，
 * 实例/待办打 {@code is_test=1}，逐节点系统自动通过跑到归档，基于历史活动算覆盖率；
 * 测试数据可经 {@link #cleanupTestData} 一键清理，不污染正常流程数据。</p>
 */
public interface IWfTestService {

    /**
     * 运行一次流程测试（真实引擎）
     *
     * @param dto 入参（流程定义ID + 测试发起人 + 可选表单字段值 formData）
     * @return 测试结果（节点经过次数 / 路径 / 逐行日志 / 结论）
     */
    WfTestResultVO run(WfTestRunDTO dto);

    /**
     * 清理测试数据：删除所有 {@code is_test=1} 的实例/待办/日志/快照，并级联卸载对应的测试部署。
     *
     * @param defId 流程定义ID；为空时清理全部测试数据
     * @return 清理的实例数量
     */
    long cleanupTestData(Long defId);

    /**
     * 测试历史列表
     *
     * @param defId 流程定义ID；为空时查全部
     * @return 最近的测试记录（倒序，最多 100 条）
     */
    List<WfTestLog> list(Long defId);

    /**
     * 测试详情（含日志正文与结构化结果）
     */
    WfTestLog detail(Long id);

    /**
     * 删除测试记录
     *
     * @param ids 主键列表
     */
    boolean remove(List<Long> ids);
}
