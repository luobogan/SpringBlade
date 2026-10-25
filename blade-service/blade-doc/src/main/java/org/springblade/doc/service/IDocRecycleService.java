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

    /**
     * 移入回收站（逻辑删除 + 写回收站记录）。
     *
     * <p>这是文档进入回收站的唯一入口：主表 is_deleted 置 1（检索不再返回），并登记
     * doc_recycle（doc_id/delete_user/delete_time）供列表展示与恢复。</p>
     *
     * @param docId 文档ID
     * @return 是否成功移入
     */
    boolean removeToRecycle(Long docId);
}
