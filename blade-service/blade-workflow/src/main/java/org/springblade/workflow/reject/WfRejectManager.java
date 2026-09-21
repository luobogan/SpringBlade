package org.springblade.workflow.reject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.WfNodeLink;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.mapper.WfNodeLinkMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
import org.springframework.stereotype.Component;

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
 * <p>对齐 ecology {@code RequestRejectManager}：从当前节点沿 {@code wf_node_link}
 * <b>反向</b>回溯到创建节点，凡出口 {@code is_reject=1} 的方向不可退（封锁该分支回溯），
 * 并依据节点 {@code settings.reject.nodeKeys} 白名单裁剪。计算结果即「可退回的候选节点集合」。</p>
 *
 * <p>设计原则：读不到配置就返回「全部上游可达节点」的兜底集合，绝不抛异常阻断退回主链路。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfRejectManager {

    private final WfProcessNodeMapper nodeMapper;
    private final WfNodeLinkMapper linkMapper;

    /**
     * 计算当前节点可退回的目标节点集合（不含当前节点）。
     *
     * <p>只保留「引擎能停留的节点」：创建(0)/审批(1)/提交(2)。归档(3)/等待(5)/自动处理(6)/网关(7)
     * 在 BPMN 里不是等待态（startEvent/endEvent/receiveTask/serviceTask/gateway），
     * token 移过去不会停住 —— 会立刻沿出口继续流出，表现为「退回了但没动」，
     * 落在网关上还会按条件重新选分支（可能走回原节点或跳到别的分支）。</p>
     *
     * <p>创建节点(0)在引擎里同样是 startEvent，但它有专门的「退回发起人」路径
     * （见 {@code WfTaskServiceImpl#reject} → {@code rejectToStarter}），故保留为候选。</p>
     *
     * @return 候选节点（按离当前节点由近及远排序）；无可退节点返回空列表
     */
    public List<WfProcessNode> computeRejectableNodes(Long defId, String currentNodeKey, WfProcessNode currentNode) {
        if (defId == null || currentNodeKey == null) {
            return List.of();
        }
        // 白名单：rejectType=2 时限制可选范围
        List<String> whitelist = WfNodeSettingsUtil.rejectableNodeKeys(currentNode);

        // 反向邻接表：toNodeKey -> 入边列表
        List<WfNodeLink> links = linkMapper.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers
            .<WfNodeLink>lambdaQuery()
            .eq(WfNodeLink::getDefId, defId));
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
        List<WfProcessNode> nodes = nodeMapper.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers
            .<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .in(WfProcessNode::getNodeKey, order));
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

}
