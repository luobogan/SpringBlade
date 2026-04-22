package org.springblade.mall.service;

import org.springblade.mall.dto.BatchImportDTO;
import org.springblade.mall.dto.BatchUpdateDTO;
import org.springblade.mall.entity.SalesChannel;
import org.springblade.mall.vo.ProductChannelVO;
import org.springblade.mall.vo.ProductVO;

import java.util.List;

/**
 * 铺货商品服务接口
 */
public interface DistributionService {
    
    List<ProductVO> batchImport(BatchImportDTO dto);
    
    void batchUpdate(BatchUpdateDTO dto);
    
    void batchDelete(List<Long> productIds);
    
    List<SalesChannel> getChannels();
    
    SalesChannel createChannel(SalesChannel channel);
    
    void updateChannelStatus(Long channelId, Integer status);
    
    List<ProductChannelVO> getProductChannels(Long productId);
    
    void publishToChannel(Long productId, Long channelId);
    
    void unpublishFromChannel(Long productId, Long channelId);
    
    void batchPublishToChannels(List<Long> productIds, List<Long> channelIds);
    
    void syncChannelProduct(Long productId, Long channelId);
}



