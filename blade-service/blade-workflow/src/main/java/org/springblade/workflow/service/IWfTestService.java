package org.springblade.workflow.service;

import org.springblade.workflow.dto.WfTestRunDTO;
import org.springblade.workflow.dto.WfTestStepDTO;
import org.springblade.workflow.entity.WfTestLog;
import org.springblade.workflow.vo.WfTaskVO;
import org.springblade.workflow.vo.WfTestResultVO;

import java.util.List;
import java.util.Map;

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
     * 「我的测试待办」（真人模式，方案 §6.4 **C12**）：当前登录人在测试实例上的待办
     * （{@code is_test=1} 且 {@code status=待办}）。
     *
     * <p>这是「用相应用户审批」的数据源 —— 节点操作者本人从测试入口登录后能看到并办理
     * 属于自己的测试单；而生产入口（待办/已办/角标/我的请求）对测试数据一律不可见（C1）。</p>
     */
    List<WfTaskVO> myTodo();

    /**
     * 真人办理测试待办（方案 §6.4 **C12**）：提交「当前用户作为执行人」的那条测试待办。
     *
     * <p>与 {@link #step} 的差别<b>只有鉴权</b>：本方法要求当前用户是该测试实例当前待办的
     * 执行人（管理员不受限，保留代跑）；提交逻辑完全复用 {@code step}
     * （同样按待办接收人身份办理、同样做节点必填校验、同样返回最新状态）。</p>
     */
    WfTestResultVO approve(WfTestStepDTO dto);

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
     * 影子比对（方案 §4.3 / §6「影子测试」落地项）：把同一套测试分别在<b>对照版本</b>与<b>目标版本</b>
     * 的定义上各跑一遍，比对流转路径、节点经过次数与覆盖率 —— 作为「新版本与旧版本行为是否等价」
     * 的上线依据（等价才放量，不等价先查差异）。
     *
     * <p>落地方式复用现有能力：临时部署 → 真实发起测试态实例 → 自动驱动到终态 → 采集节点/出口覆盖。
     * 跑完<b>只清理本次比对产生的测试实例</b>（不动该流程既有的测试历史），结论直接返回。</p>
     *
     * @param defId      目标（新）版本定义ID
     * @param baseDefId  对照（旧）版本定义ID；为空时自动取同 procKey 的上一版本
     * @param testUserId 测试发起人；为空时用目标定义的创建人
     * @return base/target 摘要 + 差异清单（{@code identical} / {@code diffs}）
     */
    Map<String, Object> shadowCompare(Long defId, Long baseDefId, Long testUserId);

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
