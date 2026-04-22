package org.springblade.mall.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物流信息VO
 */
@Data
public class LogisticsVO {
    /**
     * 物流ID
     */
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 物流单号
     */
    private String trackingNo;

    /**
     * 物流公司
     */
    private String logisticsCompany;

    /**
     * 物流状态
     */
    private String status;

    /**
     * 物流状态中文
     */
    private String statusText;

    /**
     * 最新物流信息
     */
    private String latestInfo;

    /**
     * 物流轨迹
     */
    private TrackingDetail[] trackingDetails;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 物流轨迹详情
     */
    @Data
    public static class TrackingDetail {
        /**
         * 时间
         */
        private LocalDateTime time;

        /**
         * 描述
         */
        private String description;

        /**
         * 地点
         */
        private String location;
    }
}



