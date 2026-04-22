package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springblade.mall.dto.PromotionDTO;
import org.springblade.mall.dto.PromotionProductDTO;
import org.springblade.mall.entity.Product;
import org.springblade.mall.entity.ProductSku;
import org.springblade.mall.entity.Promotion;
import org.springblade.mall.entity.PromotionProduct;
import org.springblade.mall.mapper.ProductMapper;
import org.springblade.mall.mapper.ProductSkuMapper;
import org.springblade.mall.mapper.PromotionMapper;
import org.springblade.mall.mapper.PromotionProductMapper;
import org.springblade.mall.service.PromotionService;
import org.springblade.mall.utils.PromotionRuleParser;
import org.springblade.mall.vo.PromotionVO;
import org.springblade.mall.vo.PromotionProductVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 促销规则服务实现类
 */
@Service
public class PromotionServiceImpl implements PromotionService {

    @Autowired
    private PromotionMapper promotionMapper;

    @Autowired
    private PromotionProductMapper promotionProductMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Override
    @Transactional
    public PromotionVO createPromotion(PromotionDTO promotionDTO) {
        Promotion promotion = new Promotion();
        BeanUtils.copyProperties(promotionDTO, promotion);
        promotion.setStatus(0); // 默认未开始
        promotion.setCreatedAt(LocalDateTime.now());
        promotion.setUpdatedAt(LocalDateTime.now());

        promotionMapper.insert(promotion);
        return convertToVO(promotion);
    }

    @Override
    @Transactional
    public PromotionVO updatePromotion(Long id, PromotionDTO promotionDTO) {
        Promotion promotion = promotionMapper.selectById(id);
        if (promotion == null) {
            throw new RuntimeException("促销规则不存在");
        }

        BeanUtils.copyProperties(promotionDTO, promotion, "id", "createdAt");
        promotion.setUpdatedAt(LocalDateTime.now());

        promotionMapper.updateById(promotion);
        return convertToVO(promotion);
    }

    @Override
    @Transactional
    public void deletePromotion(Long id) {
        // 删除关联的促销商品
        QueryWrapper<PromotionProduct> wrapper = new QueryWrapper<>();
        wrapper.eq("promotion_id", id);
        promotionProductMapper.delete(wrapper);

        // 删除促销规则
        promotionMapper.deleteById(id);
    }

    @Override
    public PromotionVO getPromotionById(Long id) {
        Promotion promotion = promotionMapper.selectById(id);
        if (promotion == null) {
            return null;
        }
        return convertToVO(promotion);
    }

