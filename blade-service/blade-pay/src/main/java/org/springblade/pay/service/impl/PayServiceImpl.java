package org.springblade.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.order.feign.IOrderClient;
import org.springblade.pay.constant.PayConstant;
import org.springblade.pay.dto.PaymentDTO;
import org.springblade.pay.dto.RefundDTO;
import org.springblade.pay.entity.Payment;
import org.springblade.pay.entity.Refund;
import org.springblade.pay.mapper.PaymentMapper;
import org.springblade.pay.mapper.RefundMapper;
import org.springblade.pay.service.IPayService;
import org.springblade.pay.vo.PaymentVO;
import org.springblade.pay.vo.RefundVO;
import org.springblade.pay.vo.WechatPayResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

@Service
public class PayServiceImpl implements IPayService {

    private static final Logger log = LoggerFactory.getLogger(PayServiceImpl.class);

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private RefundMapper refundMapper;

    @Autowired
    private IOrderClient orderClient;

    @Override
    @Transactional
    public PaymentVO createPayment(PaymentDTO paymentDTO) {
        String tenantId = SecureUtil.getTenantId();
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        String paymentNo = generatePaymentNo();

        Payment payment = new Payment();
        payment.setPaymentNo(paymentNo);
        payment.setOrderNo(paymentDTO.getOrderNo());
        payment.setUserId(SecureUtil.getUserId());
        payment.setAmount(paymentDTO.getAmount());
        payment.setPaymentMethod(paymentDTO.getPaymentMethod());
        payment.setPaymentStatus(PayConstant.STATUS_PENDING);
        payment.setExpireTime(LocalDateTime.now().plusMinutes(30));
        payment.setTenantId(tenantId);

        paymentMapper.insert(payment);

        log.info("创建支付单成功: paymentNo={}, orderNo={}, amount={}", paymentNo, paymentDTO.getOrderNo(), paymentDTO.getAmount());
        return convertToVO(payment);
    }

    @Override
    public PaymentVO getByPaymentNo(String paymentNo) {
        QueryWrapper<Payment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("payment_no", paymentNo);
        Payment payment = paymentMapper.selectOne(queryWrapper);
        if (payment == null) {
            throw new RuntimeException("支付单不存在");
        }
        return convertToVO(payment);
    }

    @Override
    @Transactional
    public PaymentVO handlePaymentCallback(String paymentNo, String transactionId) {
        QueryWrapper<Payment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("payment_no", paymentNo);
        Payment payment = paymentMapper.selectOne(queryWrapper);

        if (payment == null) {
            throw new RuntimeException("支付单不存在");
        }

        if (PayConstant.STATUS_SUCCESS.equals(payment.getPaymentStatus())) {
            log.info("支付单已处理，忽略重复回调: paymentNo={}", paymentNo);
            return convertToVO(payment);
        }

        payment.setPaymentStatus(PayConstant.STATUS_SUCCESS);
        payment.setTransactionId(transactionId);
        payment.setPaymentTime(LocalDateTime.now());

        paymentMapper.updateById(payment);

        log.info("支付成功: paymentNo={}, transactionId={}", paymentNo, transactionId);

        try {
            orderClient.updatePayStatus(payment.getOrderNo(), paymentNo);
        } catch (Exception e) {
            log.error("通知订单服务支付结果失败: orderNo={}", payment.getOrderNo(), e);
        }

        return convertToVO(payment);
    }

    @Override
    public WechatPayResultVO createWechatPay(PaymentDTO paymentDTO) {
        PaymentVO paymentVO = createPayment(paymentDTO);

        WechatPayResultVO result = new WechatPayResultVO();
        result.setPrepayId("wx_" + UUID.randomUUID().toString().replace("-", ""));
        result.setAppId("wx_app_id");
        result.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
        result.setNonceStr(UUID.randomUUID().toString().replace("-", ""));
        result.setPackageValue("prepay_id=" + result.getPrepayId());
        result.setSignType("RSA");
        result.setPaySign("mock_sign");

        return result;
    }

    @Override
    @Transactional
    public String handleWechatCallback(String xmlData) {
        log.info("收到微信支付回调: {}", xmlData);

        String paymentNo = extractPaymentNoFromCallback(xmlData);
        String transactionId = extractTransactionIdFromCallback(xmlData);

        if (StringUtil.isBlank(paymentNo) || StringUtil.isBlank(transactionId)) {
            return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[参数错误]]></return_msg></xml>";
        }

