package org.springblade.mall.service;

import org.springblade.mall.entity.Logistics;
import org.springblade.mall.vo.LogisticsVO;

/**
 * 物流信息Service
 */
public interface LogisticsService {
    /**
     * 根据订单ID获取物流信息
     */
    LogisticsVO getLogisticsByOrderId(Long orderId);

    /**
     * 创建物流信息
     */
    Logistics createLogistics(Logistics logistics);

    /**
     * 更新物流信息
     */
    Logistics updateLogistics(Logistics logistics);

    /**
     * 发货
     */
    Logistics shipOrder(Long orderId, String trackingNo, String logisticsCompany);

    /**
     * 更新物流状态
     */
    void updateLogisticsStatus(Long logisticsId, String status, String latestInfo);
}


