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
import org.springblade.pay.vo.PaymentMethodVO;
import org.springblade.pay.vo.PaymentVO;
import org.springblade.pay.vo.RefundVO;
import org.springblade.pay.vo.WechatPayResultVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pay")
@AllArgsConstructor
public class PayController extends BladeController {

    private final IPayService payService;

    @GetMapping("/methods")
    @ApiOperationSupport(order = 0)
    @Operation(summary = "获取可用支付方式列表", description = "无需参数")
    public R<List<PaymentMethodVO>> getPaymentMethods() {
        List<PaymentMethodVO> methods = payService.getPaymentMethods();
        return R.data(methods);
    }

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

    @PostMapping("/{paymentNo}/pay")
    @ApiOperationSupport(order = 3)
    @Operation(summary = "执行支付", description = "传入paymentNo")
    public R<PaymentVO> executePayment(@PathVariable String paymentNo, BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }
        PaymentVO paymentVO = payService.executePayment(paymentNo);
        return R.data(paymentVO, "支付成功");
    }

    @GetMapping("/{paymentNo}/status")
    @ApiOperationSupport(order = 4)
    @Operation(summary = "查询支付状态", description = "传入paymentNo")
    public R<PaymentVO> getPaymentStatus(@PathVariable String paymentNo) {
        PaymentVO paymentVO = payService.getPaymentStatus(paymentNo);
        return R.data(paymentVO);
    }

    @GetMapping("/order/{orderNo}")
    @ApiOperationSupport(order = 5)
    @Operation(summary = "根据订单号查询支付状态", description = "传入orderNo")
    public R<PaymentVO> getPaymentByOrderNo(@PathVariable String orderNo) {
        PaymentVO paymentVO = payService.getPaymentByOrderNo(orderNo);
        return R.data(paymentVO);
    }

    @GetMapping("/my")
    @ApiOperationSupport(order = 6)
    @Operation(summary = "获取当前用户的支付记录", description = "无需参数")
    public R<List<PaymentVO>> getUserPayments(BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }
        List<PaymentVO> payments = payService.getUserPayments(user.getUserId());
        return R.data(payments);
    }

    @PostMapping("/{paymentNo}/cancel")
    @ApiOperationSupport(order = 7)
    @Operation(summary = "取消支付", description = "传入paymentNo")
    public R<PaymentVO> cancelPayment(@PathVariable String paymentNo, BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }
        PaymentVO paymentVO = payService.cancelPayment(paymentNo);
        return R.data(paymentVO, "支付已取消");
    }

    @PostMapping("/wechat/create")
    @ApiOperationSupport(order = 8)
    @Operation(summary = "创建微信支付", description = "传入paymentDTO")
    public R<WechatPayResultVO> createWechatPay(@Valid @RequestBody PaymentDTO paymentDTO, BladeUser user) {
        if (user != null) {
            WechatPayResultVO result = payService.createWechatPay(paymentDTO);
            return R.data(result);
        }
        return R.fail("用户未登录");
    }

    @PostMapping("/wechat/callback")
    @ApiOperationSupport(order = 9)
    @Operation(summary = "微信支付回调", description = "处理微信支付异步回调")
    public String handleWechatCallback(@RequestParam("xmlData") String xmlData) {
        return payService.handleWechatCallback(xmlData);
    }

    @PostMapping("/refund/create")
    @ApiOperationSupport(order = 10)
    @Operation(summary = "创建退款单", description = "传入refundDTO")
    public R<RefundVO> createRefund(@Valid @RequestBody RefundDTO refundDTO, BladeUser user) {
        if (user != null) {
            RefundVO refundVO = payService.createRefund(refundDTO);
            return R.data(refundVO, "退款单创建成功");
        }
        return R.fail("用户未登录");
    }

    @GetMapping("/refund/detail")
    @ApiOperationSupport(order = 11)
    @Operation(summary = "查看退款单详情", description = "传入refundNo")
    public R<RefundVO> getByRefundNo(@Parameter(description = "退款单号", required = true) @RequestParam String refundNo) {
        RefundVO refundVO = payService.getByRefundNo(refundNo);
        return R.data(refundVO);
    }
}
