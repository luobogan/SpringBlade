package org.springblade.workflow.service;

import org.springblade.workflow.dto.StartProcessDTO;
import org.springblade.workflow.vo.ApprovalLogVO;
import org.springblade.workflow.vo.InstanceVO;

import java.util.List;

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
     * 实例详情
     */
    InstanceVO detail(Long id);

    /**
     * 按业务数据反查实例
     */
    InstanceVO getByBiz(Long formId, Long dataId);

    /**
     * 流转/审批记录
     */
    List<ApprovalLogVO> logs(Long instId);

    /**
     * 取指定节点的表单数据快照
     */
    String snapshot(Long instId, String nodeKey);

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

}
