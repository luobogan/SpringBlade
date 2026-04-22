package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springblade.mall.dto.CategoryParamTemplateDTO;
import org.springblade.mall.entity.CategoryParamTemplate;
import org.springblade.mall.mapper.CategoryParamTemplateMapper;
import org.springblade.mall.service.CategoryParamTemplateService;
import org.springblade.mall.vo.CategoryParamTemplateVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类参数模板服务实现
 */
@Service
public class CategoryParamTemplateServiceImpl implements CategoryParamTemplateService {

    private static final Logger log = LoggerFactory.getLogger(CategoryParamTemplateServiceImpl.class);

    @Autowired
    private CategoryParamTemplateMapper categoryParamTemplateMapper;

    @Override
    public CategoryParamTemplateVO createParamTemplate(CategoryParamTemplateDTO dto) {
        log.info("创建参数模板，dto: {}", dto);

        CategoryParamTemplate paramTemplate = new CategoryParamTemplate();
        BeanUtils.copyProperties(dto, paramTemplate);
        paramTemplate.setCreatedAt(LocalDateTime.now());
        paramTemplate.setUpdatedAt(LocalDateTime.now());

        categoryParamTemplateMapper.insert(paramTemplate);

        return convertToVO(paramTemplate);
    }

    @Override
    public CategoryParamTemplateVO updateParamTemplate(Long id, CategoryParamTemplateDTO dto) {
        log.info("更新参数模板，id: {}, dto: {}", id, dto);

        CategoryParamTemplate paramTemplate = categoryParamTemplateMapper.selectById(id);
        if (paramTemplate == null) {
            throw new RuntimeException("参数模板不存在");
        }

        BeanUtils.copyProperties(dto, paramTemplate);
        paramTemplate.setUpdatedAt(LocalDateTime.now());

        categoryParamTemplateMapper.updateById(paramTemplate);

        return convertToVO(paramTemplate);
    }

    @Override
    public void deleteParamTemplate(Long id) {
        log.info("删除参数模板，id: {}", id);

        CategoryParamTemplate paramTemplate = categoryParamTemplateMapper.selectById(id);
        if (paramTemplate == null) {
            throw new RuntimeException("参数模板不存在");
        }

        categoryParamTemplateMapper.deleteById(id);
    }

    @Override
    public CategoryParamTemplateVO getParamTemplateById(Long id) {
        log.info("获取参数模板详情，id: {}", id);

        CategoryParamTemplate paramTemplate = categoryParamTemplateMapper.selectById(id);
        if (paramTemplate == null) {
            throw new RuntimeException("参数模板不存在");
        }

        return convertToVO(paramTemplate);
    }

    @Override
    public List<CategoryParamTemplateVO> getParamTemplatesByCategoryId(Long categoryId) {
        log.info("根据分类ID获取参数模板列表，categoryId: {}", categoryId);

        List<CategoryParamTemplate> paramTemplates = categoryParamTemplateMapper.selectList(
                new LambdaQueryWrapper<CategoryParamTemplate>()
                        .eq(CategoryParamTemplate::getCategoryId, categoryId)
                        .orderByAsc(CategoryParamTemplate::getSortOrder)
        );

        return paramTemplates.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<CategoryParamTemplateVO> getParamTemplatePage(Long categoryId, int page, int pageSize) {
        log.info("分页查询参数模板，categoryId: {}, page: {}, pageSize: {}", categoryId, page, pageSize);

        Page<CategoryParamTemplate> pageParam = new Page<>(page, pageSize);

        LambdaQueryWrapper<CategoryParamTemplate> queryWrapper =
                new LambdaQueryWrapper<CategoryParamTemplate>()
                        .orderByAsc(CategoryParamTemplate::getSortOrder);

        if (categoryId != null) {
            queryWrapper.eq(CategoryParamTemplate::getCategoryId, categoryId);
        }

        IPage<CategoryParamTemplate> paramTemplatePage = categoryParamTemplateMapper.selectPage(pageParam, queryWrapper);

        return paramTemplatePage.convert(this::convertToVO);
    }

    @Override
    public void copyParamTemplatesToCategory(Long sourceCategoryId, Long targetCategoryId) {
        log.info("复制参数模板到其他分类，sourceCategoryId: {}, targetCategoryId: {}", sourceCategoryId, targetCategoryId);

        // 获取源分类的参数模板列表
        List<CategoryParamTemplate> sourceParamTemplates = categoryParamTemplateMapper.selectList(
                new LambdaQueryWrapper<CategoryParamTemplate>()
                        .eq(CategoryParamTemplate::getCategoryId, sourceCategoryId)
        );

        // 复制到目标分类
        for (CategoryParamTemplate sourceParamTemplate : sourceParamTemplates) {
            CategoryParamTemplate targetParamTemplate = new CategoryParamTemplate();
            BeanUtils.copyProperties(sourceParamTemplate, targetParamTemplate);
            targetParamTemplate.setId(null);
            targetParamTemplate.setCategoryId(targetCategoryId);
            targetParamTemplate.setCreatedAt(LocalDateTime.now());
            targetParamTemplate.setUpdatedAt(LocalDateTime.now());

            categoryParamTemplateMapper.insert(targetParamTemplate);
        }
    }

    /**
     * 转换为VO
     */
    private CategoryParamTemplateVO convertToVO(CategoryParamTemplate paramTemplate) {
        CategoryParamTemplateVO vo = new CategoryParamTemplateVO();
        BeanUtils.copyProperties(paramTemplate, vo);
        return vo;
    }
}



