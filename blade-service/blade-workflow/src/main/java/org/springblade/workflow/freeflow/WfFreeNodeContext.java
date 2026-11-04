package org.springblade.workflow.freeflow;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springblade.workflow.entity.WfFreeNode;
import org.springblade.workflow.mapper.WfFreeNodeMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自由流程实例步骤的请求级上下文（一次流转只查一次库，避免各钩子重复查表 N+1）。
 *
 * <p>不可变快照；非自由流程实例请用 {@link #empty()}（零查询、零行为变化）。</p>
 */
public final class WfFreeNodeContext {

    private final Long instId;
    private final List<WfFreeNode> ordered;
    private final Map<String, WfFreeNode> byNodeKey;

    private WfFreeNodeContext(Long instId, List<WfFreeNode> ordered) {
        this.instId = instId;
        this.ordered = Collections.unmodifiableList(new ArrayList<>(ordered));
        Map<String, WfFreeNode> map = new LinkedHashMap<>(ordered.size());
        for (WfFreeNode n : ordered) {
            if (n != null && n.getNodeKey() != null) {
                map.put(n.getNodeKey(), n);
            }
        }
        this.byNodeKey = Collections.unmodifiableMap(map);
    }

    /** 非自由流程实例的空上下文 */
    public static WfFreeNodeContext empty() {
        return new WfFreeNodeContext(null, Collections.emptyList());
    }

    /** 由给定步骤构造上下文（供单测与无需查库的调用方使用） */
    public static WfFreeNodeContext of(Long instId, List<WfFreeNode> steps) {
        return new WfFreeNodeContext(instId, steps == null ? Collections.emptyList() : steps);
    }

    /** 按 flow_order 一次性加载实例步骤 */
    public static WfFreeNodeContext load(WfFreeNodeMapper mapper, Long instId) {
        if (mapper == null || instId == null) {
            return empty();
        }
        List<WfFreeNode> rows = mapper.selectList(Wrappers.<WfFreeNode>lambdaQuery()
            .eq(WfFreeNode::getInstId, instId)
            .orderByAsc(WfFreeNode::getFlowOrder));
        return new WfFreeNodeContext(instId, rows == null ? Collections.emptyList() : rows);
    }

    public Long getInstId() {
        return instId;
    }

    public List<WfFreeNode> getOrdered() {
        return ordered;
    }

    public boolean isEmpty() {
        return ordered.isEmpty();
    }

    public WfFreeNode byNodeKey(String nodeKey) {
        return nodeKey == null ? null : byNodeKey.get(nodeKey);
    }

    /** 取下一步（flowOrder 严格递增的第一个） */
    public WfFreeNode nextOf(WfFreeNode current) {
        if (current == null || current.getFlowOrder() == null) {
            return null;
        }
        for (WfFreeNode n : ordered) {
            if (n.getFlowOrder() != null && n.getFlowOrder() > current.getFlowOrder()) {
                return n;
            }
        }
        return null;
    }

    /** 当前版本（整包替换时所有行同值） */
    public int version() {
        int max = 0;
        for (WfFreeNode n : ordered) {
            if (n.getVersion() != null && n.getVersion() > max) {
                max = n.getVersion();
            }
        }
        return max;
    }
}
