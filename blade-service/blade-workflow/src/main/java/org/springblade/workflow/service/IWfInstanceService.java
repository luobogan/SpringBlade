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

}
