package org.springblade.workflow.resolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.system.user.feign.IUserClient;
import org.springblade.workflow.entity.WfFormSnapshot;
import org.springblade.workflow.entity.WfNodeOperator;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.mapper.WfFormSnapshotMapper;
import org.springblade.workflow.mapper.WfNodeOperatorMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 节点操作者解析器：把 {@code wf_node_operator} 的配置解析成**具体的办理人用户ID集合**。
 *
 * <h3>支持矩阵</h3>
 * <table>
 *   <tr><th>opType</th><th>含义</th><th>解析方式</th></tr>
 *   <tr><td>3</td><td>人员</td><td>objId 即用户ID（支持逗号分隔多值 → 多个办理人）</td></tr>
 *   <tr><td>17 / 40</td><td>创建人本人 / 本人</td><td>取实例发起人</td></tr>
 *   <tr><td>5</td><td>字段-人员</td><td>取表单数据（wf_form_snapshot.data_json）中名为 objId 的字段值（同样支持多值）</td></tr>
 *   <tr><td>1</td><td>部门</td><td>{@code IUserClient#userIdsByDept}（{@code bhxj=1} 含下级；多值逐个查后合并）</td></tr>
 *   <tr><td>2</td><td>角色</td><td>{@code IUserClient#userIdsByRole}（多值逐个查后合并）</td></tr>
 *   <tr><td>58</td><td>岗位</td><td>{@code IUserClient#userIdsByPost}（多值逐个查后合并）</td></tr>
 *   <tr><td>4</td><td>所有人</td><td>{@code IUserClient#allUserIds}（服务端 LIMIT 兜底）</td></tr>
 *   <tr><td>42 / 43</td><td>字段-部门 / 字段-角色</td><td>取表单字段值后同上</td></tr>
 *   <tr><td>19</td><td>本部门</td><td>{@code IUserClient#userIdsByDeptOfUser}（到达本节点的当前办理人所在部门成员）</td></tr>
 *   <tr><td>18 / 41 / 6</td><td>创建人上级 / 上级 / 字段-人员上级</td><td>{@code IUserClient#leaderId}（manager_id 指向的主管）</td></tr>
 * </table>
 *
 * <p><b>多选说明</b>：前端「人员/部门/角色/岗位」选择器均为多选，落库的 {@code objId} 是
 * <b>逗号分隔的 id 串</b>（与 E9 一致）。因此 {@link #parseIds} 负责把 objId 拆成多个 id，
 * 人员类直接展开为多人，部门/角色/岗位类逐个查询后合并去重。</p>
 *
 * <p><b>「上级」类数据源</b>：{@code blade_user.manager_id}（个人主管 / 直线经理，贴近 E9 的 ManagerID）。
 * 18 / 41 取发起人（starter）的主管，6 取「字段-人员」对应用户的主管。</p>
 *
 * <p><b>「本部门」语义</b>：取「到达本节点的当前办理人」所在部门的成员。首节点到达人 = 发起人（starter），
 * 后续节点 = 上一节点完成人（由 {@code advance(instId, currentOperator)} 传入）。因此 {@link #resolve} 必须拿到
 * {@code currentOperator} 才能解析 19。</p>
 *
 * <p><b>降级策略（重要）</b>：所有远端查询统一走 {@link #remote}，**失败/超时/返回空一律只打 warn、降级为空集合**；
 * 调用方（{@code WfInstanceServiceImpl#advance}）在解析结果为空时会回退到「引擎 assignee」的原有行为，
 * <b>绝不抛异常阻塞流转主链路</b>。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfOperatorResolver {

    /** 人员 */
    public static final int OP_USER = 3;
    /** 部门 */
    public static final int OP_DEPT = 1;
    /** 角色 */
    public static final int OP_ROLE = 2;
    /** 岗位 */
    public static final int OP_POST = 58;
    /** 所有人 */
    public static final int OP_ALL = 4;
    /** 创建人本人 */
    public static final int OP_CREATOR = 17;
    /** 本人 */
    public static final int OP_SELF = 40;
    /** 创建人上级 */
    public static final int OP_CREATOR_LEADER = 18;
    /** 上级 */
    public static final int OP_LEADER = 41;
    /** 字段-人员 */
    public static final int OP_FIELD_USER = 5;
    /** 字段-人员上级 */
    public static final int OP_FIELD_USER_LEADER = 6;
    /** 字段-部门 */
    public static final int OP_FIELD_DEPT = 42;
    /** 字段-角色 */
    public static final int OP_FIELD_ROLE = 43;
    /** 本部门（到达本节点的当前办理人所在部门） */
    public static final int OP_DEPT_SELF = 19;

    private final WfNodeOperatorMapper operatorMapper;
    private final WfProcessNodeMapper nodeMapper;
    private final WfFormSnapshotMapper snapshotMapper;
    /** 用户中心：把「部门/角色/岗位/所有人/上级/本部门」解析成真实办理人（Feign 失败即降级，不阻塞流转） */
    private final IUserClient userClient;

    /**
     * 解析某节点的操作者为办理人ID集合（已去重、已剔除 0/null）。
     *
     * @param defId          流程定义ID
     * @param nodeKey        节点Key
     * @param instId         实例ID（用于取发起人 / 表单数据）
     * @param starter        发起人（可为 null，则取实例记录）
     * @param currentOperator 到达本节点的当前办理人（首节点=发起人，后续=上一节点完成人）；解析「本部门(19)」必需
     * @return 办理人ID集合；为空表示「无法解析」，调用方应回退原有行为
     */
    public List<Long> resolve(Long defId, String nodeKey, Long instId, Long starter, Long currentOperator) {
        if (defId == null || nodeKey == null) {
            return List.of();
        }
        WfProcessNode node = nodeMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WfProcessNode>()
                .eq(WfProcessNode::getDefId, defId)
                .eq(WfProcessNode::getNodeKey, nodeKey)
                .last("LIMIT 1"));
        if (node == null || node.getId() == null) {
            return List.of();
        }
        List<WfNodeOperator> operators = operatorMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WfNodeOperator>()
                .eq(WfNodeOperator::getNodeId, node.getId()));
        if (operators.isEmpty()) {
            return List.of();
        }
        Map<String, Object> formData = loadFormData(instId);
        Set<Long> ids = new LinkedHashSet<>();
        for (WfNodeOperator op : operators) {
            ids.addAll(resolveByType(op, starter, currentOperator, formData));
        }
        return new ArrayList<>(ids);
    }

    /** 按操作者类型解析；不支持的类型返回空集合并 warn（不抛异常） */
    private List<Long> resolveByType(WfNodeOperator op, Long starter, Long currentOperator, Map<String, Object> formData) {
        Integer type = op.getOpType();
        if (type == null) {
            return List.of();
        }
        // bhxj=1（含下级）时部门查询包含子孙部门
        boolean containChild = Integer.valueOf(1).equals(op.getBhxj());
        switch (type) {
            case OP_USER:
                // objId 可以是逗号分隔的多人
                return parseIds(op.getObjId());
            case OP_CREATOR:
            case OP_SELF:
                return single(starter);
            case OP_FIELD_USER:
                return parseIds(formData.get(op.getObjId()));
            case OP_DEPT:
                return byDeptEach(parseIds(op.getObjId()), containChild);
            case OP_FIELD_DEPT:
                return byDeptEach(parseIds(formData.get(op.getObjId())), containChild);
            case OP_ROLE:
                return byRoleEach(parseIds(op.getObjId()));
            case OP_FIELD_ROLE:
                return byRoleEach(parseIds(formData.get(op.getObjId())));
            case OP_POST:
                return byPostEach(parseIds(op.getObjId()));
            case OP_ALL:
                return remote("查全部用户", () -> userClient.allUserIds(null));
            case OP_CREATOR_LEADER:
            case OP_LEADER:
                return leaderId(starter);
            case OP_FIELD_USER_LEADER:
                return leaderIdEach(parseIds(formData.get(op.getObjId())));
            case OP_DEPT_SELF:
                return userIdsByDeptOfUser(currentOperator, containChild);
            default:
                log.warn("[blade-workflow] 操作者类型暂不支持解析，已跳过（待办将回退为引擎 assignee）. "
                        + "opType={}, objId={}", type, op.getObjId());
                return List.of();
        }
    }

    /** 按部门（可选含下级）查用户ID */
    private List<Long> byDept(Long deptId, boolean containChild) {
        if (deptId == null) {
            return List.of();
        }
        return remote("按部门查用户(deptId=" + deptId + ", containChild=" + containChild + ")",
            () -> userClient.userIdsByDept(deptId, containChild));
    }

    /** 多选部门：逐个查询后合并（一个部门失败不影响其它部门） */
    private List<Long> byDeptEach(List<Long> deptIds, boolean containChild) {
        List<Long> all = new ArrayList<>();
        for (Long id : deptIds) {
            all.addAll(byDept(id, containChild));
        }
        return all;
    }

    /** 按角色查用户ID */
    private List<Long> byRole(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        return remote("按角色查用户(roleId=" + roleId + ")", () -> userClient.userIdsByRole(roleId));
    }

    /** 多选角色：逐个查询后合并 */
    private List<Long> byRoleEach(List<Long> roleIds) {
        List<Long> all = new ArrayList<>();
        for (Long id : roleIds) {
            all.addAll(byRole(id));
        }
        return all;
    }

    /** 按岗位查用户ID */
    private List<Long> byPost(Long postId) {
        if (postId == null) {
            return List.of();
        }
        return remote("按岗位查用户(postId=" + postId + ")", () -> userClient.userIdsByPost(postId));
    }

    /** 多选岗位：逐个查询后合并 */
    private List<Long> byPostEach(List<Long> postIds) {
        List<Long> all = new ArrayList<>();
        for (Long id : postIds) {
            all.addAll(byPost(id));
        }
        return all;
    }

    /** 取指定用户的主管用户ID（上级 / 直线经理） */
    private List<Long> leaderId(Long userId) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        try {
            R<Long> r = userClient.leaderId(userId);
            if (r == null || !r.isSuccess() || r.getData() == null) {
                log.warn("[blade-workflow] 查用户主管(userId={}) 远端返回失败，已降级为空集合（待办回退引擎 assignee）", userId);
                return List.of();
            }
            Long m = r.getData();
            return (m == null || m <= 0) ? List.of() : List.of(m);
        } catch (Exception e) {
            log.warn("[blade-workflow] 查用户主管(userId={}) 远端调用异常，已降级为空集合: {}", userId, e.getMessage());
            return List.of();
        }
    }

    /** 多选用户的主管：逐个查询后合并（字段-人员上级多值时） */
    private List<Long> leaderIdEach(List<Long> userIds) {
        List<Long> all = new ArrayList<>();
        for (Long id : userIds) {
            all.addAll(leaderId(id));
        }
        return all;
    }

    /** 取指定用户所在部门的成员用户ID（本部门） */
    private List<Long> userIdsByDeptOfUser(Long userId, boolean containChild) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        return remote("按用户部门查成员(userId=" + userId + ", containChild=" + containChild + ")",
            () -> userClient.userIdsByDeptOfUser(userId, containChild));
    }

    /**
     * 统一的远端查询包装：**失败/异常一律告警并降级为空集合**。
     *
     * <p>调用方（advance → 生成待办）在解析结果为空时会回退到「引擎 assignee」的原有行为，
     * 因此这里绝不抛异常，保证用户中心不可用时流转主链路不受影响。</p>
     */
    private List<Long> remote(String desc, Supplier<R<List<Long>>> call) {
        try {
            R<List<Long>> r = call.get();
            if (r == null || !r.isSuccess() || r.getData() == null) {
                log.warn("[blade-workflow] {} 远端返回失败，已降级为空集合（待办回退引擎 assignee）", desc);
                return List.of();
            }
            return r.getData();
        } catch (Exception e) {
            log.warn("[blade-workflow] {} 远端调用异常，已降级为空集合: {}", desc, e.getMessage());
            return List.of();
        }
    }

    /** 取实例最新的表单数据（供「字段-人员」类操作者解析） */
    private Map<String, Object> loadFormData(Long instId) {
        if (instId == null) {
            return Map.of();
        }
        try {
            List<WfFormSnapshot> snaps = snapshotMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WfFormSnapshot>()
                    .eq(WfFormSnapshot::getInstId, instId)
                    .orderByDesc(WfFormSnapshot::getId)
                    .last("LIMIT 1"));
            if (snaps.isEmpty() || StringUtil.isBlank(snaps.get(0).getDataJson())) {
                return Map.of();
            }
            Map<String, Object> data = JsonUtil.parse(snaps.get(0).getDataJson(), Map.class);
            return data == null ? Map.of() : data;
        } catch (Exception e) {
            log.warn("[blade-workflow] 表单数据解析失败，字段型操作者将跳过: {}", e.getMessage());
            return Map.of();
        }
    }

    private static List<Long> single(Long id) {
        return (id == null || id <= 0) ? List.of() : List.of(id);
    }

    /**
     * 把「逗号/分号/空白分隔的 id 串」解析成 id 列表（支持多选）。
     * 非数字片段（如字段名）会被跳过，绝不抛异常。
     */
    private static List<Long> parseIds(Object v) {
        if (v == null) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : String.valueOf(v).split("[,，;；\\s]+")) {
            Long id = parseId(part);
            if (id != null && id > 0) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static Long parseId(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
