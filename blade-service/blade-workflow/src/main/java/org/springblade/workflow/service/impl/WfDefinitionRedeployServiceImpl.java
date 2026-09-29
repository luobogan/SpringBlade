package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.service.IWfDefinitionRedeployService;
import org.springblade.workflow.service.IWfDefinitionService;
import org.springblade.workflow.vo.RedeployReportVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 流程定义批量重部署实现（会签/或签下沉多实例用）。
 *
 * <p><b>为什么单独成类</b>：批量逻辑若在 {@code WfDefinitionServiceImpl} 内部自调用
 * {@code deploy(defId)}，会<b>绕过 Spring 代理</b>导致 {@code deploy} 上的
 * {@code @Transactional} 失效（多步写库失去事务保护）。故单独成类，注入
 * {@link IWfDefinitionService} 经代理调用，每条独立事务：单条失败只回滚自己，不影响其它定义。</p>
 *
 * <p><b>本方法自身不加 {@code @Transactional}</b>：让每条 {@code deploy} 各自提交，
 * 避免「一条失败把前面成功的全回滚」。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfDefinitionRedeployServiceImpl implements IWfDefinitionRedeployService {

    private final WfProcessDefinitionMapper defMapper;
    /** 经 Spring 代理调用，保证 deploy() 的 @Transactional 逐条生效 */
    private final IWfDefinitionService definitionService;
    /** 判定「引擎侧是否已注入多实例」（读已部署的 BPMN 模型） */
    private final RepositoryService repositoryService;

    @Value("${blade.workflow.engine-multi-instance.enabled:false}")
    private boolean multiInstanceEnabled;

    @Override
    public RedeployReportVO redeployAllForMultiInstance(boolean onlyMissingMi, boolean force) {
        RedeployReportVO report = new RedeployReportVO();
        report.setMiEnabled(multiInstanceEnabled);

        // 安全闸门：开关未开时重部署毫无意义（不会注入 MI），除非显式 force
        if (!multiInstanceEnabled && !force) {
            report.setMessage("多实例总开关 blade.workflow.engine-multi-instance.enabled 未开启，"
                + "重部署不会注入多实例，本次未执行。如仅需做归一化重部署，请显式传 force=true。");
            log.warn("[blade-workflow] 批量重部署未执行：多实例开关未开启（force=false）");
            return report;
        }

        // 只重部署【已发布(status=1)且有 BPMN】的定义：避免把草稿(0)/测试(3)版本误发布
        List<WfProcessDefinition> defs = defMapper.selectList(Wrappers.<WfProcessDefinition>lambdaQuery()
            .eq(WfProcessDefinition::getStatus, 1)
            .isNotNull(WfProcessDefinition::getBpmnXml)
            .ne(WfProcessDefinition::getBpmnXml, ""));
        report.setTotal(defs.size());

        for (WfProcessDefinition def : defs) {
            if (onlyMissingMi && alreadyHasMultiInstance(def)) {
                report.setSkipped(report.getSkipped() + 1);
                continue;
            }
            report.setAttempted(report.getAttempted() + 1);
            try {
                definitionService.deploy(def.getId());
                report.setSuccess(report.getSuccess() + 1);
            } catch (Exception e) {
                report.setFailed(report.getFailed() + 1);
                report.getFailures().add("defId=" + def.getId() + ", procKey=" + def.getProcKey()
                    + " : " + e.getMessage());
                log.warn("[blade-workflow] 批量重部署-单条失败. defId={}, procKey={}, {}",
                    def.getId(), def.getProcKey(), e.getMessage());
            }
        }

        report.setMessage("批量重部署完成：命中 " + report.getTotal()
            + "，尝试 " + report.getAttempted()
            + "，成功 " + report.getSuccess()
            + "，失败 " + report.getFailed()
            + "，跳过(已注入MI) " + report.getSkipped()
            + (multiInstanceEnabled ? "；多实例开关已开启，本次已注入 MI"
                                    : "；⚠ 多实例开关未开启，本次未注入 MI"));
        log.info("[blade-workflow] {}", report.getMessage());
        return report;
    }

    /**
     * 引擎侧【已部署】的 BPMN 是否已有任一 UserTask 带多实例。
     *
     * <p>注意：MI 只注入到提交引擎的 {@code deployXml}，<b>不会写回</b> {@code wf_process_definition.bpmn_xml}（存储的
     * 源 BPMN 永远没有 MI）。因此只能查引擎侧模型，不能以存储 XML 判断。</p>
     */
    private boolean alreadyHasMultiInstance(WfProcessDefinition def) {
        String procDefId = def.getProcDefId();
        if (procDefId == null || procDefId.isBlank()) {
            if (def.getProcKey() == null || def.getProcKey().isBlank()) {
                return false;
            }
            try {
                ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(def.getProcKey())
                    .latestVersion()
                    .singleResult();
                procDefId = (pd == null) ? null : pd.getId();
            } catch (Exception ignore) {
                return false;
            }
        }
        if (procDefId == null) {
            return false;
        }
        try {
            BpmnModel model = repositoryService.getBpmnModel(procDefId);
            if (model == null || model.getMainProcess() == null) {
                return false;
            }
            for (UserTask t : model.getMainProcess().findFlowElementsOfType(UserTask.class, true)) {
                if (t.getLoopCharacteristics() != null) {
                    return true;
                }
            }
        } catch (Exception ignore) {
            return false;
        }
        return false;
    }
}
