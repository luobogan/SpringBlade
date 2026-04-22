package org.springblade.mall.service;

import org.springblade.mall.dto.PaymentDTO;
import org.springblade.mall.dto.RefundDTO;
import org.springblade.mall.vo.PaymentMethodVO;
import org.springblade.mall.vo.PaymentVO;
import org.springblade.mall.vo.RefundVO;

import java.util.List;

/**
 * 支付服务接口
 */
public interface PaymentService {

    /**
     * 获取可用支付方式列表
     * @return 支付方式列表
     */
    List<PaymentMethodVO> getPaymentMethods();

    /**
     * 创建支付
     * @param userId 用户ID
     * @param paymentDTO 支付信息
     * @return 支付结果
     */
    PaymentVO createPayment(Long userId, PaymentDTO paymentDTO);

    /**
     * 执行支付（模拟支付）
     * @param paymentNo 支付单号
     * @return 支付结果
     */
    PaymentVO executePayment(String paymentNo);

    /**
     * 查询支付状态
     * @param paymentNo 支付单号
     * @return 支付信息
     */
    PaymentVO getPaymentStatus(String paymentNo);

    /**
     * 处理支付回调
     * @param paymentNo 支付单号
     * @param callbackData 回调数据
     * @return 处理结果
     */
    PaymentVO handlePaymentCallback(String paymentNo, String callbackData);

    /**
     * 申请退款
     * @param userId 用户ID
     * @param refundDTO 退款信息
     * @return 退款结果
     */
    RefundVO applyRefund(Long userId, RefundDTO refundDTO);

    /**
     * 处理退款回调
     * @param refundNo 退款单号
     * @param callbackData 回调数据
     * @return 处理结果
     */
    RefundVO handleRefundCallback(String refundNo, String callbackData);

    /**
     * 获取用户的支付记录
     * @param userId 用户ID
     * @return 支付记录列表
     */
    List<PaymentVO> getUserPayments(Long userId);

    /**
     * 根据订单号获取支付记录
     * @param orderNo 订单号
     * @return 支付记录
     */
    PaymentVO getPaymentByOrderNo(String orderNo);

    /**
     * 取消支付
     * @param paymentNo 支付单号
     * @return 取消结果
     */
    PaymentVO cancelPayment(String paymentNo);

    /**
     * 关闭过期支付订单
     * 定时任务调用
     */
    void closeExpiredPayments();
}



