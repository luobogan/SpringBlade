package org.springblade.mall.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 微信支付响应 DTO
 * 
 * @author youpinmall
 * @date 2026-03-05
 */
public class WechatPayResponseDTO {

    /**
     * 支付单号
     */
    private String paymentNo;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 订单总金额（分）
     */
    @JsonProperty("total_fee")
    private Integer totalAmount;

    /**
     * 支付过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 小程序 AppID
     */
    @JsonProperty("appId")
    private String appId;

    /**
     * 时间戳
     */
    @JsonProperty("timeStamp")
    private String timeStamp;

    /**
     * 随机字符串
     */
    @JsonProperty("nonceStr")
    private String nonceStr;

    /**
     * 订单详情扩展字符串
     */
    @JsonProperty("package")
    private String packageValue;

    /**
     * 签名类型
     */
    @JsonProperty("signType")
    private String signType;

    /**
     * 签名
     */
    @JsonProperty("paySign")
    private String paySign;

    /**
     * 扫码支付 URL（Native 支付）
     */
    private String codeUrl;

    // Getter 和 Setter 方法

    public String getPaymentNo() {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Integer totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getNonceStr() {
        return nonceStr;
    }

    public void setNonceStr(String nonceStr) {
        this.nonceStr = nonceStr;
    }

    public String getPackageValue() {
        return packageValue;
    }

    public void setPackageValue(String packageValue) {
        this.packageValue = packageValue;
    }

    public String getSignType() {
        return signType;
    }

    public void setSignType(String signType) {
        this.signType = signType;
    }

    public String getPaySign() {
        return paySign;
    }

    public void setPaySign(String paySign) {
        this.paySign = paySign;
    }

    public String getCodeUrl() {
        return codeUrl;
    }

    public void setCodeUrl(String codeUrl) {
        this.codeUrl = codeUrl;
    }
}




