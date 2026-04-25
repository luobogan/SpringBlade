package org.springblade.pay.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.pay.dto.PaymentDTO;
import org.springblade.pay.dto.RefundDTO;
import org.springblade.pay.service.IPayService;
import org.springblade.pay.vo.PaymentVO;
import org.springblade.pay.vo.RefundVO;
import org.springblade.pay.vo.WechatPayResultVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pay")
@AllArgsConstructor
public class PayController extends BladeController {

    private final IPayService payService;

    @PostMapping("/create")
    @ApiOperationSupport(order = 1)
    @Operation(summary = "创建支付单", description = "传入paymentDTO")
    public R<PaymentVO> createPayment(@Valid @RequestBody PaymentDTO paymentDTO, BladeUser user) {
        if (user != null) {
            PaymentVO paymentVO = payService.createPayment(paymentDTO);
            return R.data(paymentVO, "支付单创建成功");
        }
        return R.fail("用户未登录");
    }

    @GetMapping("/detail")
    @ApiOperationSupport(order = 2)
    @Operation(summary = "查看支付单详情", description = "传入paymentNo")
    public R<PaymentVO> getByPaymentNo(@Parameter(description = "支付单号", required = true) @RequestParam String paymentNo) {
        PaymentVO paymentVO = payService.getByPaymentNo(paymentNo);
        return R.data(paymentVO);
    }

    @PostMapping("/wechat/create")
    @ApiOperationSupport(order = 3)
    @Operation(summary = "创建微信支付", description = "传入paymentDTO")
    public R<WechatPayResultVO> createWechatPay(@Valid @RequestBody PaymentDTO paymentDTO, BladeUser user) {
        if (user != null) {
            WechatPayResultVO result = payService.createWechatPay(paymentDTO);
            return R.data(result);
        }
        return R.fail("用户未登录");
    }

    @PostMapping("/wechat/callback")
    @ApiOperationSupport(order = 4)
    @Operation(summary = "微信支付回调", description = "处理微信支付异步回调")
    public String handleWechatCallback(@RequestParam("xmlData") String xmlData) {
        return payService.handleWechatCallback(xmlData);
    }

    @PostMapping("/refund/create")
    @ApiOperationSupport(order = 5)
    @Operation(summary = "创建退款单", description = "传入refundDTO")
    public R<RefundVO> createRefund(@Valid @RequestBody RefundDTO refundDTO, BladeUser user) {
        if (user != null) {
            RefundVO refundVO = payService.createRefund(refundDTO);
            return R.data(refundVO, "退款单创建成功");
        }
        return R.fail("用户未登录");
    }

    @GetMapping("/refund/detail")
    @ApiOperationSupport(order = 6)
    @Operation(summary = "查看退款单详情", description = "传入refundNo")
    public R<RefundVO> getByRefundNo(@Parameter(description = "退款单号", required = true) @RequestParam String refundNo) {
        RefundVO refundVO = payService.getByRefundNo(refundNo);
        return R.data(refundVO);
    }
}