    @Override
    public List<PromotionVO> getPromotionList(Integer type, Integer status) {
        QueryWrapper<Promotion> wrapper = new QueryWrapper<>();
        
        if (type != null) {
            wrapper.eq("type", type);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        
        wrapper.orderByDesc("created_at");
        List<Promotion> promotions = promotionMapper.selectList(wrapper);
        
        return promotions.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PromotionProductVO addPromotionProduct(PromotionProductDTO promotionProductDTO) {
        PromotionProduct promotionProduct = new PromotionProduct();
        BeanUtils.copyProperties(promotionProductDTO, promotionProduct);
        promotionProduct.setCreatedAt(LocalDateTime.now());

        promotionProductMapper.insert(promotionProduct);
        return convertToProductVO(promotionProduct);
    }

    @Override
    @Transactional
    public List<PromotionProductVO> batchAddPromotionProducts(Long promotionId, List<PromotionProductDTO> products) {
        return products.stream()
                .map(dto -> {
                    dto.setPromotionId(promotionId);
                    return addPromotionProduct(dto);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePromotionProduct(Long id) {
        promotionProductMapper.deleteById(id);
    }

    @Override
    public List<PromotionProductVO> getPromotionProducts(Long promotionId) {
        QueryWrapper<PromotionProduct> wrapper = new QueryWrapper<>();
        wrapper.eq("promotion_id", promotionId);
        wrapper.orderByAsc("sort_order");
        
        List<PromotionProduct> products = promotionProductMapper.selectList(wrapper);
        return products.stream()
                .map(this::convertToProductVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updatePromotionStatus(Long id, Integer status) {
        Promotion promotion = promotionMapper.selectById(id);
        if (promotion == null) {
            throw new RuntimeException("促销规则不存在");
        }

        promotion.setStatus(status);
        promotion.setUpdatedAt(LocalDateTime.now());
        promotionMapper.updateById(promotion);
    }

    @Override
    @Transactional
    public void applyPromotionToProducts(Long promotionId) {
        Promotion promotion = promotionMapper.selectById(promotionId);
        if (promotion == null) {
            throw new RuntimeException("促销规则不存在");
        }

        // 解析促销规则
        PromotionRuleParser.PromotionRule rule = PromotionRuleParser.parse(promotion.getRules(), promotion.getType());
        if (rule == null) {
            throw new RuntimeException("促销规则解析失败");
        }

        // 获取促销商品列表
        QueryWrapper<PromotionProduct> wrapper = new QueryWrapper<>();
        wrapper.eq("promotion_id", promotionId);
        List<PromotionProduct> promotionProducts = promotionProductMapper.selectList(wrapper);

        for (PromotionProduct pp : promotionProducts) {
            // 更新商品促销信息
            Product product = productMapper.selectById(pp.getProductId());
            if (product != null) {
                product.setPromotionId(promotionId);
                
                // 计算促销价格
                BigDecimal promotionPrice;
                if (pp.getPromotionPrice() != null) {
                    // 使用指定的促销价格
                    promotionPrice = pp.getPromotionPrice();
                } else {
                    // 根据规则计算促销价格
                    promotionPrice = PromotionRuleParser.calculatePromotionPrice(
                            product.getPrice(), rule, promotion.getType());
                }
                
                product.setPromotionPrice(promotionPrice);
                product.setPromotionEndTime(promotion.getEndTime());
                productMapper.updateById(product);
            }

            // 如果指定了SKU，更新SKU促销信息
            if (pp.getSkuId() != null) {
                ProductSku sku = productSkuMapper.selectById(pp.getSkuId());
                if (sku != null) {
                    sku.setPromotionId(promotionId);
                    
                    BigDecimal skuPromotionPrice;
                    if (pp.getPromotionPrice() != null) {
                        skuPromotionPrice = pp.getPromotionPrice();
                    } else {
                        skuPromotionPrice = PromotionRuleParser.calculatePromotionPrice(
                                sku.getPrice(), rule, promotion.getType());
                    }
                    
                    sku.setPromotionPrice(skuPromotionPrice);
                    productSkuMapper.updateById(sku);
                }
            }
        }
    }

    @Override
    @Transactional
    public void cancelPromotionFromProducts(Long promotionId) {
        // 清除商品的促销信息
        QueryWrapper<Product> productWrapper = new QueryWrapper<>();
        productWrapper.eq("promotion_id", promotionId);
        
        List<Product> products = productMapper.selectList(productWrapper);
        for (Product product : products) {
            product.setPromotionId(null);
            product.setPromotionPrice(null);
            product.setPromotionEndTime(null);
            productMapper.updateById(product);
        }

        // 清除SKU的促销信息
        QueryWrapper<ProductSku> skuWrapper = new QueryWrapper<>();
        skuWrapper.eq("promotion_id", promotionId);
        
        List<ProductSku> skus = productSkuMapper.selectList(skuWrapper);
        for (ProductSku sku : skus) {
            sku.setPromotionId(null);
            sku.setPromotionPrice(null);
            productSkuMapper.updateById(sku);
        }
    }

    private PromotionVO convertToVO(Promotion promotion) {
        PromotionVO vo = new PromotionVO();
        BeanUtils.copyProperties(promotion, vo);
        
        // 设置促销类型文本
        Integer type = promotion.getType();
        if (type != null) {
            switch (type) {
                case 1:
                    vo.setTypeText("满减");
                    break;
                case 2:
                    vo.setTypeText("折扣");
                    break;
                case 3:
                    vo.setTypeText("秒杀");
                    break;
                case 4:
                    vo.setTypeText("团购");
                    break;
                default:
                    vo.setTypeText("未知");
            }
        } else {
            vo.setTypeText("未知");
        }
        
        // 设置状态文本
        Integer status = promotion.getStatus();
        if (status != null) {
            switch (status) {
                case 0:
                    vo.setStatusText("未开始");
                    break;
                case 1:
                    vo.setStatusText("进行中");
                    break;
                case 2:
                    vo.setStatusText("已结束");
                    break;
                case 3:
                    vo.setStatusText("已停用");
                    break;
                default:
                    vo.setStatusText("未知");
            }
        } else {
            vo.setStatusText("未知");
        }
        
        return vo;
    }

    private PromotionProductVO convertToProductVO(PromotionProduct promotionProduct) {
        PromotionProductVO vo = new PromotionProductVO();
        BeanUtils.copyProperties(promotionProduct, vo);
        return vo;
    }
}



