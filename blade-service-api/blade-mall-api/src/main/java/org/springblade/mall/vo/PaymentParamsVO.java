package org.springblade.mall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付参数VO（用于第三方支付跳转）
 */
@Data
public class PaymentParamsVO {

    /**
     * 支付单号
     */
    private String paymentNo;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 支付方式
     */
    private String payType;

    /**
     * 支付URL（微信/支付宝跳转链接）
     */
    private String paymentUrl;

    /**
     * 支付URL（微信/支付宝跳转链接）
     */
    private String payUrl;

    /**
     * 二维码URL（扫码支付时使用）
     */
    private String qrCode;

    /**
     * 二维码URL（扫码支付时使用）
     */
    private String qrCodeUrl;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 表单HTML（支付宝表单提交）
     */
    private String formHtml;

    /**
     * 预支付ID（微信支付使用）
     */
    private String prepayId;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 时间戳
     */
    private String timeStamp;

    /**
     * 随机字符串
     */
    private String nonceStr;

    /**
     * 订单详情扩展字符串
     */
    private String packageValue;

    /**
     * 签名类型
     */
    private String signType;

    /**
     * 签名
     */
    private String paySign;
}




