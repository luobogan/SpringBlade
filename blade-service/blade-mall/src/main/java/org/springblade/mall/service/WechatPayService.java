package org.springblade.mall.service;

import org.springblade.mall.dto.WechatPayRequestDTO;
import org.springblade.mall.dto.WechatPayResponseDTO;

import java.math.BigDecimal;

/**
 * 微信支付服务接口
 * 
 * @author youpinmall
 * @date 2026-03-05
 */
public interface WechatPayService {

    /**
     * 创建微信支付订单
     * 
     * @param userId     用户 ID
     * @param requestDTO 微信支付请求参数
     * @return 微信支付参数
     */
    WechatPayResponseDTO createWechatPayOrder(Long userId, WechatPayRequestDTO requestDTO);

    /**
     * 查询微信支付订单状态
     * 
     * @param paymentNo 支付单号
     * @return 支付状态
     */
    String queryWechatPayStatus(String paymentNo);

    /**
     * 处理微信支付回调通知
     * 
     * @param body      回调体
     * @param signature 签名
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param serial    证书序列号
     * @return 回调响应
     */
    String handleWechatPayCallback(String body, String signature, String timestamp, 
                                   String nonce, String serial);

    /**
     * 微信退款
     * 
     * @param paymentNo    支付单号
     * @param refundAmount 退款金额
     * @param reason       退款原因
     * @return 退款单号
     */
    String wechatRefund(String paymentNo, BigDecimal refundAmount, String reason);

    /**
     * 查询微信退款状态
     * 
     * @param refundNo 退款单号
     * @return 退款状态
     */
    String queryWechatRefundStatus(String refundNo);
}



