package org.springblade.formmode.service.impl;

import lombok.RequiredArgsConstructor;
import org.springblade.formmode.entity.ModeTriggerWorkflowSet;
import org.springblade.formmode.mapper.ModeTriggerWorkflowSetMapper;
import org.springblade.formmode.service.IApprovalTriggerService;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 审批触发服务实现
 *
 * 对应 ecology 的 ModeDataApproval 核心逻辑
 */
@Service
@RequiredArgsConstructor
public class ApprovalTriggerServiceImpl implements IApprovalTriggerService {

    private final ModeTriggerWorkflowSetMapper triggerWorkflowSetMapper;

    @Override
    public boolean triggerApproval(Long modeId, Long dataId, String action, Map<String, Object> fieldValues) {
        // 查询该模块的触发工作流设置
        var triggerSets = triggerWorkflowSetMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ModeTriggerWorkflowSet>()
                .eq(ModeTriggerWorkflowSet::getModeid, modeId.intValue())
                .eq(ModeTriggerWorkflowSet::getStatus, 1)
        );

        if (triggerSets.isEmpty()) {
            return false;
        }

        for (ModeTriggerWorkflowSet triggerSet : triggerSets) {
            // 判断触发操作类型
            if (triggerSet.getTriggeropt() != null) {
                boolean shouldTrigger = switch (triggerSet.getTriggeropt()) {
                    case 0 -> "save".equals(action);         // 新建
                    case 1 -> "update".equals(action);       // 编辑
                    case 2 -> "del".equals(action);          // 删除
                    case 3 -> true;                          // 全部
                    default -> false;
                };

                if (shouldTrigger) {
                    // TODO: 通过 Feign 调用工作流服务创建审批实例
                    // IWorkflowClient.createWorkflowRequest(triggerSet.getWorkflowid(), dataId, fieldValues);
                    // 暂未对接工作流服务，记录日志
                    org.slf4j.LoggerFactory.getLogger(getClass())
                        .info("触发审批: modeId={}, dataId={}, workflowId={}",
                            modeId, dataId, triggerSet.getWorkflowid());
                }
            }
        }
        return true;
    }

}
