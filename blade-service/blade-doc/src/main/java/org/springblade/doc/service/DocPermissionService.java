package org.springblade.doc.service;

/**
 * 文档级权限判定（对齐《落地规范》§3 的 DocViewPermission 等价物）。
 *
 * <p>统一收口「能不能动这一条数据」：控制器用 {@code @PreAuth} 管「能不能调这类接口」，
 * 本服务管「这一条归不归你」，两者缺一不可（防 DOC-P0-3 注释权限 / WF-N9 条件≠权限）。
 * 默认 fail-closed：无任一路径命中即拒绝。</p>
 */
public interface DocPermissionService {

    /** 可读（查看级 shareLevel>=1） */
    boolean canRead(Long docId);

    /** 可管理/编辑（编辑级 shareLevel>=2，或创建人/管理员） */
    boolean canManage(Long docId);

    /** 可下载 */
    boolean canDownload(Long docId);
}
