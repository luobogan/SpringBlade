package org.springblade.workflow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.workflow.dto.FormSaveDTO;
import org.springblade.workflow.dto.StartProcessDTO;
import org.springblade.workflow.enums.AdvanceSrc;
import org.springblade.workflow.vo.ApprovalLogVO;
import org.springblade.workflow.vo.InstanceFreshVO;
import org.springblade.workflow.vo.InstanceVO;
import org.springblade.workflow.vo.WfNodeOperatorVO;

import java.util.List;
import java.util.Map;

/**
 * 流程实例语义服务
 *
 * <p>负责 wf_instance / wf_form_snapshot / wf_approval_log 等语义层数据，
 * 引擎调用经由 {@link IProcessService} 适配，二者解耦。</p>
 */
public interface IWfInstanceService {

    /**
     * 发起流程：落库 wf_instance → 写表单快照 → 记录提交日志 → 生成首个待办
     *
     * @return 流程实例ID（wf_instance.id）
     */
    Long start(StartProcessDTO dto);

    /**
     * 保存草稿（只存不流转）：创建/更新草稿实例（status=草稿）与发起人待办任务，
     * 并写业务数据行与表单快照；返回草稿实例ID。再次保存同一草稿时复用实例与业务行。
     *
     * @return 草稿实例ID（wf_instance.id）
     */
    Long saveDraft(FormSaveDTO dto);

    /**
     * 删除草稿（只删草稿）：删除草稿实例及其合成待办、表单快照、流转记录，并清理其独占的业务数据行。
     *
     * <p>仅<b>草稿态</b>（status=5）实例可删；仅<b>发起人本人</b>可删（草稿不进入他人待办/可见范围）。
     * 已发起（运行中/通过/终止…）的实例不可走此接口，须用撤回/终止/作废等语义动作。</p>
     *
     * @param id 草稿实例ID
     * @return 是否删除成功（实例不存在返回 false）
     */
    boolean deleteDraft(Long id);

    /**
     * 实例详情
     */
    InstanceVO detail(Long id);

    /**
     * 我的请求：我发起的流程实例分页（「我的请求」页签数据源）。
     *
     * <p>按「发起人 = 当前登录人」过滤，普通用户看不到别人的申请；
     * 传入当前页与页大小，返回 MyBatis-Plus 分页对象（前端 ProTable 直接消费）。</p>
     *
     * @param current  当前页（从 1 开始，空则 1）
     * @param pageSize 每页条数（空则 20，上限 200，防大页拖库）
     * @param title    流程标题（模糊匹配，可空）
     */
    IPage<InstanceVO> mine(Long current, Long pageSize, String title);

    /**
     * 按业务数据反查实例
     */
    InstanceVO getByBiz(Long formId, Long dataId);

    /**
     * 统计某表单下已产生的流程实例数量
     *
     * @param formId 表单ID（workflow_bill.id）
     * @return 实例数量（逻辑未删除）
     */
    int countByForm(Long formId);

    /**
     * 当前登录用户能否查看该实例（记录级鉴权的唯一口径）。
     *
     * <p>规则：流程管理员、发起人本人、或在该实例上有任务记录的人（办理人 / 被抄送 /
     * 被传阅）。用于渲染包这类<b>按实例组装数据</b>的接口做越权拦截，
     * 避免「登录即可」的接口被拿去猜 id 读别人的单据。</p>
     *
     * @param instId 实例ID（不存在返回 false）
     */
    boolean canView(Long instId);

    /**
     * 流转/审批记录
     */
    List<ApprovalLogVO> logs(Long instId);

    /**
     * 节点操作者情况（流程图节点悬浮「操作者」面板）。
     *
     * <p>按 nodeKey 返回该节点的「已操作 / 已查看 / 未操作」人员ID分组，
     * 供流程图节点悬浮面板与节点下方「谁办了」展示。</p>
     *
     * @param instId 流程实例ID
     * @return nodeKey → 分组人员ID（没有办理记录的节点不会出现在结果里）
     */
    Map<String, WfNodeOperatorVO> nodeOperators(Long instId);

    /**
     * 取指定节点的表单数据快照
     */
    String snapshot(Long instId, String nodeKey);