        try {
            handlePaymentCallback(paymentNo, transactionId);
            return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
        } catch (Exception e) {
            log.error("处理微信支付回调失败", e);
            return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[处理失败]]></return_msg></xml>";
        }
    }

    @Override
    @Transactional
    public RefundVO createRefund(RefundDTO refundDTO) {
        String tenantId = SecureUtil.getTenantId();
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        PaymentVO paymentVO = getByPaymentNo(refundDTO.getPaymentNo());
        if (!PayConstant.STATUS_SUCCESS.equals(paymentVO.getPaymentStatus())) {
            throw new RuntimeException("支付单状态不正确，只有已支付的订单才能退款");
        }

        String refundNo = generateRefundNo();

        Refund refund = new Refund();
        refund.setRefundNo(refundNo);
        refund.setPaymentNo(refundDTO.getPaymentNo());
        refund.setOrderNo(paymentVO.getOrderNo());
        refund.setUserId(paymentVO.getUserId());
        refund.setAmount(refundDTO.getAmount());
        refund.setReason(refundDTO.getReason());
        refund.setRefundStatus(PayConstant.STATUS_PENDING);
        refund.setTenantId(tenantId);

        refundMapper.insert(refund);

        log.info("创建退款单成功: refundNo={}, paymentNo={}, amount={}", refundNo, refundDTO.getPaymentNo(), refundDTO.getAmount());
        return convertRefundToVO(refund);
    }

    @Override
    public RefundVO getByRefundNo(String refundNo) {
        QueryWrapper<Refund> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no", refundNo);
        Refund refund = refundMapper.selectOne(queryWrapper);
        if (refund == null) {
            throw new RuntimeException("退款单不存在");
        }
        return convertRefundToVO(refund);
    }

    @Override
    @Transactional
    public RefundVO handleRefundCallback(String refundNo, String transactionId) {
        QueryWrapper<Refund> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no", refundNo);
        Refund refund = refundMapper.selectOne(queryWrapper);

        if (refund == null) {
            throw new RuntimeException("退款单不存在");
        }

        refund.setRefundStatus(PayConstant.STATUS_REFUNDED);
        refund.setRefundTransactionId(transactionId);
        refund.setRefundedAt(LocalDateTime.now());

        refundMapper.updateById(refund);

        log.info("退款成功: refundNo={}, transactionId={}", refundNo, transactionId);
        return convertRefundToVO(refund);
    }

    private String generatePaymentNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        Random random = new Random();
        int randomNum = random.nextInt(10000);
        return "PAY" + timestamp + String.format("%04d", randomNum);
    }

    private String generateRefundNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        Random random = new Random();
        int randomNum = random.nextInt(10000);
        return "REF" + timestamp + String.format("%04d", randomNum);
    }

    private String extractPaymentNoFromCallback(String xmlData) {
        int start = xmlData.indexOf("<out_trade_no>");
        int end = xmlData.indexOf("</out_trade_no>");
        if (start != -1 && end != -1) {
            return xmlData.substring(start + 14, end);
        }
        return null;
    }

    private String extractTransactionIdFromCallback(String xmlData) {
        int start = xmlData.indexOf("<transaction_id>");
        int end = xmlData.indexOf("</transaction_id>");
        if (start != -1 && end != -1) {
            return xmlData.substring(start + 18, end);
        }
        return null;
    }

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(DATE_TIME_PATTERN).format(date);
    }

    private String formatLocalDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
    }

    private PaymentVO convertToVO(Payment payment) {
        PaymentVO vo = new PaymentVO();
        BeanUtils.copyProperties(payment, vo);
        vo.setPaymentTime(formatLocalDateTime(payment.getPaymentTime()));
        vo.setExpireTime(formatLocalDateTime(payment.getExpireTime()));
        vo.setCreatedAt(formatDate(payment.getCreateTime()));
        vo.setUpdatedAt(formatDate(payment.getUpdateTime()));
        return vo;
    }

    private RefundVO convertRefundToVO(Refund refund) {
        RefundVO vo = new RefundVO();
        BeanUtils.copyProperties(refund, vo);
        vo.setRefundedAt(formatLocalDateTime(refund.getRefundedAt()));
        vo.setCreatedAt(formatDate(refund.getCreateTime()));
        vo.setUpdatedAt(formatDate(refund.getUpdateTime()));
        return vo;
    }
}
