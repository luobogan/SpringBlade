package org.springblade.doc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.entity.DocRecycle;
import org.springblade.doc.enums.DocStatus;
import org.springblade.doc.mapper.DocDetailMapper;
import org.springblade.doc.mapper.DocRecycleMapper;
import org.springblade.doc.service.IDocRecycleService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 回收站服务实现。
 *
 * <p><b>DET-P0-1 修复</b>：E9 回收站列表权限判断被整段注释，任意登录用户可拉取。
 * 本实现列表/恢复/彻底删除均由控制器 {@code @PreAuth(HAS_ROLE_DOC)} 门禁（内容管理员），
 * 服务层不再依赖「被注释」的判断。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocRecycleServiceImpl implements IDocRecycleService {

    private final DocRecycleMapper recycleMapper;
    private final DocDetailMapper detailMapper;

    @Override
    public List<DocRecycle> list(Long secCategoryId) {
        if (secCategoryId == null) {
            return recycleMapper.selectList(
                Wrappers.<DocRecycle>lambdaQuery().orderByDesc(DocRecycle::getDeleteTime));
        }
        // 按目录过滤：取该目录下所有文档的 docId，再过滤回收站（MP 自动带租户条件）
        List<DocDetail> docs = detailMapper.selectList(
            Wrappers.<DocDetail>lambdaQuery().eq(DocDetail::getSecCategoryId, secCategoryId));
        if (docs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> docIds = docs.stream().map(DocDetail::getId).collect(Collectors.toList());
        return recycleMapper.selectList(
            Wrappers.<DocRecycle>lambdaQuery()
                .in(DocRecycle::getDocId, docIds)
                .orderByDesc(DocRecycle::getDeleteTime));
    }

    @Override
    public boolean restore(Long docId) {
        if (docId == null) {
            throw new ServiceException("docId 不能为空");
        }
        int n = recycleMapper.delete(Wrappers.<DocRecycle>lambdaQuery().eq(DocRecycle::getDocId, docId));
        DocDetail doc = detailMapper.selectById(docId);
        if (doc != null) {
            doc.setDocStatus(DocStatus.EFFECTIVE.getCode());
            detailMapper.updateById(doc);
        }
        return n > 0;
    }

    @Override
    public boolean purge(Long docId) {
        if (docId == null) {
            throw new ServiceException("docId 不能为空");
        }
        return recycleMapper.delete(Wrappers.<DocRecycle>lambdaQuery().eq(DocRecycle::getDocId, docId)) > 0;
    }
}
