package org.springblade.workflow.reject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.WfNodeLink;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.mapper.WfNodeLinkMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.resolver.WfBpmnExtensionReader;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springblade.workflow.config.WfRetirementProperties;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 退回（reject）目标节点计算。
 *
 * <p>对齐 ecology {@code RequestRejectManager}：从当前节点沿出口连线
 * <b>反向</b>回溯到创建节点，凡出口 {@code is_reject=1} 的方向不可退（封锁该分支回溯），
 * 并依据节点 {@code settings.reject.nodeKeys} 白名单裁剪。计算结果即「可退回的候选节点集合」。</p>
 *
 * <p><b>读源（P3-5）</b>：开关 {@code blade.workflow.definition-from-bpmn.enabled}（与
 * {@code WfTaskServiceImpl.loadNode} / {@code WfDefinitionServiceImpl.loadNodes} 共用）开启时，
 * 出口/节点改读引擎部署模型（{@link WfBpmnExtensionReader#links} 含折叠连线 +
 * {@link WfBpmnExtensionReader#nodes}），空（草稿/未部署）或异常回退 {@code wf_node_link}/{@code wf_process_node}。</p>
 *
 * <p>设计原则：读不到配置就返回「全部上游可达节点」的兜底集合，绝不抛异常阻断退回主链路。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfRejectManager {

    private final WfProcessNodeMapper nodeMapper;
    private final WfNodeLinkMapper linkMapper;
    /** BPMN wf: 扩展读取收口：定义读源=BPMN 时出口/节点改读引擎部署模型（含折叠连线） */
    private final WfBpmnExtensionReader bpmnReader;

    /** 定义读源开关：三个消费方（任务/定义/退回）共用同一配置 key，默认关 */
    @Value("${blade.workflow.definition-from-bpmn.enabled:false}")
    private boolean definitionFromBpmn;

    @Autowired
    private WfRetirementProperties retirement = new WfRetirementProperties();

    /**
     * 计算当前节点可退回的目标节点集合（不含当前节点）——读源收口入口。
     * 数据加载与算法分离：算法见纯函数 {@link #computeRejectableNodes(Long, String, WfProcessNode, List, List)}。
     */
    public List<WfProcessNode> computeRejectableNodes(Long defId, String currentNodeKey, WfProcessNode currentNode) {
        if (defId == null || currentNodeKey == null) {
            return List.of();
        }
        return computeRejectableNodes(defId, currentNodeKey, currentNode, loadLinks(defId), loadNodes(defId));
    }

    /**
     * 计算当前节点可退回的目标节点集合（不含当前节点）——纯函数核心（数据来源无关）。
     *
     * <p>只保留「引擎能停留的节点」：创建(0)/审批(1)/提交(2) —— 这三类都是 {@code UserTask} 等待态。
     * 归档(3)/等待(5)/自动处理(6)/网关(7) 在 BPMN 里不是等待态（endEvent/receiveTask/serviceTask/gateway），
     * token 移过去不会停住 —— 会立刻沿出口继续流出，表现为「退回了但没动」，
     * 落在网关上还会按条件重新选分支（可能走回原节点或跳到别的分支）。</p>
     *
     * <p>创建节点(0)「申请人填单」同样是 {@code UserTask} 等待态（早期建模为 startEvent 时不是等待态，
     * 需靠专门的「退回发起人」路径；该错配已根治），故它与其余 UserTask 一样可正常作为退回目标：
     * 引擎会停住并生成真实待办（办理人=发起人）。</p>
     *
     * @param links  出口连线（wf_node_link 表或 BPMN wf:link+wf:foldedLink）
     * @param nodes  节点配置（wf_process_node 表或 BPMN wf:node）
     * @return 候选节点（按离当前节点由近及远排序）；无可退节点返回空列表
     */
    public static List<WfProcessNode> computeRejectableNodes(Long defId, String currentNodeKey, WfProcessNode currentNode,
                                                             List<WfNodeLink> links, List<WfProcessNode> nodes) {
        // 白名单：rejectType=2 时限制可选范围
        List<String> whitelist = WfNodeSettingsUtil.rejectableNodeKeys(currentNode);

        // 反向邻接表：toNodeKey -> 入边列表
        Map<String, List<WfNodeLink>> incoming = new HashMap<>();
        for (WfNodeLink l : links) {
            if (l.getToNodeKey() == null) {
                continue;
            }
            incoming.computeIfAbsent(l.getToNodeKey(), k -> new ArrayList<>()).add(l);
        }

        // BFS 反向回溯：仅走 is_reject<>1 的入边，跳过已访问；可覆盖分叉/并行（多入边均入队）
        Set<String> visited = new HashSet<>();
        List<String> order = new ArrayList<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(currentNodeKey);
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            for (WfNodeLink l : incoming.getOrDefault(cur, List.of())) {
                String from = l.getFromNodeKey();
                if (from == null || from.equals(currentNodeKey)) {
                    continue;
                }
                // 出口方向封锁：该连线 is_reject=1 表示反向不可退（对齐 ecology nodelink.isreject）
                if (l.getIsReject() != null && l.getIsReject() == 1) {
                    continue;
                }
                if (visited.add(from)) {
                    order.add(from);
                    queue.add(from);
                }
            }
        }

        // 白名单裁剪
        if (!whitelist.isEmpty()) {
            order.removeIf(k -> !whitelist.contains(k));
        }
        if (order.isEmpty()) {
            return List.of();
        }

        // 取节点对象并按回溯顺序（由近及远）返回
        Map<String, WfProcessNode> byKey = new HashMap<>();
        for (WfProcessNode n : nodes) {
            byKey.put(n.getNodeKey(), n);
        }
        List<WfProcessNode> result = new ArrayList<>();
        for (String k : order) {
            WfProcessNode n = byKey.get(k);
            if (n == null) {
                continue;
            }
            // 剔除「引擎停不住」的节点：归档(3)/等待(5)/自动处理(6)/网关(7)。
            // 它们不是等待态，移 token 过去会立刻继续流出 —— 是「退回了但节点没变」的根因之一。
            // 仅按类型剔除（不白名单化），未知/未配类型的节点保持既有行为，避免误伤。
            Integer type = n.getNodeType();
            if (type != null && (type == 3 || type == 5 || type == 6 || type == 7)) {
                continue;
            }
            result.add(n);
        }
        return result;
    }

    /**
     * 默认退回节点：配置的 {@code defaultNodeKey}（须在可退回集合内）优先，
     * 否则取最近的上游节点（{@link #computeRejectableNodes} 已按由近及远排序，首项即最近）。
     */
    public String resolveDefaultRejectNode(Long defId, String currentNodeKey, WfProcessNode currentNode,
                                          List<WfProcessNode> candidates) {
        String configured = WfNodeSettingsUtil.defaultRejectNodeKey(currentNode);
        if (configured != null && candidates.stream().anyMatch(n -> configured.equals(n.getNodeKey()))) {
            return configured;
        }
        return candidates.isEmpty() ? null : candidates.get(0).getNodeKey();
    }

    /** 目标节点是否合法（必须在可退回集合内） */
    public boolean isRejectable(List<WfProcessNode> candidates, String targetNodeKey) {
        if (targetNodeKey == null || candidates.isEmpty()) {
            return false;
        }
        return candidates.stream().anyMatch(n -> targetNodeKey.equals(n.getNodeKey()));
    }

    // ------------------------------------------------------------------ 读源取数（BPMN 优先，回退 wf_*）

    /** 出口连线：定义源=BPMN 时读引擎部署模型（wf:link + wf:foldedLink），空/异常回退 wf_node_link */
    private List<WfNodeLink> loadLinks(Long defId) {
        if (definitionFromBpmn) {
            try {
                List<WfNodeLink> links = bpmnReader.links(defId);
                if (links != null && !links.isEmpty()) {
                    return links;
                }
                log.info("[blade-workflow] 定义读源=BPMN：该定义未部署（草稿/无 procDefId），回退 wf_node_link. defId={}", defId);
            } catch (Exception e) {
                log.warn("[blade-workflow] 定义读源=BPMN 读取出口失败，回退 wf_node_link. defId={}, {}", defId, e.getMessage());
            }
        }
        // T-14 退役：主开关开启时禁用 wf_node_link 回退读（BPMN 为唯一源；草稿/缺扩展按空返回）。
        if (!retirement.isEnabled()) {
            return linkMapper.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers
                .<WfNodeLink>lambdaQuery()
                .eq(WfNodeLink::getDefId, defId));
        }
        return new ArrayList<>();
    }

    /** 节点配置：定义源=BPMN 时读引擎部署模型（wf:node），空/异常回退 wf_process_node */
    private List<WfProcessNode> loadNodes(Long defId) {
        if (definitionFromBpmn) {
            try {
                List<WfProcessNode> nodes = bpmnReader.nodes(defId);
                if (nodes != null && !nodes.isEmpty()) {
                    return nodes;
                }
                log.info("[blade-workflow] 定义读源=BPMN：该定义未部署（草稿/无 procDefId），回退 wf_process_node. defId={}", defId);
            } catch (Exception e) {
                log.warn("[blade-workflow] 定义读源=BPMN 读取节点失败，回退 wf_process_node. defId={}, {}", defId, e.getMessage());
            }
        }
        // T-14 退役：主开关开启时禁用 wf_process_node 回退读（BPMN 为唯一源；草稿/缺扩展按空返回）。
        if (!retirement.isEnabled()) {
            return nodeMapper.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers
                .<WfProcessNode>lambdaQuery()
                .eq(WfProcessNode::getDefId, defId));
        }
        return new ArrayList<>();
    }

}
