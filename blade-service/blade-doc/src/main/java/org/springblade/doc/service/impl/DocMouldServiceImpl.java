package org.springblade.doc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.doc.entity.DocMould;
import org.springblade.doc.mapper.DocMouldMapper;
import org.springblade.doc.service.IDocMouldService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 显示模板服务实现。
 *
 * <p><b>DOC-P0-5 修复</b>：E9 模板删除用 {@code '(' + id + ') and id in(...)} 字符串拼接（括号闭合注入）。
 * 本实现批量删一律走 QueryWrapper {@code .in(id, ids)} 参数化，从根上消除注入面。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocMouldServiceImpl implements IDocMouldService {

    private final DocMouldMapper mouldMapper;

    @Override
    public List<DocMould> list() {
        return mouldMapper.selectList(Wrappers.<DocMould>lambdaQuery()
            .orderByDesc(DocMould::getIsDefault, DocMould::getId));
    }

    @Override
    public DocMould save(DocMould mould) {
        if (mould == null || mould.getMouldName() == null || mould.getMouldName().isBlank()) {
            throw new ServiceException("模板名不能为空");
        }
        if (mould.getId() == null) {
            mouldMapper.insert(mould);
        } else {
            mouldMapper.updateById(mould);
        }
        return mould;
    }

    @Override
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("待删除ID不能为空");
        }
        // ★ DOC-P0-5：参数化 in (?) 批量删，禁用字符串拼接
        return mouldMapper.delete(Wrappers.<DocMould>lambdaQuery().in(DocMould::getId, ids));
    }
}
