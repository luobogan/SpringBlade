package org.springblade.mall.controller;

import org.springblade.mall.dto.PaymentDTO;
import org.springblade.mall.dto.RefundDTO;
import org.springblade.mall.service.PaymentService;
import org.springblade.mall.vo.PaymentMethodVO;
import org.springblade.mall.vo.PaymentVO;
import org.springblade.mall.vo.RefundVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.boot.ctrl.BladeController;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 支付控制器
 */
@RestController
@RequestMapping(AppConstant.APPLICATION_MALL_NAME + "/payment")
@RequiredArgsConstructor
public class PaymentController extends BladeController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    /**
     * 获取可用支付方式列表
     */
    @GetMapping("/methods")
    public R<List<PaymentMethodVO>> getPaymentMethods() {
        try {
            List<PaymentMethodVO> methods = paymentService.getPaymentMethods();
            return R.data(methods);
        } catch (Exception e) {
            log.error("获取支付方式失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 创建支付
     */
    @PostMapping("/create")
    public R<PaymentVO> createPayment(
            @Valid @RequestBody PaymentDTO paymentDTO,
            BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }

        try {
            PaymentVO paymentVO = paymentService.createPayment(user.getUserId(), paymentDTO);
            return R.data(paymentVO, "支付订单创建成功");
        } catch (Exception e) {
            log.error("创建支付失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 执行支付（模拟支付）
     */
    @PostMapping("/{paymentNo}/pay")
    public R<PaymentVO> executePayment(@PathVariable String paymentNo, BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }

        try {
            PaymentVO paymentVO = paymentService.executePayment(paymentNo);
            return R.data(paymentVO, "支付成功");
        } catch (Exception e) {
            log.error("支付失败: paymentNo={}", paymentNo, e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 查询支付状态
     */
    @GetMapping("/{paymentNo}/status")
    public R<PaymentVO> getPaymentStatus(@PathVariable String paymentNo) {
        try {
            PaymentVO paymentVO = paymentService.getPaymentStatus(paymentNo);
            return R.data(paymentVO);
        } catch (Exception e) {
            log.error("查询支付状态失败: paymentNo={}", paymentNo, e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据订单号查询支付状态
     */
    @GetMapping("/order/{orderNo}")
    public R<PaymentVO> getPaymentByOrderNo(@PathVariable String orderNo) {
        try {
            PaymentVO paymentVO = paymentService.getPaymentByOrderNo(orderNo);
            return R.data(paymentVO);
        } catch (Exception e) {
            log.error("查询支付记录失败: orderNo={}", orderNo, e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取当前用户的支付记录
     */
    @GetMapping("/my")
    public R<List<PaymentVO>> getUserPayments(BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }

        try {
            List<PaymentVO> payments = paymentService.getUserPayments(user.getUserId());
            return R.data(payments);
        } catch (Exception e) {
            log.error("获取用户支付记录失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 取消支付
     */
    @PostMapping("/{paymentNo}/cancel")
    public R<PaymentVO> cancelPayment(@PathVariable String paymentNo, BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }

        try {
            PaymentVO paymentVO = paymentService.cancelPayment(paymentNo);
            return R.data(paymentVO, "支付已取消");
        } catch (Exception e) {
            log.error("取消支付失败: paymentNo={}", paymentNo, e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 支付回调接口
     */
    @PostMapping("/{paymentNo}/callback")
    public R<PaymentVO> handlePaymentCallback(
            @PathVariable String paymentNo,
            @RequestBody String callbackData) {
        try {
            PaymentVO paymentVO = paymentService.handlePaymentCallback(paymentNo, callbackData);
            return R.data(paymentVO, "回调处理成功");
        } catch (Exception e) {
            log.error("支付回调处理失败: paymentNo={}", paymentNo, e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 申请退款
     */
    @PostMapping("/refund")
    public R<RefundVO> applyRefund(@Valid @RequestBody RefundDTO refundDTO, BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }

        try {
            RefundVO refundVO = paymentService.applyRefund(user.getUserId(), refundDTO);
            return R.data(refundVO, "退款申请成功");
        } catch (Exception e) {
            log.error("申请退款失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 退款回调接口
     */
    @PostMapping("/refund/{refundNo}/callback")
    public R<RefundVO> handleRefundCallback(
            @PathVariable String refundNo,
            @RequestBody String callbackData) {
        try {
            RefundVO refundVO = paymentService.handleRefundCallback(refundNo, callbackData);
            return R.data(refundVO, "退款回调处理成功");
        } catch (Exception e) {
            log.error("退款回调处理失败: refundNo={}", refundNo, e);
            return R.fail(e.getMessage());
        }
    }


}



