package org.springblade.workflow.service.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;

import java.util.List;

/**
 * 会签/或签/依次「引擎多实例 vs 自研计数」的统一灰度门（2026-11-17，flowtable 扩展安全垫）。
 *
 * <p>改造前两处 service 各自持有 {@code multiInstanceEnabled} 判定，口径易分叉；
 * 本组件把判定收敛到一处，供 {@code WfTaskServiceImpl#doApprove}（推进门禁）、
 * {@code WfInstanceServiceImpl#advanceInternal}（待办生成/对账）与两处 {@code injectMiCollectionVars}（变量注入）共用。</p>
 *
 * <p><b>灰度语义</b>：</p>
 * <ul>
 *   <li>全局开关 {@code blade.workflow.engine-multi-instance.enabled=false} → 永不走引擎（旧轨，最安全）；</li>
 *   <li>全局 true + 灰度白名单为空 → 全部走引擎（完全等同改造前行为）；</li>
 *   <li>全局 true + 灰度白名单非空 → 仅名单内 defId 走引擎，其余回退自研计数（逐流程灰度）。</li>
 * </ul>
 *
 * <p>节点类型约束沿用原口径：仅审批/提交类（nodeType ∈ {1,2}）走引擎多实例。</p>
 */
@Component
@RequiredArgsConstructor
public class WfEngineMiGate {

    /** 与新轨判定同源的全局开关；两个 service 内的同名字段仅保留兼容，不再作为判定源。 */
    @Value("${blade.workflow.engine-multi-instance.enabled:false}")
    private boolean multiInstanceEnabled;

    private final WfFlowtableCompatProperties compat;

    /**
     * 节点级判定：该节点运行期是否由引擎多实例驱动。
     *
     * @param inst    流程实例（提供 defId 与灰度判定）
     * @param node    节点（提供 nodeType）
     * @param nodeKey 节点Key（日志/兼容，可空）
     * @return true=走引擎 MI；false=走自研计数
     */
    public boolean useEngineMi(WfInstance inst, WfProcessNode node, String nodeKey) {
        // 与改造前 isEngineMultiInstance 的 null 守卫逐字节等价：inst 为 null 一律 false。
        if (inst == null) {
            return false;
        }
        if (!globalEnabled(inst)) {
            return false;
        }
        return node != null && node.getNodeType() != null
            && (Integer.valueOf(1).equals(node.getNodeType()) || Integer.valueOf(2).equals(node.getNodeType()));
    }

    /**
     * 实例级判定：不做节点类型检查，用于 {@code injectMiCollectionVars} 这类按 defId 决定是否注入集合变量的场景。
     *
     * <p>与节点级判定共用同一全局开关与灰度名单，避免「待办按灰度生成、MI 变量按全局注入」错配。</p>
     */
    public boolean useEngineMiForInstance(WfInstance inst) {
        return globalEnabled(inst);
    }

    /** 全局开关 + 定义灰度白名单（白名单空=全部放行，等同改造前）。 */
    private boolean globalEnabled(WfInstance inst) {
        if (!multiInstanceEnabled) {
            return false;
        }
        List<Long> gray = compat.getEngineMiGrayDefIds();
        if (gray == null || gray.isEmpty()) {
            return true;
        }
        return inst != null && inst.getDefId() != null && gray.contains(inst.getDefId());
    }
}
