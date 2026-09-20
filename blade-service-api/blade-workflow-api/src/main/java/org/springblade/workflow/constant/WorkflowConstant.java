package org.springblade.workflow.constant;

import org.springblade.core.tool.constant.RoleConstant;

/**
 * 审批流程模块常量
 */
public interface WorkflowConstant {

    /**
     * 审批流程服务名称（Nacos 注册名）
     */
    String APPLICATION_WORKFLOW_NAME = "blade-workflow";

    /**
     * 审批流程 API 前缀
     */
    String API_PREFIX = "/api/blade-workflow";

    /**
     * 默认租户ID
     */
    String DEFAULT_TENANT_ID = "000000";

    /**
     * 审批流程业务角色别名（对齐 {@code blade_role.role_alias}）。
     *
     * <p>workflow 与 formmode 两侧控制器的 {@code @PreAuth} 必须统一使用该角色，
     * 否则会出现「一侧放行、一侧拦截」的鉴权不一致。</p>
     */
    String ROLE_WORKFLOW = "workflow";

    /**
     * {@code @PreAuth} 表达式：要求「流程管理员」角色。
     *
     * <p>同时放行 {@code administrator}（超级管理员），符合 BladeX「超管可访问一切」的惯例，
     * 避免把 000000 高级管理员挡在 formmode/workflow 管理界面之外。</p>
     */
    String HAS_ROLE_WORKFLOW = "hasRole('" + ROLE_WORKFLOW + "') or hasRole('" + RoleConstant.ADMINISTRATOR + "')";

    /**
     * {@code @PreAuth} 表达式：只要求「已登录」，不要求任何角色。
     *
     * <p><b>用途</b>：流程中心面向<b>全体员工</b>的接口——发起流程、我的请求、待办/已办、
     * 办理（同意/退回/转办/加签/催办）、表单渲染包、流程图与流转记录等。
     * 普通员工没有 {@code workflow} 角色也必须能走流程；{@link #HAS_ROLE_WORKFLOW}
     * 只保留给「建模 / 部署 / 版本 / 节点与出口配置 / 字段权限 / 测试 / 监控」等管理动作。</p>
     *
     * <p><b>为什么可以放开</b>：控制器上的角色门只解决「能不能调这类接口」，
     * 放开后「能不能动<b>这一条</b>数据」由服务层记录级校验兜底——
     * 办理类动作校验「任务办理人是否为当前用户」，实例读取校验「是否参与人」，
     * 见 {@code WfAuthUtil}。两者缺一不可：只有记录级校验时菜单/按钮权限管不住，
     * 只有角色门时普通员工进不来。</p>
     *
     * <p>取值来自 {@code AuthFun.hasAuth()}（等价于 {@code AuthUtil.hasAuth()}），
     * 与 {@code AuthAspect} 在注解全为空时的兜底分支同一语义。</p>
     */
    String HAS_AUTH = "hasAuth()";

    /**
     * 测试部署的流程 key 后缀：测试态部署用 {@code procKey + "__test"} 作为独立 key。
     *
     * <p>原因：Flowable 的 {@code startProcessInstanceByKey} 取「该 key 的<b>最新部署</b>」，
     * 若测试沿用正式 procKey，每次测试都会把正式版本顶下去（未清理时正式发起会跑
     * 「关闭校验 + 消毒过」的测试 BPMN）。独立 key 让两条路径在引擎里彻底隔离。</p>
     *
     * <p>使用方必须成对：{@code WfDefinitionServiceImpl#deployForTest} 用该 key 部署；
     * {@code WfTestServiceImpl} 发起时把 {@code StartProcessDTO.engineKey} 设为同一个 key。</p>
     */
    String TEST_DEPLOY_KEY_SUFFIX = "__test";

}
