package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springblade.mall.dto.BatchImportDTO;
import org.springblade.mall.dto.BatchUpdateDTO;
import org.springblade.mall.dto.ProductDTO;
import org.springblade.mall.entity.Product;
import org.springblade.mall.entity.ProductChannel;
import org.springblade.mall.entity.SalesChannel;
import org.springblade.mall.mapper.ProductChannelMapper;
import org.springblade.mall.mapper.ProductMapper;
import org.springblade.mall.mapper.SalesChannelMapper;
import org.springblade.mall.service.DistributionService;
import org.springblade.mall.service.ProductService;
import org.springblade.mall.vo.ProductChannelVO;
import org.springblade.mall.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DistributionServiceImpl implements DistributionService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private SalesChannelMapper salesChannelMapper;

    @Autowired
    private ProductChannelMapper productChannelMapper;

    @Autowired
    private ProductService productService;

    @Override
    @Transactional
    public List<ProductVO> batchImport(BatchImportDTO dto) {
        List<ProductVO> results = new ArrayList<>();

        for (BatchImportDTO.ImportItem item : dto.getItems()) {
            ProductDTO productDTO = new ProductDTO();
            productDTO.setName(item.getName());
            productDTO.setDescription(item.getDescription());
            productDTO.setCategoryId(item.getCategoryId());
            productDTO.setBrandId(item.getBrandId());
            productDTO.setPrice(item.getPrice());
            productDTO.setOriginalPrice(item.getOriginalPrice());
            productDTO.setStock(item.getStock());
            productDTO.setMainImage(item.getImage());
            productDTO.setImages(item.getImages());
            productDTO.setColors(item.getColors());
            productDTO.setSizes(item.getSizes());
            productDTO.setStatus(1);

            ProductVO productVO = productService.createProduct(productDTO);
            results.add(productVO);
        }

        return results;
    }

    @Override
    @Transactional
    public void batchUpdate(BatchUpdateDTO dto) {
        if (dto.getProductIds() == null || dto.getProductIds().isEmpty()) {
            return;
        }

        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Product::getId, dto.getProductIds());

        if (dto.getStatus() != null) {
            Integer statusValue = "active".equals(dto.getStatus()) ? 1 : 0;
            updateWrapper.set(Product::getStatus, statusValue);
        }

        if (dto.getIsFeatured() != null) {
            updateWrapper.set(Product::getIsRecommend, dto.getIsFeatured() ? 1 : 0);
        }

        if (dto.getIsNew() != null) {
            updateWrapper.set(Product::getIsNew, dto.getIsNew() ? 1 : 0);
        }

        if (dto.getIsHot() != null) {
            updateWrapper.set(Product::getIsHot, dto.getIsHot() ? 1 : 0);
        }

        productMapper.update(null, updateWrapper);

        if (dto.getPriceAdjust() != null) {
            for (Long productId : dto.getProductIds()) {
                Product product = productMapper.selectById(productId);
                if (product != null) {
                    BigDecimal newPrice = product.getPrice().add(dto.getPriceAdjust());
                    if (newPrice.compareTo(BigDecimal.ZERO) > 0) {
                        product.setPrice(newPrice);
                        productMapper.updateById(product);
                    }
                }
            }
        }

        if (dto.getStockAdjust() != null) {
            for (Long productId : dto.getProductIds()) {
                Product product = productMapper.selectById(productId);
                if (product != null) {
                    int newStock = product.getStock() + dto.getStockAdjust();
                    product.setStock(Math.max(0, newStock));
                    productMapper.updateById(product);
                }
            }
        }
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        productMapper.deleteBatchIds(productIds);

        LambdaQueryWrapper<ProductChannel> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ProductChannel::getProductId, productIds);
        productChannelMapper.delete(wrapper);
    }

    @Override
    public List<SalesChannel> getChannels() {
        LambdaQueryWrapper<SalesChannel> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SalesChannel::getId);
        return salesChannelMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public SalesChannel createChannel(SalesChannel channel) {
        channel.setStatus(1);
        salesChannelMapper.insert(channel);
        return channel;
    }

    @Override
    @Transactional
    public void updateChannelStatus(Long channelId, Integer status) {
        SalesChannel channel = salesChannelMapper.selectById(channelId);
        if (channel != null) {
            channel.setStatus(status);
            salesChannelMapper.updateById(channel);
        }
    }

    @Override
    public List<ProductChannelVO> getProductChannels(Long productId) {
        LambdaQueryWrapper<ProductChannel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductChannel::getProductId, productId);
        List<ProductChannel> productChannels = productChannelMapper.selectList(wrapper);

        return productChannels.stream().map(pc -> {
            ProductChannelVO vo = new ProductChannelVO();
            vo.setProductId(pc.getProductId());
            vo.setChannelId(pc.getChannelId());
            vo.setChannelProductId(pc.getChannelProductId());
            vo.setChannelProductUrl(pc.getChannelProductUrl());
            vo.setChannelPrice(pc.getChannelPrice());
            vo.setChannelStock(pc.getChannelStock());
            vo.setStatus(pc.getStatus() != null && pc.getStatus() == 1 ? "active" : "inactive");

            SalesChannel channel = salesChannelMapper.selectById(pc.getChannelId());
            if (channel != null) {
                vo.setChannelName(channel.getName());
                vo.setChannelCode(channel.getCode());
                vo.setChannelIcon(channel.getIcon());
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void publishToChannel(Long productId, Long channelId) {
        LambdaQueryWrapper<ProductChannel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductChannel::getProductId, productId)
               .eq(ProductChannel::getChannelId, channelId);

        ProductChannel existing = productChannelMapper.selectOne(wrapper);
        if (existing == null) {
            Product product = productMapper.selectById(productId);
            if (product != null) {
                ProductChannel pc = new ProductChannel();
                pc.setProductId(productId);
                pc.setChannelId(channelId);
                pc.setChannelPrice(product.getPrice());
                pc.setChannelStock(product.getStock());
                pc.setStatus(1);
                productChannelMapper.insert(pc);
            }
        } else {
            existing.setStatus(1);
            productChannelMapper.updateById(existing);
        }
    }

    @Override
    @Transactional
    public void unpublishFromChannel(Long productId, Long channelId) {
        LambdaQueryWrapper<ProductChannel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductChannel::getProductId, productId)
               .eq(ProductChannel::getChannelId, channelId);

        ProductChannel pc = productChannelMapper.selectOne(wrapper);
        if (pc != null) {
            pc.setStatus(0);
            productChannelMapper.updateById(pc);
        }
    }

    @Override
    @Transactional
    public void batchPublishToChannels(List<Long> productIds, List<Long> channelIds) {
        for (Long productId : productIds) {
            for (Long channelId : channelIds) {
                publishToChannel(productId, channelId);
            }
        }
    }

    @Override
    @Transactional
    public void syncChannelProduct(Long productId, Long channelId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return;
        }

        LambdaQueryWrapper<ProductChannel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductChannel::getProductId, productId)
               .eq(ProductChannel::getChannelId, channelId);

        ProductChannel pc = productChannelMapper.selectOne(wrapper);
        if (pc != null) {
            pc.setChannelPrice(product.getPrice());
            pc.setChannelStock(product.getStock());
            productChannelMapper.updateById(pc);
        }
    }
}



