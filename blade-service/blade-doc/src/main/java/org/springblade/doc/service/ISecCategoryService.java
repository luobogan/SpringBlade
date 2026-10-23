package org.springblade.doc.service;

import org.springblade.doc.entity.DocSecCategory;

import java.util.List;

/**
 * 文档目录服务（对齐《泛微E9-内容引擎-SpringBlade落地规范》§3 目录管理）。
 */
public interface ISecCategoryService {

    /**
     * 目录列表（树形由前端按 parentId 组装）。
     * 分部隔离（sub_company_id）由 DataScope（blade_scope_data）统一追加，业务代码零硬编码。
     */
    List<DocSecCategory> listTree();

    /** 新建/更新目录 */
    DocSecCategory submit(DocSecCategory category);

    /** 删除目录（有子目录则 fail-closed 拒绝） */
    boolean remove(Long id);
}
