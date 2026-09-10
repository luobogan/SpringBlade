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

}
