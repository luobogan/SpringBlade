package org.springblade.workflow.service;

import org.springblade.workflow.dto.WfTestRunDTO;
import org.springblade.workflow.dto.WfTestStepDTO;
import org.springblade.workflow.entity.WfTestLog;
import org.springblade.workflow.vo.WfTaskVO;
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
     * 发起一次「交互式测试」：临时部署草稿流程并真实发起测试态实例，<b>不自动驱动</b>。
     *
     * <p>发起后实例停留在创建节点等待办理，前端可：</p>
     * <ul>
     *   <li>「开始自动测试」→ 循环调用 {@link #step} 逐节点推进（可随时暂停）；</li>
     *   <li>「手动测试」→ 填写表单后调用 {@link #step} 单步提交。</li>
     * </ul>
     *
     * @param dto 入参（流程定义ID + 测试发起人 + 可选表单字段值）
     * @return 发起后的测试状态（含 instId / 当前节点 / 节点经过次数）；预校验未过时 instId 为 null
     */
    WfTestResultVO start(WfTestRunDTO dto);

    /**
     * 查询交互式测试的当前状态（节点经过次数 / 出口覆盖 / 当前节点 / 待办 / 逐行日志）。
     *
     * @param instId 测试态实例ID
     */
    WfTestResultVO state(Long instId);

    /**
     * 交互式测试-单步推进：提交「当前节点」的待办，推进到下一节点。
     *
     * <p>「自动测试」与「手动测试」共用本方法：前者由前端循环调用，后者由用户点「提交」调用。
     * 推进前按节点必填矩阵校验（未填则抛 {@code ServiceException}），并把表单值作为流程变量下发引擎。</p>
     *
     * @param dto 入参（实例ID + 意见 + 可选表单值）
     * @return 推进后的测试状态
     */
    WfTestResultVO step(WfTestStepDTO dto);

    /**
     * 交互式测试-查询测试态实例的待办（供手动办理定位 taskId / 判断当前节点是否有待办）。
     *
     * @param instId 测试态实例ID
     */
    List<WfTaskVO> todo(Long instId);

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
