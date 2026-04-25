package org.springblade.pay.feign;

import org.springblade.core.tool.api.R;
import org.springblade.pay.dto.PaymentDTO;
import org.springblade.pay.dto.RefundDTO;
import org.springblade.pay.vo.PaymentVO;
import org.springblade.pay.vo.RefundVO;
import org.springblade.pay.vo.WechatPayResultVO;
import org.springframework.stereotype.Component;

@Component
public class IPayClientFallback implements IPayClient {

    @Override
    public R<PaymentVO> createPayment(PaymentDTO paymentDTO) {
        return R.fail("支付服务不可用");
    }

    @Override
    public R<PaymentVO> getByPaymentNo(String paymentNo) {
        return R.fail("支付服务不可用");
    }

    @Override
    public R<WechatPayResultVO> createWechatPay(PaymentDTO paymentDTO) {
        return R.fail("支付服务不可用");
    }

    @Override
    public R<String> handleWechatCallback(String xmlData) {
        return R.fail("支付服务不可用");
    }

    @Override
    public R<RefundVO> createRefund(RefundDTO refundDTO) {
        return R.fail("退款服务不可用");
    }

    @Override
    public R<RefundVO> getByRefundNo(String refundNo) {
        return R.fail("退款服务不可用");
    }
}
