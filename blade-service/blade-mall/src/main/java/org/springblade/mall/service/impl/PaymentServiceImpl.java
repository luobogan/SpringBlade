package org.springblade.mall.service.impl;

import org.springblade.mall.dto.PaymentDTO;
import org.springblade.mall.dto.RefundDTO;
import org.springblade.mall.entity.Order;
import org.springblade.mall.entity.Payment;
import org.springblade.mall.entity.Refund;
import org.springblade.mall.mapper.OrderMapper;
import org.springblade.mall.mapper.PaymentMapper;
import org.springblade.mall.mapper.RefundMapper;
import org.springblade.mall.service.PaymentService;
import org.springblade.core.tool.utils.DigestUtil;
import org.springblade.mall.utils.PaymentRiskControl;
import org.springblade.mall.utils.SmsService;
import org.springblade.mall.vo.PaymentMethodVO;
import org.springblade.mall.vo.PaymentParamsVO;
import org.springblade.mall.vo.PaymentVO;
import org.springblade.mall.vo.RefundVO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.Func;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 支付服务实现类
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final OrderMapper orderMapper;
    private final PaymentRiskControl riskControl;
    private SmsService smsService; // 可选依赖

    // 支付单号前缀
    private static final String PAYMENT_PREFIX = "PAY";
    // 退款单号前缀
    private static final String REFUND_PREFIX = "REF";

    @Override
    public List<PaymentMethodVO> getPaymentMethods() {
        List<PaymentMethodVO> methods = new ArrayList<>();

        // 余额支付
        PaymentMethodVO balance = new PaymentMethodVO();
        balance.setCode("BALANCE");
        balance.setName("余额支付");
        balance.setIcon("https://placehold.co/40x40/4CAF50/FFFFFF?text=余额");
        balance.setFeeRate(BigDecimal.ZERO);
        balance.setEnabled(true);
        balance.setRecommended(true);
        balance.setSort(1);
        balance.setDescription("使用账户余额支付，安全快捷");
        methods.add(balance);

        // 模拟微信支付
        PaymentMethodVO wechat = new PaymentMethodVO();
        wechat.setCode("WECHAT");
        wechat.setName("微信支付");
        wechat.setIcon("https://placehold.co/40x40/07C160/FFFFFF?text=微信");
        wechat.setFeeRate(BigDecimal.ZERO);
        wechat.setEnabled(true);
        wechat.setRecommended(false);
        wechat.setSort(2);
        wechat.setDescription("微信扫码支付（模拟）");
        methods.add(wechat);

        // 模拟支付宝
        PaymentMethodVO alipay = new PaymentMethodVO();
        alipay.setCode("ALIPAY");
        alipay.setName("支付宝");
        alipay.setIcon("https://placehold.co/40x40/1677FF/FFFFFF?text=支付");
        alipay.setFeeRate(BigDecimal.ZERO);
        alipay.setEnabled(true);
        alipay.setRecommended(false);
        alipay.setSort(3);
        alipay.setDescription("支付宝扫码支付（模拟）");
        methods.add(alipay);

        return methods;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO createPayment(Long userId, PaymentDTO paymentDTO) {
        // 1. 验证订单
        Order order = orderMapper.selectByOrderNo(paymentDTO.getOrderNo());
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不正确，无法支付");
        }
        if (order.getActualAmount().compareTo(paymentDTO.getAmount()) != 0) {
            throw new RuntimeException("支付金额与订单金额不符");
        }

        // 2. 检查是否已有支付记录
        List<Payment> existingPayments = paymentMapper.selectByOrderNo(paymentDTO.getOrderNo());
        for (Payment existing : existingPayments) {
            if ("PENDING".equals(existing.getStatus()) || "PROCESSING".equals(existing.getStatus())) {
                // 返回已有的支付记录
                return convertToVO(existing);
            }
        }

        // 3. 支付风控检查
        PaymentRiskControl.RiskCheckResult riskResult = riskControl.checkRisk(userId, paymentDTO.getAmount());
        if (!riskResult.isPassed()) {
            throw new RuntimeException(riskResult.getMessage());
        }

        // 4. 加密支付密码（如果提供）
        if (paymentDTO.getPayPassword() != null && !paymentDTO.getPayPassword().isEmpty()) {
            paymentDTO.setPayPassword(DigestUtil.encrypt(paymentDTO.getPayPassword()));
        }

        // 5. 创建支付记录
        Payment payment = new Payment();
        payment.setPaymentNo(generatePaymentNo());
        payment.setOrderNo(paymentDTO.getOrderNo());
        payment.setUserId(userId);
        payment.setAmount(paymentDTO.getAmount());
        payment.setPaymentMethod(paymentDTO.getPaymentMethod());
        payment.setPaymentStatus("PENDING");
        payment.setExpireTime(LocalDateTime.now().plusMinutes(30)); // 30分钟过期
        payment.setPaymentCreateTime(LocalDateTime.now());
        payment.setPaymentUpdateTime(LocalDateTime.now());
        payment.setTenantId(String.valueOf(SecureUtil.getTenantId()));

        paymentMapper.insert(payment);

        log.info("创建支付记录成功: paymentNo={}, orderNo={}, amount={}",
                payment.getPaymentNo(), payment.getOrderNo(), payment.getAmount());

        return convertToVO(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO executePayment(String paymentNo) {
        Payment payment = paymentMapper.selectByPaymentNo(paymentNo);
        if (payment == null) {
            throw new RuntimeException("支付记录不存在");
        }

        // 检查支付状态
        if (!"PENDING".equals(payment.getPaymentStatus()) && !"PROCESSING".equals(payment.getPaymentStatus())) {
            throw new RuntimeException("支付状态不正确");
        }

        // 检查是否过期
        if (payment.getExpireTime() != null && payment.getExpireTime().isBefore(LocalDateTime.now())) {
            payment.setPaymentStatus("FAILED");
            payment.setPaymentUpdateTime(LocalDateTime.now());
            paymentMapper.updateById(payment);
            throw new RuntimeException("支付已过期");
        }

        // 模拟支付处理中
        payment.setPaymentStatus("PROCESSING");
        payment.setPaymentUpdateTime(LocalDateTime.now());
        paymentMapper.updateById(payment);

        // 模拟支付结果（90%成功率）
        Random random = new Random();
        boolean success = random.nextInt(100) < 90;

        if (success) {
            // 支付成功
            payment.setPaymentStatus("SUCCESS");
            payment.setPaymentTime(LocalDateTime.now());
            payment.setTransactionId(generateTransactionId());
            payment.setPaymentUpdateTime(LocalDateTime.now());
            paymentMapper.updateById(payment);

            // 更新订单状态
            Order order = orderMapper.selectByOrderNo(payment.getOrderNo());
            if (order != null) {
                order.setOrderStatus("PAID");
                order.setPaymentMethod(payment.getPaymentMethod());
                order.setPaymentNo(payment.getPaymentNo());
                order.setPaymentTime(LocalDateTime.now());
                order.setUpdatedAt(LocalDateTime.now());
                orderMapper.updateById(order);

                // 发送支付成功短信通知
                // 暂时注释掉，因为Order实体类中没有phone字段
                /*
                if (smsService != null) {
                    smsService.sendPaymentSuccess(
                            "", // 暂时使用空字符串
                            order.getOrderNo(),
                            order.getActualAmount().toString()
                    );
                }
                */
            }

            log.info("支付成功: paymentNo={}, transactionId={}", paymentNo, payment.getTransactionId());
        } else {
            // 支付失败
            payment.setPaymentStatus("FAILED");
            payment.setPaymentUpdateTime(LocalDateTime.now());
            paymentMapper.updateById(payment);

            log.warn("支付失败: paymentNo={}, reason=余额不足", paymentNo);
        }

        return convertToVO(payment);
    }

    @Override
    public PaymentVO getPaymentStatus(String paymentNo) {
        Payment payment = paymentMapper.selectByPaymentNo(paymentNo);
        if (payment == null) {
            throw new RuntimeException("支付记录不存在");
        }
        return convertToVO(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO handlePaymentCallback(String paymentNo, String callbackData) {
        log.info("开始处理支付回调: paymentNo={}, callbackData={}", paymentNo, callbackData);

        try {
            Payment payment = paymentMapper.selectByPaymentNo(paymentNo);
            if (payment == null) {
                log.error("支付回调处理失败: 支付记录不存在, paymentNo={}", paymentNo);
                throw new RuntimeException("支付记录不存在");
            }

            // 检查支付状态是否已处理
            if ("SUCCESS".equals(payment.getPaymentStatus())) {
                log.info("支付回调处理: 支付记录已处理, paymentNo={}, status={}", paymentNo, payment.getPaymentStatus());
                return convertToVO(payment);
            }

            // 更新支付状态为成功
            payment.setPaymentStatus("SUCCESS");
            payment.setPaymentTime(LocalDateTime.now());
            payment.setTransactionId(generateTransactionId());
            payment.setPaymentUpdateTime(LocalDateTime.now());

            int paymentUpdateResult = paymentMapper.updateById(payment);
            log.info("支付记录更新结果: paymentNo={}, updateResult={}", paymentNo, paymentUpdateResult);

            // 更新订单状态
            Order order = orderMapper.selectByOrderNo(payment.getOrderNo());
            if (order != null) {
                log.info("订单状态更新前: orderNo={}, currentStatus={}", order.getOrderNo(), order.getOrderStatus());

                if ("PENDING".equals(order.getOrderStatus())) {
                    order.setOrderStatus("PAID");
                    order.setPaymentMethod(payment.getPaymentMethod());
                    order.setPaymentNo(payment.getPaymentNo());
                    order.setPaymentTime(LocalDateTime.now());
                    order.setUpdatedAt(LocalDateTime.now());

                    int orderUpdateResult = orderMapper.updateById(order);
                    log.info("订单状态更新结果: orderNo={}, newStatus={}, updateResult={}",
                            order.getOrderNo(), order.getOrderStatus(), orderUpdateResult);
                } else {
                    log.warn("订单状态不正确，无法更新: orderNo={}, currentStatus={}", order.getOrderNo(), order.getOrderStatus());
                }
            } else {
                log.error("支付回调处理: 订单不存在, orderNo={}", payment.getOrderNo());
            }

            log.info("支付回调处理成功: paymentNo={}, orderNo={}", paymentNo, payment.getOrderNo());
            return convertToVO(payment);
        } catch (Exception e) {
            log.error("支付回调处理失败: paymentNo={}", paymentNo, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO applyRefund(Long userId, RefundDTO refundDTO) {
        // 1. 验证支付记录
        Payment payment = paymentMapper.selectByPaymentNo(refundDTO.getPaymentNo());
        if (payment == null) {
            throw new RuntimeException("支付记录不存在");
        }
        if (!"SUCCESS".equals(payment.getPaymentStatus())) {
            throw new RuntimeException("该订单尚未支付成功，无法退款");
        }
        if (!payment.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此支付记录");
        }

        // 2. 验证退款金额
        if (refundDTO.getAmount().compareTo(payment.getAmount()) > 0) {
            throw new RuntimeException("退款金额不能超过支付金额");
        }

        // 3. 检查是否已退款
        List<Refund> existingRefunds = refundMapper.selectByPaymentNo(refundDTO.getPaymentNo());
        BigDecimal refundedAmount = existingRefunds.stream()
                .filter(r -> "SUCCESS".equals(r.getRefundStatus()))
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (refundedAmount.add(refundDTO.getAmount()).compareTo(payment.getAmount()) > 0) {
            throw new RuntimeException("退款总额不能超过支付金额");
        }

        // 4. 创建退款记录
        Refund refund = new Refund();
        refund.setRefundNo(generateRefundNo());
        refund.setPaymentNo(refundDTO.getPaymentNo());
        refund.setOrderNo(payment.getOrderNo());
        refund.setUserId(userId);
        refund.setAmount(refundDTO.getAmount());
        refund.setReason(refundDTO.getReason());
        refund.setRefundStatus("PENDING");
        refund.setCreatedAt(LocalDateTime.now());
        refund.setUpdatedAt(LocalDateTime.now());
        refund.setTenantId(payment.getTenantId());

        refundMapper.insert(refund);

        // 5. 模拟退款处理（95%成功率）
        Random random = new Random();
        boolean success = random.nextInt(100) < 95;

        if (success) {
            refund.setRefundStatus("SUCCESS");
            refund.setRefundedAt(LocalDateTime.now());
            refund.setRefundTransactionId(generateTransactionId());

            // 更新支付记录状态
            payment.setPaymentStatus("REFUNDED");
            payment.setPaymentUpdateTime(LocalDateTime.now());
            paymentMapper.updateById(payment);

            log.info("退款成功: refundNo={}, paymentNo={}", refund.getRefundNo(), refundDTO.getPaymentNo());
        } else {
            refund.setRefundStatus("FAILED");
            refund.setFailReason("系统处理失败，请稍后重试");
            log.warn("退款失败: refundNo={}, paymentNo={}", refund.getRefundNo(), refundDTO.getPaymentNo());
        }

        refund.setUpdatedAt(LocalDateTime.now());
        refundMapper.updateById(refund);

        return convertRefundToVO(refund);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO handleRefundCallback(String refundNo, String callbackData) {
        Refund refund = refundMapper.selectByRefundNo(refundNo);
        if (refund == null) {
            throw new RuntimeException("退款记录不存在");
        }

        refund.setCallbackData(callbackData);
        refund.setUpdatedAt(LocalDateTime.now());
        refundMapper.updateById(refund);

        log.info("处理退款回调: refundNo={}", refundNo);

        return convertRefundToVO(refund);
    }

    @Override
    public List<PaymentVO> getUserPayments(Long userId) {
        List<Payment> payments = paymentMapper.selectByUserId(userId);
        List<PaymentVO> result = new ArrayList<>();
        for (Payment payment : payments) {
            result.add(convertToVO(payment));
        }
        return result;
    }

    @Override
    public PaymentVO getPaymentByOrderNo(String orderNo) {
        List<Payment> payments = paymentMapper.selectByOrderNo(orderNo);
        if (payments.isEmpty()) {
            return null;
        }
        // 按创建时间降序排序，返回最新的支付记录
        payments.sort((p1, p2) -> {
            if (p1.getPaymentCreateTime() == null && p2.getPaymentCreateTime() == null) {
                return 0;
            }
            if (p1.getPaymentCreateTime() == null) {
                return 1;
            }
            if (p2.getPaymentCreateTime() == null) {
                return -1;
            }
            return p2.getPaymentCreateTime().compareTo(p1.getPaymentCreateTime());
        });
        return convertToVO(payments.get(0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO cancelPayment(String paymentNo) {
        Payment payment = paymentMapper.selectByPaymentNo(paymentNo);
        if (payment == null) {
            throw new RuntimeException("支付记录不存在");
        }

        if (!"PENDING".equals(payment.getPaymentStatus())) {
            throw new RuntimeException("只能取消待支付的订单");
        }

        payment.setPaymentStatus("CANCELLED");
        payment.setPaymentUpdateTime(LocalDateTime.now());
        paymentMapper.updateById(payment);

        log.info("取消支付: paymentNo={}", paymentNo);

        return convertToVO(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeExpiredPayments() {
        // 查询所有已过期的待支付订单
        List<Payment> expiredPayments = paymentMapper.selectByStatus("PENDING");
        LocalDateTime now = LocalDateTime.now();

        int count = 0;
        for (Payment payment : expiredPayments) {
            if (payment.getExpireTime() != null && payment.getExpireTime().isBefore(now)) {
                payment.setPaymentStatus("CLOSED");
                payment.setPaymentUpdateTime(now);
                paymentMapper.updateById(payment);
                count++;
            }
        }

        if (count > 0) {
            log.info("关闭过期支付订单: {} 条", count);
        }
    }

    /**
     * 生成支付单号
     */
    private String generatePaymentNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomStr = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return PAYMENT_PREFIX + dateStr + randomStr;
    }

    /**
     * 生成退款单号
     */
    private String generateRefundNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomStr = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return REFUND_PREFIX + dateStr + randomStr;
    }

    /**
     * 生成第三方交易号
     */
    private String generateTransactionId() {
        return "TRX" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 转换为VO
     */
    private PaymentVO convertToVO(Payment payment) {
        PaymentVO vo = new PaymentVO();
        vo.setId(payment.getId());
        vo.setPaymentNo(payment.getPaymentNo());
        vo.setOrderNo(payment.getOrderNo());
        vo.setAmount(payment.getAmount());
        vo.setPaymentMethod(payment.getPaymentMethod());
        vo.setStatus(payment.getPaymentStatus());
        vo.setStatusText(getStatusText(payment.getPaymentStatus()));
        vo.setPaymentMethodName(getPaymentMethodName(payment.getPaymentMethod()));
        vo.setPaymentTime(payment.getPaymentTime());
        vo.setCreateTime(payment.getPaymentCreateTime());
        vo.setExpireTime(payment.getExpireTime());

        // 如果是待支付状态，生成支付参数
        if ("PENDING".equals(payment.getPaymentStatus()) || "PROCESSING".equals(payment.getPaymentStatus())) {
            PaymentParamsVO params = new PaymentParamsVO();
            params.setPayType(payment.getPaymentMethod());
            params.setQrCodeUrl("https://placehold.co/200x200/000000/FFFFFF?text=" + payment.getPaymentNo());
            params.setPayUrl("/pay/" + payment.getPaymentNo());
            vo.setPaymentParams(params);
        }

        return vo;
    }

    /**
     * 转换为退款VO
     */
    private RefundVO convertRefundToVO(Refund refund) {
        RefundVO vo = new RefundVO();
        vo.setId(refund.getId());
        vo.setRefundNo(refund.getRefundNo());
        vo.setPaymentNo(refund.getPaymentNo());
        vo.setOrderNo(refund.getOrderNo());
        vo.setAmount(refund.getAmount());
        vo.setReason(refund.getReason());
        vo.setStatus(refund.getRefundStatus());
        vo.setStatusText(getRefundStatusText(refund.getRefundStatus()));
        vo.setRefundedAt(refund.getRefundedAt());
        vo.setCreatedAt(refund.getCreatedAt());
        vo.setAuditRemark(refund.getAuditRemark());
        return vo;
    }

    /**
     * 获取支付状态文本
     */
    private String getStatusText(String status) {
        switch (status) {
            case "PENDING":
                return "待支付";
            case "PROCESSING":
                return "支付中";
            case "SUCCESS":
                return "支付成功";
            case "FAILED":
                return "支付失败";
            case "REFUNDED":
                return "已退款";
            case "CANCELLED":
                return "已取消";
            case "CLOSED":
                return "已关闭";
            default:
                return status;
        }
    }

    /**
     * 获取退款状态文本
     */
    private String getRefundStatusText(String status) {
        switch (status) {
            case "PENDING":
                return "待处理";
            case "PROCESSING":
                return "处理中";
            case "SUCCESS":
                return "退款成功";
            case "FAILED":
                return "退款失败";
            default:
                return status;
        }
    }

    /**
     * 获取支付方式名称
     */
    private String getPaymentMethodName(String method) {
        switch (method) {
            case "BALANCE":
                return "余额支付";
            case "WECHAT":
                return "微信支付";
            case "ALIPAY":
                return "支付宝";
            default:
                return method;
        }
    }
}



