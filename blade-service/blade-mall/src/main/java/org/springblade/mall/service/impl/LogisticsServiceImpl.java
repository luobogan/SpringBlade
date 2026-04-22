package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springblade.mall.entity.Logistics;
import org.springblade.mall.entity.Order;
import org.springblade.mall.mapper.LogisticsMapper;
import org.springblade.mall.mapper.OrderMapper;
import org.springblade.mall.service.LogisticsService;
import org.springblade.mall.vo.LogisticsVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 物流信息Service实现
 */
@Service
public class LogisticsServiceImpl implements LogisticsService {

    @Resource
    private LogisticsMapper logisticsMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 物流状态映射
     */
    private static final Map<String, String> STATUS_MAP = new HashMap<>();

    static {
        STATUS_MAP.put("PENDING", "待发货");
        STATUS_MAP.put("SHIPPED", "已发货");
        STATUS_MAP.put("IN_TRANSIT", "运输中");
        STATUS_MAP.put("DELIVERED", "已送达");
        STATUS_MAP.put("SIGNED", "已签收");
    }

    @Override
    public LogisticsVO getLogisticsByOrderId(Long orderId) {
        Logistics logistics = logisticsMapper.selectByOrderId(orderId);
        if (logistics == null) {
            return null;
        }

        LogisticsVO logisticsVO = new LogisticsVO();
        BeanUtils.copyProperties(logistics, logisticsVO);
        logisticsVO.setStatusText(STATUS_MAP.getOrDefault(logistics.getLogisticsStatus(), logistics.getLogisticsStatus()));

        // 解析物流轨迹
        if (logistics.getTrackingDetails() != null) {
            try {
                LogisticsVO.TrackingDetail[] trackingDetails = objectMapper.readValue(
                        logistics.getTrackingDetails(), LogisticsVO.TrackingDetail[].class);
                logisticsVO.setTrackingDetails(trackingDetails);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        return logisticsVO;
    }

    @Override
    public Logistics createLogistics(Logistics logistics) {
        logistics.setCreatedAt(LocalDateTime.now());
        logistics.setUpdatedAt(LocalDateTime.now());
        logisticsMapper.insert(logistics);
        return logistics;
    }

    @Override
    public Logistics updateLogistics(Logistics logistics) {
        logistics.setUpdatedAt(LocalDateTime.now());
        logisticsMapper.updateById(logistics);
        return logistics;
    }

    @Override
    public Logistics shipOrder(Long orderId, String trackingNo, String logisticsCompany) {
        // 查找或创建物流信息
        Logistics logistics = logisticsMapper.selectByOrderId(orderId);
        if (logistics == null) {
            logistics = new Logistics();
            logistics.setOrderId(orderId);
        }

        logistics.setTrackingNo(trackingNo);
        logistics.setLogisticsCompany(logisticsCompany);
        logistics.setLogisticsStatus("SHIPPED");
        logistics.setLatestInfo("【" + logisticsCompany + "】您的订单已发货，物流单号：" + trackingNo);

        // 创建初始物流轨迹
        LogisticsVO.TrackingDetail detail = new LogisticsVO.TrackingDetail();
        detail.setTime(LocalDateTime.now());
        detail.setDescription(logistics.getLatestInfo());
        detail.setLocation("商家");

        try {
            logistics.setTrackingDetails(objectMapper.writeValueAsString(new LogisticsVO.TrackingDetail[]{detail}));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        if (logistics.getId() == null) {
            createLogistics(logistics);
        } else {
            updateLogistics(logistics);
        }

        // 更新订单状态
        Order order = orderMapper.selectById(orderId);
        if (order != null) {
            order.setOrderStatus("SHIPPED");
            order.setShippingTime(LocalDateTime.now());
            order.setTrackingNo(trackingNo);
            orderMapper.updateById(order);
        }

        return logistics;
    }

    @Override
    public void updateLogisticsStatus(Long logisticsId, String status, String latestInfo) {
        Logistics logistics = logisticsMapper.selectById(logisticsId);
        if (logistics != null) {
            logistics.setLogisticsStatus(status);
            logistics.setLatestInfo(latestInfo);
            logistics.setUpdatedAt(LocalDateTime.now());

            // 添加新的物流轨迹
            try {
                LogisticsVO.TrackingDetail[] existingDetails = new LogisticsVO.TrackingDetail[0];
                if (logistics.getTrackingDetails() != null) {
                    existingDetails = objectMapper.readValue(
                            logistics.getTrackingDetails(), LogisticsVO.TrackingDetail[].class);
                }

                LogisticsVO.TrackingDetail[] newDetails = new LogisticsVO.TrackingDetail[existingDetails.length + 1];
                System.arraycopy(existingDetails, 0, newDetails, 0, existingDetails.length);

                LogisticsVO.TrackingDetail detail = new LogisticsVO.TrackingDetail();
                detail.setTime(LocalDateTime.now());
                detail.setDescription(latestInfo);
                newDetails[existingDetails.length] = detail;

                logistics.setTrackingDetails(objectMapper.writeValueAsString(newDetails));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            logisticsMapper.updateById(logistics);
        }
    }
}



