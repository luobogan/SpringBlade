package org.springblade.pay.service;

import org.springblade.pay.dto.PaymentDTO;
import org.springblade.pay.dto.RefundDTO;
import org.springblade.pay.vo.PaymentVO;
import org.springblade.pay.vo.RefundVO;
import org.springblade.pay.vo.WechatPayResultVO;

public interface IPayService {

    PaymentVO createPayment(PaymentDTO paymentDTO);

    PaymentVO getByPaymentNo(String paymentNo);

    PaymentVO handlePaymentCallback(String paymentNo, String transactionId);

    WechatPayResultVO createWechatPay(PaymentDTO paymentDTO);

    String handleWechatCallback(String xmlData);

    RefundVO createRefund(RefundDTO refundDTO);

    RefundVO getByRefundNo(String refundNo);

    RefundVO handleRefundCallback(String refundNo, String transactionId);
}
