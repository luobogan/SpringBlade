package org.springblade.doc.service;

import org.springblade.doc.entity.DocMould;

import java.util.List;

/**
 * 显示模板服务。
 */
public interface IDocMouldService {

    List<DocMould> list();

    DocMould save(DocMould mould);

    /** 批量删除（参数化，修 DOC-P0-5） */
    int batchDelete(java.util.List<Long> ids);
}
