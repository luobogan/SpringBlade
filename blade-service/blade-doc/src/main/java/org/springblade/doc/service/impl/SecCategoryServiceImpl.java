package org.springblade.doc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.doc.entity.DocSecCategory;
import org.springblade.doc.mapper.DocSecCategoryMapper;
import org.springblade.doc.service.ISecCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文档目录服务实现（Mapper 直连范式，对齐 blade-workflow 既有 Service）。
 *
 * <p>全部走 QueryWrapper 参数化（守 G2 / DOC-P0-2 SQL 拼接）；
 * 分部隔离 sub_company_id 由 DataScope 追加，不在此手写。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecCategoryServiceImpl implements ISecCategoryService {

    private final DocSecCategoryMapper categoryMapper;

    @Override
    public List<DocSecCategory> listTree() {
        return categoryMapper.selectList(Wrappers.<DocSecCategory>lambdaQuery()
            .orderByAsc(DocSecCategory::getParentId, DocSecCategory::getId));
    }

    @Override
    public DocSecCategory submit(DocSecCategory category) {
        if (category == null || category.getName() == null || category.getName().isBlank()) {
            throw new ServiceException("目录名不能为空");
        }
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getId() == null) {
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
        return category;
    }

    @Override
    public boolean remove(Long id) {
        if (id == null) {
            throw new ServiceException("目录ID不能为空");
        }
        List<DocSecCategory> children = categoryMapper.selectList(
            Wrappers.<DocSecCategory>lambdaQuery().eq(DocSecCategory::getParentId, id));
        if (!children.isEmpty()) {
            throw new ServiceException("存在下级目录，禁止删除");
        }
        return categoryMapper.deleteById(id) > 0;
    }
}
