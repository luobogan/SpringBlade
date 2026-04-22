package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springblade.mall.dto.ProductRelationDTO;
import org.springblade.mall.entity.Product;
import org.springblade.mall.entity.ProductRelation;
import org.springblade.mall.mapper.ProductMapper;
import org.springblade.mall.mapper.ProductRelationMapper;
import org.springblade.mall.service.ProductRelationService;
import org.springblade.mall.vo.ProductRelationVO;
import org.springblade.mall.vo.ProductVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品关联服务实现类
 */
@Service
public class ProductRelationServiceImpl implements ProductRelationService {

    @Autowired
    private ProductRelationMapper productRelationMapper;

    @Autowired
    private ProductMapper productMapper;

    @Override
    public List<ProductRelationVO> getProductRelations(Long productId, Integer type) {
        QueryWrapper<ProductRelation> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", productId);
        if (type != null) {
            wrapper.eq("type", type);
        }
        wrapper.orderByAsc("sort_order", "created_at");

        List<ProductRelation> relations = productRelationMapper.selectList(wrapper);
        return relations.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductRelationVO addProductRelation(ProductRelationDTO relationDTO) {
        // 检查是否已存在
        if (isRelationExists(relationDTO.getProductId(), relationDTO.getRelatedProductId(), relationDTO.getType())) {
            throw new RuntimeException("该关联已存在");
        }

        // 检查商品是否存在
        Product product = productMapper.selectById(relationDTO.getProductId());
        if (product == null) {
            throw new RuntimeException("主商品不存在");
        }

        Product relatedProduct = productMapper.selectById(relationDTO.getRelatedProductId());
        if (relatedProduct == null) {
            throw new RuntimeException("关联商品不存在");
        }

        // 不能关联自己
        if (relationDTO.getProductId().equals(relationDTO.getRelatedProductId())) {
            throw new RuntimeException("不能关联商品自己");
        }

        ProductRelation relation = new ProductRelation();
        BeanUtils.copyProperties(relationDTO, relation);
        relation.setSortOrder(relationDTO.getSortOrder() != null ? relationDTO.getSortOrder() : 0);
        relation.setCreatedAt(LocalDateTime.now());

        productRelationMapper.insert(relation);
        return convertToVO(relation);
    }

    @Override
    @Transactional
    public List<ProductRelationVO> batchAddProductRelations(Long productId, List<ProductRelationDTO> relations) {
        List<ProductRelationVO> result = new ArrayList<>();
        for (ProductRelationDTO dto : relations) {
            dto.setProductId(productId);
            try {
                ProductRelationVO vo = addProductRelation(dto);
                result.add(vo);
            } catch (Exception e) {
                // 跳过已存在的关联
                System.out.println("跳过已存在的关联: " + e.getMessage());
            }
        }
        return result;
    }

    @Override
    @Transactional
    public ProductRelationVO updateProductRelation(Long id, ProductRelationDTO relationDTO) {
        ProductRelation relation = productRelationMapper.selectById(id);
        if (relation == null) {
            throw new RuntimeException("关联不存在");
        }

        BeanUtils.copyProperties(relationDTO, relation, "id", "productId", "createdAt");
        productRelationMapper.updateById(relation);
        return convertToVO(relation);
    }

    @Override
    @Transactional
    public void deleteProductRelation(Long id) {
        productRelationMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void batchDeleteProductRelations(List<Long> ids) {
        productRelationMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional
    public void clearProductRelations(Long productId, Integer type) {
        QueryWrapper<ProductRelation> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", productId);
        if (type != null) {
            wrapper.eq("type", type);
        }
        productRelationMapper.delete(wrapper);
    }

    @Override
    public boolean isRelationExists(Long productId, Long relatedProductId, Integer type) {
        QueryWrapper<ProductRelation> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", productId);
        wrapper.eq("related_product_id", relatedProductId);
        wrapper.eq("type", type);
        return productRelationMapper.selectCount(wrapper) > 0;
    }

    private ProductRelationVO convertToVO(ProductRelation relation) {
        ProductRelationVO vo = new ProductRelationVO();
        BeanUtils.copyProperties(relation, vo);

        // 设置关联类型文本
        switch (relation.getType()) {
            case 1:
                vo.setTypeText("相关商品");
                break;
            case 2:
                vo.setTypeText("搭配商品");
                break;
            case 3:
                vo.setTypeText("推荐商品");
                break;
            default:
                vo.setTypeText("未知");
        }

        // 查询关联商品信息
        Product relatedProduct = productMapper.selectById(relation.getRelatedProductId());
        if (relatedProduct != null) {
            ProductVO productVO = new ProductVO();
            BeanUtils.copyProperties(relatedProduct, productVO);
            vo.setRelatedProduct(productVO);
        }

        return vo;
    }
}



