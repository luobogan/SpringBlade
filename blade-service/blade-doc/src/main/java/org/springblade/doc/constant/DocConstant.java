package org.springblade.doc.constant;

import org.springblade.core.tool.constant.RoleConstant;

/**
 * 内容引擎（DocEngine）模块常量。
 *
 * <p>对齐《泛微E9-内容引擎-SpringBlade落地规范》§3/§4 的权限点命名
 * （doc:view / doc:edit / doc:publish / doc:download / doc:delete / doc:manage）。</p>
 */
public interface DocConstant {

    /** 内容引擎服务名称（Nacos 注册名，网关路由 /blade-doc/** -> lb://blade-doc） */
    String APPLICATION_DOC_NAME = "blade-doc";

    /** 默认租户ID */
    String DEFAULT_TENANT_ID = "000000";

    /** 内容管理角色别名（对齐 blade_role.role_alias） */
    String ROLE_DOC = "document";

    /**
     * {@code @PreAuth} 表达式：要求「内容管理员」角色（或超级管理员）。
     * 用于目录/模板/字段/应用设置等后台维护动作。
     */
    String HAS_ROLE_DOC = "hasRole('" + ROLE_DOC + "') or hasRole('" + RoleConstant.ADMINISTRATOR + "')";

    /**
     * {@code @PreAuth} 表达式：只要求「已登录」，不要求任何角色。
     * 面向全体员工的内容检索/详情/上传等；「能不能动这一条数据」由服务层
     * {@code canRead/canManage/canDownload} 记录级校验兜底（落地规范 §3）。
     */
    String HAS_AUTH = "hasAuth()";
}
