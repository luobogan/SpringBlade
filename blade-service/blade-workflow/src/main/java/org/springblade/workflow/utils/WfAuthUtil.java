package org.springblade.workflow.utils;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.utils.Func;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.exception.WfAccessDeniedException;

/**
 * 流程模块「记录级」鉴权工具。
 *
 * <p><b>为什么需要它</b>：流程中心面向全体员工的接口（发起 / 待办 / 办理 / 我的请求 / 渲染包）
 * 只能要求「已登录」（{@link WorkflowConstant#HAS_AUTH}），不能再要求
 * {@code workflow} 角色——否则普通员工连提交申请都做不到。角色门一旦放开，
 * 「能不能动<b>这一条</b>数据」就必须由本类在服务层兜住，否则任何登录用户
 * 只要猜到 taskId / instId 就能审批或读取别人的单据。</p>
 *
 * <p><b>分工</b>：</p>
 * <ul>
 *   <li>控制器注解：管「能不能调这类接口」（粗粒度、按角色）；</li>
 *   <li>本类：管「能不能动这一条数据」（细粒度、按本人/参与人），失败抛
 *       {@link WfAccessDeniedException}（403 + 明确原因）。</li>
 * </ul>
 *
 * <p>身份取自 token（{@code SecureUtil.getUser()}）：{@code role_name} 是登录时写入的
 * <b>角色别名</b>列表（逗号分隔），所以「改了角色要重新登录」同样适用于这里的管理员判定。</p>
 */
@Slf4j
public final class WfAuthUtil {

    /**
     * 「所有人」占位（系统自动审批、超时自动通过等无登录上下文的场景）
     */
    private static final Long SYSTEM = 0L;

    private WfAuthUtil() {
    }

    /**
     * 当前登录用户ID（无登录上下文返回 null，例如定时任务）
     */
    public static Long userId() {
        return SecureUtil.getUserId();
    }

    /**
     * 「所有人」占位 id（0）。
     *
     * <p>供无登录上下文的系统动作记录操作人：超时自动通过、异常兜底自动流转、子流程自动推进等，
     * 使 {@code wf_approval_log.operator} 有确定值（{@code secureUtil.getUserId()} 在定时任务里为 null）。</p>
     */
    public static Long systemId() {
        return SYSTEM;
    }

    /**
     * 当前用户是否流程管理员（角色别名 {@code workflow} 或 {@code administrator}）
     */
    public static boolean isAdmin() {
        BladeUser user = SecureUtil.getUser();
        if (user == null || Func.isBlank(user.getRoleName())) {
            return false;
        }
        for (String role : Func.toStrArray(user.getRoleName())) {
            if (WorkflowConstant.ROLE_WORKFLOW.equals(role) || RoleConstant.ADMINISTRATOR.equals(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 当前用户是否管理员，或目标人就是自己
     *
     * @param target 目标人ID（发起人 / 办理人等）
     */
    public static boolean isSelfOrAdmin(Long target) {
        if (isAdmin()) {
            return true;
        }
        Long me = userId();
        return me != null && target != null && me.equals(target);
    }

    /**
     * 当前用户能否办理该任务：任务办理人是自己，或自己是代理链上的原处理人，或管理员。
     *
     * <p>管理员放行是刻意的：流程管理员需要能替人处理卡住的单子（催办/转办/兜底审批），
     * 否则管理员角色在流程上反而比普通员工更受限。</p>
     */
    public static boolean canOperateTask(WfTask task) {
        if (task == null) {
            return false;
        }
        if (isAdmin()) {
            return true;
        }
        Long me = userId();
        if (me == null) {
            return false;
        }
        return me.equals(task.getAssignee()) || me.equals(task.getOriginalUser());
    }

    /**
     * 断言「可以办理该任务」，否则抛 403
     *
     * @param action 动作名（拼进提示语，如「同意」「退回」「转办」）
     */
    public static void requireOperateTask(WfTask task, String action) {
        if (canOperateTask(task)) {
            return;
        }
        Long me = userId();
        log.warn("[blade-workflow] 越权拦截：{} 非本人任务. taskId={}, assignee={}, current={}",
            action, task == null ? null : task.getId(), task == null ? null : task.getAssignee(), me);
        throw new WfAccessDeniedException("无权" + action + "：只能处理分配给自己的待办（流程管理员除外）");
    }

    /**
     * 断言「本人或管理员」，否则抛 403
     *
     * @param target 目标人ID（如流程发起人）
     * @param action 动作名（如「撤回」「终止」）
     */
    public static void requireSelfOrAdmin(Long target, String action) {
        if (isSelfOrAdmin(target)) {
            return;
        }
        log.warn("[blade-workflow] 越权拦截：{} 非本人. target={}, current={}", action, target, userId());
        throw new WfAccessDeniedException("无权" + action + "：只有发起人本人或流程管理员可以操作");
    }

    /**
     * 断言「本人或管理员」——按实例发起人判定，否则抛 403
     *
     * <p>实例不存在时抛的是普通业务异常（400「流程实例不存在」），保持与原行为一致；
     * 只有「存在但不属于你」才是 403 越权。</p>
     */
    public static void requireStarterOrAdmin(WfInstance inst, String action) {
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        requireSelfOrAdmin(inst.getStarter(), action);
    }

    /**
     * 查询目标人收口：非管理员一律只能查自己，管理员可按入参查任意人（为空则查自己）。
     *
     * <p>用于「我的待办 / 我的已办 / 待办角标」这类带 {@code assignee} 入参的查询，
     * 避免普通用户传别人的 userId 看别人的待办。</p>
     */
    public static Long resolveSelfIfNotAdmin(Long requested) {
        Long me = userId();
        if (!isAdmin()) {
            return me;
        }
        return requested == null ? me : requested;
    }

    /**
     * 是否系统占位操作者（自动审批 / 定时任务）
     */
    public static boolean isSystem(Long operator) {
        return SYSTEM.equals(operator);
    }

}
