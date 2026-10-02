package org.springblade.formmode.service;

import java.util.Map;

/**
 * 审批触发服务接口
 *
 * 对应 ecology 的 ModeDataApproval
 */
public interface IApprovalTriggerService {

    /**
     * 触发审批流程
     *
     * @param modeId 模块ID
     * @param dataId 数据ID
     * @param action 触发操作（save/submit）
     * @param fieldValues 字段值
     * @return 是否成功触发
     */
    boolean triggerApproval(Long modeId, Long dataId, String action, Map<String, Object> fieldValues);

    /**
     * 跨库回填：把 mode_triggerworkflowset.workflowid（wf_process_definition.id）转换为 workflow_key（procKey）。
     *
     * <p>「以 Flowable 为唯一事实源」改造：mode_triggerworkflowset（blade 库）与 wf_process_definition
     * （blade_workflow 库）跨库，无法用单条 SQL JOIN 回填；逐行经 workflow Feign 取 (procKey,tenantId) 后本地更新。
     * 仅回填 workflow_key 为空的存量行；已回填的跳过。幂等。</p>
     *
     * @return 回填成功的行数
     */
    int backfillWorkflowKey();

}
