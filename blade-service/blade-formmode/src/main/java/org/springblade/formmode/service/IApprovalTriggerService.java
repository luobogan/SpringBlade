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

}
