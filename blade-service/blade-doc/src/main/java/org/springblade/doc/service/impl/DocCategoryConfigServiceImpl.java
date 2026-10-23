package org.springblade.doc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.doc.entity.DocSecCategoryApproveWfDetail;
import org.springblade.doc.entity.DocSecCategoryCoder;
import org.springblade.doc.entity.DocSecCategoryDocProperty;
import org.springblade.doc.mapper.DocSecCategoryApproveWfDetailMapper;
import org.springblade.doc.mapper.DocSecCategoryCoderMapper;
import org.springblade.doc.mapper.DocSecCategoryDocPropertyMapper;
import org.springblade.doc.service.IDocCategoryConfigService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 目录配置子页实现（Mapper 直连 + LambdaQueryWrapper 参数化，守 G2）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocCategoryConfigServiceImpl implements IDocCategoryConfigService {

    private final DocSecCategoryCoderMapper coderMapper;
    private final DocSecCategoryDocPropertyMapper propertyMapper;
    private final DocSecCategoryApproveWfDetailMapper approveMapper;

    @Override
    public List<DocSecCategoryCoder> listCoder(Long categoryId) {
        if (categoryId == null) {
            return Collections.emptyList();
        }
        return coderMapper.selectList(Wrappers.<DocSecCategoryCoder>lambdaQuery()
            .eq(DocSecCategoryCoder::getCategoryId, categoryId)
            .orderByAsc(DocSecCategoryCoder::getOrderNum));
    }

    @Override
    public DocSecCategoryCoder saveCoder(DocSecCategoryCoder e) {
        if (e == null) {
            throw new ServiceException("参数不能为空");
        }
        if (e.getId() == null) {
            coderMapper.insert(e);
        } else {
            coderMapper.updateById(e);
        }
        return e;
    }

    @Override
    public boolean deleteCoder(Long id) {
        return id != null && coderMapper.deleteById(id) > 0;
    }

    @Override
    public List<DocSecCategoryDocProperty> listProperty(Long categoryId) {
        if (categoryId == null) {
            return Collections.emptyList();
        }
        return propertyMapper.selectList(Wrappers.<DocSecCategoryDocProperty>lambdaQuery()
            .eq(DocSecCategoryDocProperty::getCategoryId, categoryId)
            .orderByAsc(DocSecCategoryDocProperty::getViewIndex));
    }

    @Override
    public DocSecCategoryDocProperty saveProperty(DocSecCategoryDocProperty e) {
        if (e == null) {
            throw new ServiceException("参数不能为空");
        }
        if (e.getId() == null) {
            propertyMapper.insert(e);
        } else {
            propertyMapper.updateById(e);
        }
        return e;
    }

    @Override
    public boolean deleteProperty(Long id) {
        return id != null && propertyMapper.deleteById(id) > 0;
    }

    @Override
    public List<DocSecCategoryApproveWfDetail> listApprove(Long categoryId) {
        if (categoryId == null) {
            return Collections.emptyList();
        }
        return approveMapper.selectList(Wrappers.<DocSecCategoryApproveWfDetail>lambdaQuery()
            .eq(DocSecCategoryApproveWfDetail::getCategoryId, categoryId));
    }

    @Override
    public DocSecCategoryApproveWfDetail saveApprove(DocSecCategoryApproveWfDetail e) {
        if (e == null) {
            throw new ServiceException("参数不能为空");
        }
        if (e.getId() == null) {
            approveMapper.insert(e);
        } else {
            approveMapper.updateById(e);
        }
        return e;
    }

    @Override
    public boolean deleteApprove(Long id) {
        return id != null && approveMapper.deleteById(id) > 0;
    }
}
