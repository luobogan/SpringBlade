package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.entity.ModeInfo;
import org.springblade.formmode.entity.ModeTriggerWorkflowSet;
import org.springblade.formmode.entity.WorkflowBill;
import org.springblade.formmode.mapper.ModeInfoMapper;
import org.springblade.formmode.mapper.ModeTriggerWorkflowSetMapper;
import org.springblade.formmode.mapper.WorkflowBillMapper;
import org.springblade.formmode.utils.TableNameUtil;
import org.springblade.formmode.service.IApprovalTriggerService;
import org.springblade.workflow.dto.StartProcessDTO;
import org.springblade.workflow.feign.IWorkflowClient;
import org.springblade.workflow.vo.DefinitionKeyTenantVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
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
    private final WorkflowBillMapper workflowBillMapper;
    private final IWorkflowClient workflowClient;

    /**
     * 「以 Flowable 为唯一事实源」开关：开启后触发优先用 {@code workflow_key}（procKey）绑引擎身份，
     * 关闭时回退 {@code workflowid}（defId）。默认关，待任务 3（workflow 侧按 (procKey,tenant) 解析）完成后开启。
     */
    @Value("${blade.workflow.trigger-from-flowable.enabled:false}")
    private boolean triggerFromFlowableEnabled;

    @Override
    public boolean triggerApproval(Long modeId, Long dataId, String action, Map<String, Object> fieldValues) {
        var triggerSets = triggerWorkflowSetMapper.selectList(
            new LambdaQueryWrapper<ModeTriggerWorkflowSet>()
                .eq(ModeTriggerWorkflowSet::getModeid, modeId)
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
        // 单据/触发表发起：业务行由单据侧维护与流程的关联，发起后不回填 request_id
        dto.setBillInitiated(true);

        // 以 Flowable 为事实源（开关默认关）：优先用 workflow_key（procKey）绑引擎原生身份；
        // 开关未开、或 workflow_key 为空时回退 workflowid（wf_process_definition.id），完全兼容存量。
        String bindDesc;
        if (triggerFromFlowableEnabled
                && triggerSet.getWorkflowKey() != null && !triggerSet.getWorkflowKey().isBlank()) {
            dto.setProcKey(triggerSet.getWorkflowKey());
            if (triggerSet.getTenantId() != null && !triggerSet.getTenantId().isBlank()) {
                dto.setTenantId(triggerSet.getTenantId());
            }
            bindDesc = "procKey=" + triggerSet.getWorkflowKey();
        } else if (triggerSet.getWorkflowid() != null) {
            dto.setDefId(triggerSet.getWorkflowid().longValue());
            bindDesc = "defId=" + triggerSet.getWorkflowid();
        } else {
            log.error("[formmode] 触发配置缺少 workflowid/workflow_key，无法发起: triggerSetId={}, dataId={}",
                triggerSet.getId(), dataId);
            return;
        }

        try {
            // 实例ID按字符串返回（19 位雪花 ID 经 JSON 数字会丢精度）
            R<String> result = workflowClient.startProcess(dto);
            if (result == null || !result.isSuccess()) {
                // R-A（已撤回/停用）/ R-B（测试残留）等引擎侧明确错误经 Feign 返回失败，必须可见，不再静默吞掉
                log.error("[formmode] 发起审批流程失败[{}]: triggerSetId={}, dataId={}, msg={}",
                    bindDesc, triggerSet.getId(), dataId, result == null ? "无响应" : result.getMsg());
                return;
            }
            log.info("[formmode] 发起审批流程成功[{}]: instanceId={}, dataId={}", bindDesc, result.getData(), dataId);
        } catch (Exception e) {
            // 工作流服务不可用时由 Feign Fallback 降级；此处兜底记录，避免阻断表单保存主流程。
            // 仅记录、不抛异常（表单保存主流程不应被流程发起失败阻断）；bindDesc 便于事后定位是哪条触发配置。
            log.error("[formmode] 调用工作流服务异常[{}]: triggerSetId={}, dataId={}",
                bindDesc, triggerSet.getId(), dataId, e);
        }
    }

    /**
     * 跨库回填 workflow_key（「以 Flowable 为唯一事实源」改造的一次性迁移）。
     *
     * <p>mode_triggerworkflowset（blade 库）与 wf_process_definition（blade_workflow 库）跨库，
     * 逐行经 workflow Feign 取 (procKey,tenantId) 后本地更新；仅回填 workflow_key 为空者，幂等。</p>
     */
    @Override
    public int backfillWorkflowKey() {
        List<ModeTriggerWorkflowSet> rows = triggerWorkflowSetMapper.selectList(
            new LambdaQueryWrapper<ModeTriggerWorkflowSet>()
                .and(w -> w.isNull(ModeTriggerWorkflowSet::getWorkflowKey)
                    .or().eq(ModeTriggerWorkflowSet::getWorkflowKey, "")));
        int ok = 0;
        for (ModeTriggerWorkflowSet row : rows) {
            if (row.getWorkflowid() == null) {
                continue;
            }
            try {
                R<DefinitionKeyTenantVO> r = workflowClient.getDefinitionKeyAndTenant(row.getWorkflowid().longValue());
                DefinitionKeyTenantVO vo = (r != null && r.isSuccess()) ? r.getData() : null;
                if (vo == null || vo.getProcKey() == null) {
                    log.warn("[formmode] 回填跳过（定义不存在或无 procKey）: triggerSetId={}, workflowid={}",
                        row.getId(), row.getWorkflowid());
                    continue;
                }
                row.setWorkflowKey(vo.getProcKey());
                if (vo.getTenantId() != null && !vo.getTenantId().isBlank()) {
                    row.setTenantId(vo.getTenantId());
                }
                triggerWorkflowSetMapper.updateById(row);
                ok++;
            } catch (Exception e) {
                log.error("[formmode] 回填异常: triggerSetId={}, workflowid={}", row.getId(), row.getWorkflowid(), e);
            }
        }
        log.info("[formmode] workflow_key 回填完成: 待回填={}, 成功={}", rows.size(), ok);
        return ok;
    }

    /**
     * 由模块ID解析表单ID（modeinfo.billid → workflow_bill.id）
     *
     * <p>迁移表单语义：modeinfo.billid 只是物理表序号（formtable_main_{N}），
     * 真实表单ID以 workflow_bill.table_name 反查为准（与 saveByForm 同源）——
     * 流程实例的 formId 必须是 workflow_bill.id，审批页渲染包才取得到节点布局；
     * 反查不到再回退 billid（兼容未登记物理表的旧模块）。</p>
     */
    private Long resolveFormId(Long modeId) {
        if (modeId == null) {
            return null;
        }
        ModeInfo modeInfo = modeInfoMapper.selectById(modeId);
        if (modeInfo == null || modeInfo.getBillid() == null) {
            return null;
        }
        try {
            WorkflowBill bill = workflowBillMapper.selectOne(
                Wrappers.<WorkflowBill>lambdaQuery()
                    .eq(WorkflowBill::getTableName,
                        TableNameUtil.getMainTableName(modeInfo.getBillid().longValue()))
                    .last("LIMIT 1"));
            if (bill != null && bill.getId() != null) {
                return bill.getId();
            }
        } catch (Exception e) {
            log.warn("[formmode] 经 table_name 反查表单ID失败，回退 billid. modeId={}", modeId, e);
        }
        return modeInfo.getBillid().longValue();
    }

}
