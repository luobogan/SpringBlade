package org.springblade.pay.service;

import org.springblade.pay.dto.PaymentDTO;
import org.springblade.pay.dto.RefundDTO;
import org.springblade.pay.vo.PaymentMethodVO;
import org.springblade.pay.vo.PaymentVO;
import org.springblade.pay.vo.RefundVO;
import org.springblade.pay.vo.WechatPayResultVO;

import java.util.List;

public interface IPayService {

    PaymentVO createPayment(PaymentDTO paymentDTO);

    PaymentVO getByPaymentNo(String paymentNo);

    PaymentVO handlePaymentCallback(String paymentNo, String transactionId);

    WechatPayResultVO createWechatPay(PaymentDTO paymentDTO);

    String handleWechatCallback(String xmlData);

    RefundVO createRefund(RefundDTO refundDTO);

    RefundVO getByRefundNo(String refundNo);

    RefundVO handleRefundCallback(String refundNo, String transactionId);

    List<PaymentMethodVO> getPaymentMethods();

    PaymentVO executePayment(String paymentNo);

    PaymentVO getPaymentStatus(String paymentNo);

    PaymentVO getPaymentByOrderNo(String orderNo);

    List<PaymentVO> getUserPayments(Long userId);

    PaymentVO cancelPayment(String paymentNo);
}
