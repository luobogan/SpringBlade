package org.springblade.mall.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 微信支付请求 DTO
 * 
 * @author youpinmall
 * @date 2026-03-05
 */
@Data
public class WechatPayRequestDTO {

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 交易类型（JSAPI-小程序/公众号支付，NATIVE-扫码支付）
     */
    private String tradeType = "JSAPI";

    /**
     * 用户 OpenID（JSAPI 支付必填）
     */
    private String openid;
}




