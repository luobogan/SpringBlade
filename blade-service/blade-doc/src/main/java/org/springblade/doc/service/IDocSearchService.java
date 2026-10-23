package org.springblade.doc.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.doc.entity.DocDetail;

/**
 * 文档检索服务（记录级权限可见性，等价 E9 DocShareUtil.getOnlyHasRight）。
 */
public interface IDocSearchService {

    /**
     * 分页检索：按「我创建的 + 共享给我 + 我管理的目录」裁剪；超管/内容管理员返回租户内全部。
     */
    IPage<DocDetail> page(long current, long size, String keyword, Long secCategoryId);
}
