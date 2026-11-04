package org.springblade.workflow.freeflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.WfFreeNode;
import org.springblade.workflow.entity.WfProcessNode;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 影子节点装饰器：把「预置池静态节点」包装成本次实例该步骤的 {@link WfProcessNode}。
 *
 * <p>这是整套方案的关键：{@code wf_task.node_key} 恒为已部署 BPMN 里真实存在的
 * 静态 activityId（如 {@code Free_3}），{@code loadNode(defId,nodeKey)} 天然命中，
 * 因此公文四钩子、字段权限、操作菜单、超时规则等以 {@code def_id+node_key} 为索引的
 * 既有链路不会因「运行期动态 key」而静默失效（见落地分析 §10.1）。</p>
 *
 * <p>装饰只作用于传入的副本，绝不修改设计期节点对象（可能被缓存复用）。
 * {@code extJson} 采用<b>按键合并</b>而非整体覆盖：否则步骤自带 extJson 会冲掉静态节点上的
 * {@code operateMenu / signOpinion / printSet / opinionDisplay / preOperate / postOperate}。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfFreeNodeResolver {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    /**
     * 用实例步骤装饰静态节点。
     *
     * @param context 请求级步骤快照；{@link WfFreeNodeContext#empty()} 时原样返回
     * @param base    设计期静态节点（可为 null）
     * @param nodeKey 当前节点 key
     * @return 装饰后的节点副本；无对应步骤或 base 为 null 时原样返回 base
     */
    public WfProcessNode decorate(WfFreeNodeContext context, WfProcessNode base, String nodeKey) {
        if (base == null || context == null || context.isEmpty()) {
            return base;
        }
        WfFreeNode step = context.byNodeKey(nodeKey);
        if (step == null) {
            return base;
        }
        WfProcessNode copy = new WfProcessNode();
        BeanUtils.copyProperties(base, copy);
        if (step.getNodeName() != null && !step.getNodeName().isBlank()) {
            copy.setNodeName(step.getNodeName());
        }
        if (step.getSignType() != null) {
            copy.setSignOrder(step.getSignType());
        }
        copy.setExtJson(mergeExtJson(base.getExtJson(), step.getExtJson()));
        return copy;
    }

    /** 当前步骤是否为定稿取号点（替代按静态 finalizeNodeKey 判定） */
    public boolean isFinalize(WfFreeNodeContext context, String nodeKey) {
        WfFreeNode step = context == null ? null : context.byNodeKey(nodeKey);
        return step != null && Integer.valueOf(1).equals(step.getIsFinalize());
    }

    /** 当前步骤是否为套红必做点 */
    public boolean mustTaohong(WfFreeNodeContext context, String nodeKey) {
        WfFreeNode step = context == null ? null : context.byNodeKey(nodeKey);
        return step != null && Integer.valueOf(1).equals(step.getMustTaohong());
    }

    /** 静态配置 + 步骤覆盖，settings 子对象按键合并，避免整体覆盖导致按钮/意见配置丢失 */
    private String mergeExtJson(String baseJson, String stepJson) {
        if (stepJson == null || stepJson.isBlank()) {
            return baseJson;
        }
        if (baseJson == null || baseJson.isBlank()) {
            return stepJson;
        }
        try {
            Map<String, Object> base = MAPPER.readValue(baseJson, MAP_TYPE);
            Map<String, Object> step = MAPPER.readValue(stepJson, MAP_TYPE);
            Object baseSettings = base.get("settings");
            Object stepSettings = step.get("settings");
            if (baseSettings instanceof Map && stepSettings instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> bs = (Map<String, Object>) baseSettings;
                @SuppressWarnings("unchecked")
                Map<String, Object> ss = (Map<String, Object>) stepSettings;
                bs.putAll(ss);
                step.remove("settings");
            }
            base.putAll(step);
            return MAPPER.writeValueAsString(base);
        } catch (Exception e) {
            log.warn("[free-flow] extJson 合并失败，回退为步骤配置. {}", e.getMessage());
            return stepJson;
        }
    }
}
