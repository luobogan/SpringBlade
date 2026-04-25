package org.springblade.pay.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.pay.dto.PaymentDTO;
import org.springblade.pay.dto.RefundDTO;
import org.springblade.pay.feign.IPayClient;
import org.springblade.pay.service.IPayService;
import org.springblade.pay.vo.PaymentVO;
import org.springblade.pay.vo.RefundVO;
import org.springblade.pay.vo.WechatPayResultVO;
import org.springframework.web.bind.annotation.*;

@Hidden
@RestController
@AllArgsConstructor
public class PayClientController implements IPayClient {

    private final IPayService payService;

    @Override
    @PostMapping(CREATE_PAYMENT)
    public R<PaymentVO> createPayment(PaymentDTO paymentDTO) {
        try {
            PaymentVO paymentVO = payService.createPayment(paymentDTO);
            return R.data(paymentVO);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    @GetMapping(GET_PAYMENT_BY_NO)
    public R<PaymentVO> getByPaymentNo(@RequestParam("paymentNo") String paymentNo) {
        try {
            PaymentVO paymentVO = payService.getByPaymentNo(paymentNo);
            return R.data(paymentVO);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    @PostMapping(CREATE_WECHAT_PAY)
    public R<WechatPayResultVO> createWechatPay(PaymentDTO paymentDTO) {
        try {
            WechatPayResultVO result = payService.createWechatPay(paymentDTO);
            return R.data(result);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    @PostMapping(HANDLE_WECHAT_CALLBACK)
    public R<String> handleWechatCallback(@RequestParam("xmlData") String xmlData) {
        try {
            String result = payService.handleWechatCallback(xmlData);
            return R.data(result);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    @PostMapping(CREATE_REFUND)
    public R<RefundVO> createRefund(RefundDTO refundDTO) {
        try {
            RefundVO refundVO = payService.createRefund(refundDTO);
            return R.data(refundVO);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    @GetMapping(GET_REFUND_BY_NO)
    public R<RefundVO> getByRefundNo(@RequestParam("refundNo") String refundNo) {
        try {
            RefundVO refundVO = payService.getByRefundNo(refundNo);
            return R.data(refundVO);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}
