package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.entity.ModeInfo;
import org.springblade.formmode.entity.ModeTriggerWorkflowSet;
import org.springblade.formmode.mapper.ModeInfoMapper;
import org.springblade.formmode.mapper.ModeTriggerWorkflowSetMapper;
import org.springblade.formmode.service.IApprovalTriggerService;
import org.springblade.workflow.dto.StartProcessDTO;
import org.springblade.workflow.feign.IWorkflowClient;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 审批触发服务实现
 *
 * <p>对应 ecology 的 ModeDataApproval 核心逻辑。</p>
 * <p><b>接线说明</b>：复用已有触发表 {@code mode_triggerworkflowset}（不另建触发配置表），
 * 命中触发条件后经 Feign 调用 {@code blade-workflow} 的
 * {@code POST /instance/start} 真正发起流程实例，
 * 替代此前的 TODO + 打日志。Feign 服务名为统一后的 {@code blade-workflow}（常量
 * {@code WorkflowConstant.APPLICATION_WORKFLOW_NAME}），不再硬编码 blade-flow。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalTriggerServiceImpl implements IApprovalTriggerService {

    private final ModeTriggerWorkflowSetMapper triggerWorkflowSetMapper;
    private final ModeInfoMapper modeInfoMapper;
    private final IWorkflowClient workflowClient;

    @Override
    public boolean triggerApproval(Long modeId, Long dataId, String action, Map<String, Object> fieldValues) {
        var triggerSets = triggerWorkflowSetMapper.selectList(
            new LambdaQueryWrapper<ModeTriggerWorkflowSet>()
                .eq(ModeTriggerWorkflowSet::getModeid, modeId.intValue())
                .eq(ModeTriggerWorkflowSet::getStatus, 1)
        );

        if (triggerSets.isEmpty()) {
            return false;
        }

        Long formId = resolveFormId(modeId);

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
                    startWorkflowInstance(triggerSet, formId, dataId, fieldValues);
                }
            }
        }
        return true;
    }

    /**
     * 调用工作流服务发起流程实例
     */
    private void startWorkflowInstance(ModeTriggerWorkflowSet triggerSet, Long formId,
                                       Long dataId, Map<String, Object> fieldValues) {
        StartProcessDTO dto = new StartProcessDTO();
        dto.setFormId(formId);
        dto.setDataId(dataId);
        dto.setFieldValues(fieldValues);
        dto.setStarter(SecureUtil.getUserId());
        // 触发表中的 workflowid 对应新流程定义ID（wf_process_definition.id）；
        // 存量 ecology 流程经 wf_migration_map 映射后同样落到这里。
        if (triggerSet.getWorkflowid() != null) {
            dto.setDefId(triggerSet.getWorkflowid().longValue());
        }

        try {
            R<Long> result = workflowClient.startProcess(dto);
            if (result == null || !result.isSuccess()) {
                log.error("[formmode] 发起审批流程失败: modeWorkflowId={}, dataId={}, msg={}",
                    triggerSet.getWorkflowid(), dataId, result == null ? "无响应" : result.getMsg());
                return;
            }
            log.info("[formmode] 发起审批流程成功: instanceId={}, dataId={}", result.getData(), dataId);
        } catch (Exception e) {
            // 工作流服务不可用时由 Feign Fallback 降级；此处兜底记录，避免阻断表单保存主流程
            log.error("[formmode] 调用工作流服务异常: workflowId={}, dataId={}",
                triggerSet.getWorkflowid(), dataId, e);
        }
    }

    /**
     * 由模块ID解析表单ID（modeinfo.billid → workflow_bill.id）
     */
    private Long resolveFormId(Long modeId) {
        if (modeId == null) {
            return null;
        }
        ModeInfo modeInfo = modeInfoMapper.selectById(modeId);
        if (modeInfo == null || modeInfo.getBillid() == null) {
            return null;
        }
        return modeInfo.getBillid().longValue();
    }

}
