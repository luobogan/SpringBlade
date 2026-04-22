package org.springblade.mall.controller;

import org.springblade.mall.service.WechatPayCallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.boot.ctrl.BladeController;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * 微信支付回调控制器
 * 处理微信支付结果通知和退款结果通知
 * 
 * @author youpinmall
 * @date 2026-03-05
 */
@Tag(name = "微信支付回调", description = "微信支付回调通知接口")
@RestController
@RequestMapping(AppConstant.APPLICATION_MALL_NAME + "/pay")
@RequiredArgsConstructor
public class WechatPayCallbackController extends BladeController {

    private static final Logger log = LoggerFactory.getLogger(WechatPayCallbackController.class);

    private final WechatPayCallbackService callbackService;

    /**
     * 微信支付结果通知
     * 
     * @param request HTTP 请求
     * @return 处理结果
     */
    @PostMapping("/wechat/notify")
    @Operation(summary = "微信支付结果通知", description = "接收微信支付的异步通知，处理订单支付结果")
    public ResponseEntity<String> handleWechatPayNotify(HttpServletRequest request) {
        try {
            // 读取请求体
            String body = getRequestBody(request);
            log.info("收到微信支付结果通知");
            log.debug("通知内容：{}", body);

            // 获取请求头中的签名信息
            String signature = request.getHeader("Wechatpay-Signature");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String serial = request.getHeader("Wechatpay-Serial");

            log.debug("签名信息：signature={}, timestamp={}, nonce={}, serial={}", 
                    signature, timestamp, nonce, serial);

            // 处理支付回调
            String result = callbackService.handlePayNotify(body, signature, timestamp, nonce, serial);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("处理微信支付回调失败", e);
            return ResponseEntity.ok("{\"code\": \"FAIL\", \"message\": \"处理失败\"}");
        }
    }

    /**
     * 微信退款结果通知
     * 
     * @param request HTTP 请求
     * @return 处理结果
     */
    @PostMapping("/wechat/refund/notify")
    @Operation(summary = "微信退款结果通知", description = "接收微信退款的异步通知，处理退款结果")
    public ResponseEntity<String> handleWechatRefundNotify(HttpServletRequest request) {
        try {
            // 读取请求体
            String body = getRequestBody(request);
            log.info("收到微信退款结果通知");
            log.debug("通知内容：{}", body);

            // 获取请求头中的签名信息
            String signature = request.getHeader("Wechatpay-Signature");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String serial = request.getHeader("Wechatpay-Serial");

            log.debug("签名信息：signature={}, timestamp={}, nonce={}, serial={}", 
                    signature, timestamp, nonce, serial);

            // 处理退款回调
            String result = callbackService.handleRefundNotify(body, signature, timestamp, nonce, serial);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("处理微信退款回调失败", e);
            return ResponseEntity.ok("{\"code\": \"FAIL\", \"message\": \"处理失败\"}");
        }
    }

    /**
     * 获取请求体
     * 
     * @param request HTTP 请求
     * @return 请求体字符串
     * @throws IOException IO 异常
     */
    private String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}



