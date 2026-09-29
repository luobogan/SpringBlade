package org.springblade.workflow.service;

import org.springblade.workflow.vo.RedeployReportVO;

/**
 * 流程定义<b>批量重部署</b>（会签/或签/依次下沉多实例用）。
 *
 * <p><b>为什么需要它</b>：{@code multiInstanceLoopCharacteristics} 由
 * {@code WfDefinitionServiceImpl#deploy} 在<b>部署期</b>注入（{@code applyMultiInstanceIfEnabled}）。
 * 只把开关 {@code blade.workflow.engine-multi-instance.enabled} 改成 true，
 * <b>不会改写任何已部署的定义</b>——必须重新部署，新实例才会走引擎多实例（1:1）。</p>
 */
public interface IWfDefinitionRedeployService {

    /**
     * 批量重部署全部【已发布且有 BPMN】的流程定义。
     *
     * <p>逐条复用 {@link IWfDefinitionService#deploy(Long)}，保证与「单个发布」语义完全一致
     * （注入出口条件 → 归一化 → 注入多实例 → 部署 → 回写 deploymentId/procDefId → 激活版本）。</p>
     *
     * <p><b>在途实例不受影响</b>：Flowable 以创建时绑定的 {@code ACT_RE_PROCDEF.ID_} 隔离版本，
     * 重新部署只影响此后发起的新实例。</p>
     *
     * @param onlyMissingMi true=仅重部署「引擎侧尚未注入多实例」的定义（幂等、推荐）；
     *                      false=全量重部署（会产生新的引擎部署版本）
     * @param force         true=即使多实例开关未开启也执行重部署（仅做归一化部署，不会注入 MI）
     * @return 执行结果报告（失败明细逐条列出，单条失败不影响其它定义）
     */
    RedeployReportVO redeployAllForMultiInstance(boolean onlyMissingMi, boolean force);
}
