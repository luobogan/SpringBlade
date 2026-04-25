package org.springblade.pay.feign;

import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.tool.api.R;
import org.springblade.pay.dto.PaymentDTO;
import org.springblade.pay.dto.RefundDTO;
import org.springblade.pay.vo.PaymentVO;
import org.springblade.pay.vo.RefundVO;
import org.springblade.pay.vo.WechatPayResultVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    value = AppConstant.APPLICATION_PAY_NAME,
    fallback = IPayClientFallback.class
)
public interface IPayClient {

    String API_PREFIX = "/client/pay";

    String CREATE_PAYMENT = API_PREFIX + "/create";
    String GET_PAYMENT_BY_NO = API_PREFIX + "/get-by-payment-no";
    String CREATE_WECHAT_PAY = API_PREFIX + "/wechat/create";
    String HANDLE_WECHAT_CALLBACK = API_PREFIX + "/wechat/callback";
    String CREATE_REFUND = API_PREFIX + "/refund/create";
    String GET_REFUND_BY_NO = API_PREFIX + "/refund/get-by-refund-no";

    @PostMapping(CREATE_PAYMENT)
    R<PaymentVO> createPayment(@RequestBody PaymentDTO paymentDTO);

    @GetMapping(GET_PAYMENT_BY_NO)
    R<PaymentVO> getByPaymentNo(@RequestParam("paymentNo") String paymentNo);

    @PostMapping(CREATE_WECHAT_PAY)
    R<WechatPayResultVO> createWechatPay(@RequestBody PaymentDTO paymentDTO);

    @PostMapping(HANDLE_WECHAT_CALLBACK)
    R<String> handleWechatCallback(@RequestParam("xmlData") String xmlData);

    @PostMapping(CREATE_REFUND)
    R<RefundVO> createRefund(@RequestBody RefundDTO refundDTO);

    @GetMapping(GET_REFUND_BY_NO)
    R<RefundVO> getByRefundNo(@RequestParam("refundNo") String refundNo);
}
