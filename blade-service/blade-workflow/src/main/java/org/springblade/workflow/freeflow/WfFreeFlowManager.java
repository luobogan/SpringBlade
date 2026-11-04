package org.springblade.workflow.freeflow;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.dto.FreeFlowSaveDTO;
import org.springblade.workflow.dto.FreeFlowStepDTO;
import org.springblade.workflow.entity.WfFreeLink;
import org.springblade.workflow.entity.WfFreeNode;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfNodeLink;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.mapper.WfFreeLinkMapper;
import org.springblade.workflow.mapper.WfFreeNodeMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfNodeLinkMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.vo.FreeFlowStepVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 自由流程步骤编排管理器。
 *
 * <p>核心约束（落地分析 §7.7 / §10.1）：</p>
 * <ul>
 *   <li>每个步骤绑定到预置池<b>静态</b> activityId {@code Free_<flowOrder>}，写入后不变；</li>
 *   <li>「删旧 + 建新 + 建链 + 复制出口」放在<b>同一事务</b>内整包替换；</li>
 *   <li>{@code version} 乐观锁 + {@code saveToken} 幂等键，防止两人并发编辑互相覆盖；</li>
 *   <li>物理删除（不是逻辑删除），否则唯一键被旧行占用；</li>
 *   <li>按 {@code inst_id} 级联清理，草稿 / 测试清理 / 正式删除三处调用同一入口。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfFreeFlowManager {

    /** 与前端 buildDefaultBpmn 生成节点池时保持一致的命名约定 */
    public static final String POOL_NODE_PREFIX = "Free_";
    /** 未配置 free_node_pool_size 时的默认值 */
    public static final int DEFAULT_POOL_SIZE = 5;

    private final WfFreeNodeMapper nodeMapper;
    private final WfFreeLinkMapper linkMapper;
    private final WfNodeLinkMapper nodeLinkMapper;
    private final WfProcessDefinitionMapper defMapper;
    private final WfInstanceMapper instanceMapper;

    /** 保存幂等键（进程级；重启后由 version 乐观锁兜底） */
    private final Map<Long, String> lastSaveTokens = new ConcurrentHashMap<>();

    /** 预置节点池容量（= 单实例自由步骤上限） */
    public int poolSize(Long defId) {
        if (defId == null) {
            return DEFAULT_POOL_SIZE;
        }
        WfProcessDefinition def = defMapper.selectById(defId);
        if (def == null || def.getFreeNodePoolSize() == null || def.getFreeNodePoolSize() <= 0) {
            return DEFAULT_POOL_SIZE;
        }
        return def.getFreeNodePoolSize();
    }

    public List<WfFreeNode> loadSteps(Long instId) {
        if (instId == null) {
            return Collections.emptyList();
        }
        return nodeMapper.selectList(Wrappers.<WfFreeNode>lambdaQuery()
            .eq(WfFreeNode::getInstId, instId)
            .orderByAsc(WfFreeNode::getFlowOrder));
    }

    /** 步骤编排回显 */
    public List<FreeFlowStepVO> view(Long instId) {
        return loadSteps(instId).stream().map(n -> {
            FreeFlowStepVO vo = new FreeFlowStepVO();
            vo.setFlowOrder(n.getFlowOrder());
            vo.setNodeName(n.getNodeName());
            vo.setBoundNodeKey(n.getNodeKey());
            vo.setOperatorIds(parseIds(n.getOperatorIds()));
            vo.setSignType(n.getSignType());
            vo.setIsFinalize(n.getIsFinalize() != null && n.getIsFinalize() == 1);
            vo.setMustTaohong(n.getMustTaohong() != null && n.getMustTaohong() == 1);
            vo.setParallelGroup(n.getParallelGroup());
            vo.setNodeStatus(n.getNodeStatus());
            vo.setExtJson(n.getExtJson());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 整包保存步骤（单事务）。返回新版本号。
     */
    @Transactional(rollbackFor = Exception.class)
    public int saveSteps(FreeFlowSaveDTO dto, Long operator) {
        if (dto == null || dto.getInstId() == null) {
            throw new ServiceException("流程实例ID不能为空");
        }
        List<FreeFlowStepDTO> steps = dto.getSteps();
        if (steps == null || steps.isEmpty()) {
            throw new ServiceException("请至少设置一个自由流程步骤");
        }
        Long instId = dto.getInstId();
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            throw new ServiceException("流程实例不存在：" + instId);
        }
        int poolSize = poolSize(inst.getDefId());
        if (steps.size() > poolSize) {
            throw new ServiceException("步骤数（" + steps.size() + "）超过预置节点池容量 " + poolSize
                + "，请到「流程设计 → 基础设置」调大「通用节点数量」并重新发布后再设置");
        }

        WfFreeNodeContext current = WfFreeNodeContext.load(nodeMapper, instId);
        int curVersion = current.version();
        if (dto.getVersion() != null && dto.getVersion() != curVersion) {
            throw new ServiceException("自由流程步骤已被他人修改（当前版本 " + curVersion
                + "，提交版本 " + dto.getVersion() + "），请刷新后重试");
        }
        String token = dto.getSaveToken();
        if (token != null && !token.isBlank() && token.equals(lastSaveTokens.get(instId))) {
            log.info("[free-flow] 重复保存被幂等键拦截. instId={}, version={}", instId, curVersion);
            return curVersion;
        }

        // 按 flowOrder 排序后重排为 1..N，绑定池内静态节点 Free_<order>
        List<FreeFlowStepDTO> sorted = new ArrayList<>(steps);
        sorted.sort(Comparator.comparingInt(s -> s.getFlowOrder() == null ? Integer.MAX_VALUE : s.getFlowOrder()));
        int newVersion = curVersion + 1;
        List<WfFreeNode> rows = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            FreeFlowStepDTO s = sorted.get(i);
            int order = i + 1;
            List<Long> ids = s.getOperatorIds() == null ? Collections.emptyList() : s.getOperatorIds();
            if (ids.isEmpty()) {
                throw new ServiceException("第 " + order + " 步未指定办理人");
            }
            // 发起后实例通常停在「创建」节点（真实 UserTask，非池内节点）。若把第 1 步
            // 绑定到 Free_1，driver.submit 拿当前节点去查步骤会落空并拒绝提交。
            // 故第 1 步直接绑定到实例当前节点，后续步骤才用池内 Free_k。
            String boundKey = POOL_NODE_PREFIX + order;
            String startKey = inst.getCurrentNodeKey();
            if (i == 0 && startKey != null && !startKey.isBlank()
                && !startKey.startsWith(POOL_NODE_PREFIX)) {
                boundKey = startKey;
            }
            WfFreeNode n = new WfFreeNode();
            n.setInstId(instId);
            n.setDefId(inst.getDefId());
            n.setNodeKey(boundKey);
            n.setStartNodeKey(startKey);
            n.setFlowOrder(order);
            n.setNodeName(s.getNodeName() == null || s.getNodeName().isBlank()
                ? "步骤" + order : s.getNodeName());
            n.setOperatorIds(ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
            n.setSignType(normalizeSignType(s.getSignType()));
            n.setExtJson(s.getExtJson());
            n.setVersion(newVersion);
            n.setIsFinalize(Boolean.TRUE.equals(s.getIsFinalize()) ? 1 : 0);
            n.setMustTaohong(Boolean.TRUE.equals(s.getMustTaohong()) ? 1 : 0);
            n.setParallelGroup(s.getParallelGroup());
            n.setNodeStatus(0);
            rows.add(n);
        }

        // 整包替换：物理删旧 → 插新步骤 → 建实例链路 → 复制定义态出口接到末步
        nodeMapper.physicalDeleteByInstId(instId);
        linkMapper.physicalDeleteByInstId(instId);
        for (WfFreeNode n : rows) {
            nodeMapper.insert(n);
        }
        buildLinks(instId, inst.getDefId(), rows, inst.getCurrentNodeKey());

        if (token != null && !token.isBlank()) {
            lastSaveTokens.put(instId, token);
        } else {
            lastSaveTokens.remove(instId);
        }
        log.info("[free-flow] 步骤整包保存完成. instId={}, defId={}, steps={}, version={}, operator={}",
            instId, inst.getDefId(), rows.size(), newVersion, operator);
        return newVersion;
    }

    /** 建实例链路：相邻正向线 + 逐级退回线 + 末步接回定义态出口 */
    private void buildLinks(Long instId, Long defId, List<WfFreeNode> rows, String startNodeKey) {
        for (int i = 0; i < rows.size() - 1; i++) {
            insertLink(instId, defId, rows.get(i).getNodeKey(), rows.get(i + 1).getNodeKey(),
                0, 0, null, i + 1);
        }
        for (int i = 1; i < rows.size(); i++) {
            insertLink(instId, defId, rows.get(i).getNodeKey(), rows.get(i - 1).getNodeKey(),
                1, 0, null, 100 + i);
        }
        if (startNodeKey == null || defId == null || rows.isEmpty()) {
            return;
        }
        String lastKey = rows.get(rows.size() - 1).getNodeKey();
        List<WfNodeLink> exits = nodeLinkMapper.selectList(Wrappers.<WfNodeLink>lambdaQuery()
            .eq(WfNodeLink::getDefId, defId)
            .eq(WfNodeLink::getFromNodeKey, startNodeKey)
            .orderByAsc(WfNodeLink::getSortOrder));
        int seq = 200;
        for (WfNodeLink e : exits) {
            WfFreeLink l = new WfFreeLink();
            l.setInstId(instId);
            l.setDefId(defId);
            l.setFromNodeKey(lastKey);
            l.setToNodeKey(e.getToNodeKey());
            l.setIsReject(e.getIsReject());
            l.setIsMustPass(e.getIsMustPass());
            l.setConditionCopy(e.getConditionExpr());
            l.setSortOrder(seq++);
            linkMapper.insert(l);
        }
    }

    private void insertLink(Long instId, Long defId, String from, String to,
                            int isReject, int mustPass, String condition, int sort) {
        WfFreeLink l = new WfFreeLink();
        l.setInstId(instId);
        l.setDefId(defId);
        l.setFromNodeKey(from);
        l.setToNodeKey(to);
        l.setIsReject(isReject);
        l.setIsMustPass(mustPass);
        l.setConditionCopy(condition);
        l.setSortOrder(sort);
        linkMapper.insert(l);
    }

    /** 退回后把目标步骤及其后的步骤重新置为「未流转」 */
    @Transactional(rollbackFor = Exception.class)
    public void resetFrom(Long instId, String targetNodeKey) {
        if (instId == null || targetNodeKey == null) {
            return;
        }
        WfFreeNodeContext ctx = WfFreeNodeContext.load(nodeMapper, instId);
        if (ctx.isEmpty()) {
            return;
        }
        WfFreeNode target = ctx.byNodeKey(targetNodeKey);
        if (target == null || target.getFlowOrder() == null) {
            return;
        }
        for (WfFreeNode n : ctx.getOrdered()) {
            if (n.getFlowOrder() != null && n.getFlowOrder() >= target.getFlowOrder()
                && !Integer.valueOf(0).equals(n.getNodeStatus())) {
                n.setNodeStatus(0);
                nodeMapper.updateById(n);
            }
        }
        log.info("[free-flow] 退回后重置步骤状态. instId={}, fromNodeKey={}, fromOrder={}",
            instId, targetNodeKey, target.getFlowOrder());
    }

    /** 实例数据级联清理（草稿删除 / 测试清理 / 正式删除统一入口） */
    @Transactional(rollbackFor = Exception.class)
    public void cleanup(Long instId) {
        if (instId == null) {
            return;
        }
        linkMapper.physicalDeleteByInstId(instId);
        nodeMapper.physicalDeleteByInstId(instId);
        lastSaveTokens.remove(instId);
        log.info("[free-flow] 实例步骤与链路已级联清理. instId={}", instId);
    }

    public static List<Long> parseIds(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(csv.split(","))
            .map(String::trim).filter(s -> !s.isEmpty())
            .map(Long::valueOf).collect(Collectors.toList());
    }

    private static int normalizeSignType(Integer signType) {
        if (signType == null) {
            return WfFreeNode.SIGN_NONE;
        }
        // 运行期仅支持 0/1/2；3/4（传阅）不在此暴露，避免按注释误判已支持
        return (signType == WfFreeNode.SIGN_ALL || signType == WfFreeNode.SIGN_SEQUENCE)
            ? signType : WfFreeNode.SIGN_NONE;
    }
}