    /**
     * 界面新鲜度复检：判定「浏览器里已打开的页面」是否仍与实例真实状态一致。
     *
     * <p>用于识别并拦截「流程已回退 / 被他人流转 / 已归档撤回，但界面仍显示原节点」
     * 造成的状态不一致：前端在页面打开、每次保存/提交等写操作前、以及定时与切回标签页时
     * 调用本方法，{@code stale=true} 时必须拦截操作并提示用户刷新。</p>
     *
     * <p>{@code nodeActive} 采用「并行分支安全」口径（见 {@code InstanceFreshVO} 注释），
     * 不会把并行网关另一条分支上的合法办理误判为过期。</p>
     *
     * @param instId  流程实例ID
     * @param nodeKey 界面当前展示/操作的节点Key（可为空：为空时按 taskId 或实例当前节点推定）
     * @param taskId  界面持有的任务ID（可为空：只读查看页无任务）
     * @return 复检结果（含 stale 与已成人话的 staleReason）
     */
    InstanceFreshVO fresh(Long instId, String nodeKey, Long taskId);

    /**
     * 撤回（发起人收回）
     */
    boolean withdraw(Long instId, String opinion);

    /**
     * 暂停
     */
    boolean stop(Long instId);

    /**
     * 恢复
     */
    boolean resume(Long instId);

    /**
     * 撤销（作废）
     */
    boolean cancel(Long instId, String opinion);

    /**
     * 推进实例：按引擎当前活动任务同步 wf_task，并更新当前节点；
     * 若引擎已无活动任务，则置实例为通过并写结束时间。
     * <p>供任务服务在完成/退回后调用。</p>
     */
    void advance(Long instId);

    /**
     * 推进实例（带「到达本节点的当前办理人」，用于解析「本部门(19)」类操作者）。
     *
     * @param instId          实例ID
     * @param currentOperator 到达本节点的当前办理人（首节点一般为发起人；后续节点为上一节点完成人）
     */
    void advance(Long instId, Long currentOperator);

    /**
     * 推进实例（带「指定流转」的节点/操作者覆盖）。
     *
     * <p>当处理人手工指定了下一节点时，该节点的操作者以 {@code overrideAssignee} 为准（模式1），
     * 其余节点仍按「节点操作者」正常解析。</p>
     *
     * @param instId           实例ID
     * @param currentOperator  到达本节点的当前办理人
     * @param overrideNodeKey  指定流转的目标节点Key（仅该节点的操作者被覆盖；null 表示不覆盖）
     * @param overrideAssignee 指定流转的目标操作者（null 表示按节点设置解析）
     */
    void advance(Long instId, Long currentOperator, String overrideNodeKey, Long overrideAssignee);

    /**
     * 推进实例（带「推进来源」标识）。
     *
     * <p>来源用于让运行时按链路施加不同策略。当前唯一消费方是「流程异常处理」
     * （{@code settings.exceptionHandle}）：该兜底只在<b>提交</b>链路生效，
     * <b>退回</b>链路不生效（对齐 ecology「退回忽略异常处理设置」）。</p>
     *
     * <p>其余重载（1/2/4 参）等价于传入 {@link AdvanceSrc#SUBMIT}，行为不变。</p>
     *
     * @param instId           实例ID
     * @param currentOperator  到达本节点的当前办理人
     * @param overrideNodeKey  指定流转的目标节点Key（null 表示不覆盖）
     * @param overrideAssignee 指定流转的目标操作者（null 表示按节点设置解析）
     * @param src              推进来源（null 视为 {@link AdvanceSrc#SUBMIT}）
     */
    void advance(Long instId, Long currentOperator, String overrideNodeKey, Long overrideAssignee, AdvanceSrc src);

    /**
     * 推进实例（「指定流转·多目标」专用：按节点 Key 分别指定操作者）。
     *
     * @param instId            实例ID
     * @param currentOperator   到达本节点的当前办理人
     * @param overrideAssignees 节点Key → 指定操作者ID（仅对该节点的待办生效；未列出的节点按自身设置解析）
     * @param src               推进来源（null 视为 {@link AdvanceSrc#SUBMIT}）
     */
    void advance(Long instId, Long currentOperator, Map<String, Long> overrideAssignees, AdvanceSrc src);

    /**
     * 记录一条流转日志（供子流程服务等内部模块复用，避免重复私有方法）。
     *
     * @param instId  实例ID
     * @param nodeKey 节点Key（可空）
     * @param operator 操作人
     * @param logType 流转动作（WfApprovalLog.LOG_*）
     * @param opinion 意见/说明
     */
    void recordLog(Long instId, String nodeKey, Long operator, String logType, String opinion);

}
