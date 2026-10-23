package org.springblade.doc.service;

import org.springblade.doc.entity.DocRecycle;

import java.util.List;

/**
 * 回收站服务（对齐 E9 recycle_docdetail；权限点 DocumentRecycle:All → 内容管理员角色）。
 */
public interface IDocRecycleService {

    List<DocRecycle> list(Long secCategoryId);

    boolean restore(Long docId);

    boolean purge(Long docId);
}
