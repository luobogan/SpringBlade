package org.springblade.mall.service.impl;

import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.refund.RefundService;
import org.springblade.mall.config.WeChatPayConfig;
import org.springblade.mall.dto.WechatPayRequestDTO;
import org.springblade.mall.dto.WechatPayResponseDTO;
import org.springblade.mall.entity.Order;
import org.springblade.mall.entity.Payment;
import org.springblade.mall.mapper.OrderMapper;
import org.springblade.mall.mapper.PaymentMapper;
import org.springblade.mall.service.WechatPayService;
import org.springblade.mall.utils.WeChatPayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.Func;

/**
 * 微信支付服务实现类
 *
 * @author youpinmall
 * @date 2026-03-05
 */
@Service
public class WechatPayServiceImpl implements WechatPayService {

    private static final Logger log = LoggerFactory.getLogger(WechatPayServiceImpl.class);

    private final WeChatPayConfig weChatPayConfig;
    private final WeChatPayUtil weChatPayUtil;
    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;

    @Autowired(required = false)
    private JsapiServiceExtension jsapiService;

    @Autowired(required = false)
    private NativePayService nativePayService;

    @Autowired(required = false)
    private RefundService refundService;

    public WechatPayServiceImpl(WeChatPayConfig weChatPayConfig, WeChatPayUtil weChatPayUtil,
                                PaymentMapper paymentMapper, OrderMapper orderMapper) {
        this.weChatPayConfig = weChatPayConfig;
        this.weChatPayUtil = weChatPayUtil;
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WechatPayResponseDTO createWechatPayOrder(Long userId, WechatPayRequestDTO requestDTO) {
        log.info("创建微信支付订单，userId={}, orderNo={}, amount={}",
                userId, requestDTO.getOrderNo(), requestDTO.getAmount());

        try {
            // 输入参数验证
            if (requestDTO.getAmount() == null) {
                throw new IllegalArgumentException("订单金额不能为空");
            }
            if (requestDTO.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("订单金额必须大于0，当前值：" + requestDTO.getAmount());
            }
            if (requestDTO.getDescription() == null || requestDTO.getDescription().isEmpty()) {
                throw new IllegalArgumentException("商品描述不能为空");
            }
            if (requestDTO.getTradeType() == null || requestDTO.getTradeType().isEmpty()) {
                throw new IllegalArgumentException("支付场景不能为空");
            }

            // 转换金额为分
            Integer amountInCents = weChatPayUtil.convertToCents(requestDTO.getAmount());
            log.info("金额转换：{} 元 = {} 分", requestDTO.getAmount(), amountInCents);

            if (amountInCents == null || amountInCents <= 0) {
                throw new IllegalArgumentException("金额转换错误，转换结果：" + amountInCents);
            }

            // 检查微信支付是否启用
            if (jsapiService == null || nativePayService == null) {
                log.warn("微信支付未启用，返回模拟订单");
                return createMockOrder(requestDTO);
            }

            WechatPayResponseDTO responseDTO = new WechatPayResponseDTO();

            // 1. 创建支付记录
            Payment payment = new Payment();
            payment.setPaymentNo(generatePaymentNo());
            payment.setOrderNo(requestDTO.getOrderNo());
            payment.setAmount(requestDTO.getAmount());
            payment.setPaymentStatus("NOT_PAY");
            payment.setUserId(userId);
            payment.setPaymentMethod("WECHAT");
            payment.setExpireTime(LocalDateTime.now().plusMinutes(30));
            payment.setPaymentCreateTime(LocalDateTime.now());
            payment.setPaymentUpdateTime(LocalDateTime.now());
            payment.setTenantId(SecureUtil.getTenantId());
            paymentMapper.insert(payment);

            responseDTO.setPaymentNo(payment.getPaymentNo());
            responseDTO.setOrderNo(requestDTO.getOrderNo());
            responseDTO.setDescription(requestDTO.getDescription());
            responseDTO.setTotalAmount(amountInCents);

            // 2. 根据支付场景调用不同的接口
            if ("JSAPI".equals(requestDTO.getTradeType())) {
                // JSAPI 支付（小程序/公众号）
                invokeJsapiPay(responseDTO, requestDTO, payment, amountInCents);
            } else if ("NATIVE".equals(requestDTO.getTradeType())) {
                // Native 支付（扫码支付）
                invokeNativePay(responseDTO, requestDTO, payment, amountInCents);
            } else {
                throw new RuntimeException("不支持的支付场景：" + requestDTO.getTradeType());
            }

            log.info("微信支付订单创建成功，paymentNo={}", payment.getPaymentNo());
            return responseDTO;
        } catch (Exception e) {
            log.error("创建微信支付订单失败，userId={}, requestDTO={}", userId, requestDTO, e);
            throw new RuntimeException("创建微信支付订单失败：" + e.getMessage());
        }
    }

    /**
     * 创建模拟订单（微信支付未启用时）
     */
    private WechatPayResponseDTO createMockOrder(WechatPayRequestDTO requestDTO) {
        WechatPayResponseDTO responseDTO = new WechatPayResponseDTO();
        responseDTO.setPaymentNo(generatePaymentNo());
        responseDTO.setOrderNo(requestDTO.getOrderNo());
        responseDTO.setDescription(requestDTO.getDescription());
        responseDTO.setTotalAmount(weChatPayUtil.convertToCents(requestDTO.getAmount()));

        if ("JSAPI".equals(requestDTO.getTradeType())) {
            // 返回模拟的 JSAPI 支付参数
            responseDTO.setAppId("mock_appid");
            responseDTO.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
            responseDTO.setNonceStr("mock_nonce_str");
            responseDTO.setPackageValue("mock_package");
            responseDTO.setSignType("RSA");
            responseDTO.setPaySign("mock_pay_sign");
        } else if ("NATIVE".equals(requestDTO.getTradeType())) {
            // 返回模拟的扫码支付 URL
            responseDTO.setCodeUrl("https://example.com/mock_qrcode");
        }

        return responseDTO;
    }

    /**
     * 调用 JSAPI 支付接口
     */
    private void invokeJsapiPay(WechatPayResponseDTO responseDTO, WechatPayRequestDTO requestDTO,
                               Payment payment, Integer amountInCents) throws Exception {
        if (requestDTO.getOpenid() == null || requestDTO.getOpenid().isEmpty()) {
            throw new RuntimeException("JSAPI 支付需要提供 openid");
        }

        log.info("调用 JSAPI 支付接口 - openid={}, amountInCents={}", requestDTO.getOpenid(), amountInCents);

        // 构建预支付请求
        PrepayRequest prepayRequest = weChatPayUtil.buildJsapiPrepayRequest(
                weChatPayConfig.getAppid(),
                weChatPayConfig.getMchId(),
                payment.getPaymentNo(),
                requestDTO.getDescription(),
                amountInCents,
                weChatPayConfig.getNotifyUrl(),
                requestDTO.getOpenid()
        );

        // 调用微信支付 JSAPI 下单接口
        PrepayWithRequestPaymentResponse paymentResponse = jsapiService.prepayWithRequestPayment(prepayRequest);

        // 设置返回参数
        responseDTO.setAppId(weChatPayConfig.getAppid());
        responseDTO.setTimeStamp(paymentResponse.getTimeStamp());
        responseDTO.setNonceStr(paymentResponse.getNonceStr());
        responseDTO.setPackageValue(paymentResponse.getPackageVal());
        responseDTO.setSignType("RSA");
        responseDTO.setPaySign(paymentResponse.getPaySign());

        log.info("JSAPI 支付下单成功，paymentNo={}, prepayId={}",
                payment.getPaymentNo(), paymentResponse.getPackageVal());
    }

    /**
     * 调用 Native 支付接口（扫码支付）
     */
    private void invokeNativePay(WechatPayResponseDTO responseDTO, WechatPayRequestDTO requestDTO,
                                Payment payment, Integer amountInCents) throws Exception {
        log.info("调用 Native 支付接口 - amountInCents={}", amountInCents);

        // 构建预支付请求
        com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest prepayRequest = weChatPayUtil.buildNativePrepayRequest(
                weChatPayConfig.getAppid(),
                weChatPayConfig.getMchId(),
                payment.getPaymentNo(),
                requestDTO.getDescription(),
                amountInCents,
                weChatPayConfig.getNotifyUrl()
        );

        // 调用微信支付 Native 下单接口
        PrepayResponse paymentResponse = nativePayService.prepay(prepayRequest);

        // 设置返回参数
        responseDTO.setCodeUrl(paymentResponse.getCodeUrl());

        // TODO: 生成二维码图片（可以使用 ZXing 等库）
        // responseDTO.setQrCodeImage(generateQrCode(paymentResponse.getCodeUrl()));

        log.info("Native 支付下单成功，paymentNo={}, codeUrl={}",
                payment.getPaymentNo(), paymentResponse.getCodeUrl());
    }

    @Override
    public String queryWechatPayStatus(String paymentNo) {
        log.info("查询微信支付状态，paymentNo={}", paymentNo);

        try {
            // 检查微信支付是否启用
            if (jsapiService == null) {
                log.warn("微信支付未启用，返回模拟状态");
                return "SUCCESS";
            }

            // 1. 查询本地支付记录
            Payment payment = paymentMapper.selectByPaymentNo(paymentNo);
            if (payment == null) {
                throw new RuntimeException("支付记录不存在");
            }

            // 2. 调用微信支付查询接口
            com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest queryRequest =
                new com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest();
            queryRequest.setOutTradeNo(paymentNo);
            queryRequest.setMchid(weChatPayConfig.getMchId());

            com.wechat.pay.java.service.payments.model.Transaction transaction =
                jsapiService.queryOrderByOutTradeNo(queryRequest);

            String tradeState = transaction.getTradeState() != null ? transaction.getTradeState().name() : null;
            log.info("微信支付状态查询成功，paymentNo={}, tradeState={}", paymentNo, tradeState);

            // 3. 更新本地支付记录状态
            if (transaction.getTradeState() != null && "SUCCESS".equals(transaction.getTradeState().name())) {
                payment.setPaymentStatus("SUCCESS");
                payment.setTransactionId(transaction.getTransactionId());
                payment.setPaymentUpdateTime(LocalDateTime.now());
                paymentMapper.updateById(payment);

                // 更新订单状态
                Order order = orderMapper.selectByOrderNo(payment.getOrderNo());
                if (order != null) {
                    order.setOrderStatus("PAID");
                    order.setUpdatedAt(LocalDateTime.now());
                    orderMapper.updateById(order);
                }
            }

            return tradeState;
        } catch (Exception e) {
            log.error("查询微信支付状态失败，paymentNo={}", paymentNo, e);
            return "FAILED";
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String wechatRefund(String paymentNo, java.math.BigDecimal refundAmount, String reason) {
        log.info("发起微信退款，paymentNo={}, amount={}, reason={}", paymentNo, refundAmount, reason);

        try {
            // 检查微信支付是否启用
            if (refundService == null) {
                log.warn("微信支付未启用，返回模拟退款单号");
                return "REF" + paymentNo.substring(3) + System.currentTimeMillis();
            }

            // 1. 查询支付记录
            Payment payment = paymentMapper.selectByPaymentNo(paymentNo);
            if (payment == null) {
                throw new RuntimeException("支付记录不存在");
            }
            if (!"SUCCESS".equals(payment.getPaymentStatus())) {
                throw new RuntimeException("该订单尚未支付成功，无法退款");
            }

            // 2. 检查退款金额
            if (refundAmount.compareTo(payment.getAmount()) > 0) {
                throw new RuntimeException("退款金额不能大于订单金额");
            }

            // 3. 生成退款单号
            String outRefundNo = weChatPayUtil.generateRefundNo(paymentNo);

            // 4. 构建退款请求
            Object refundRequest = weChatPayUtil.buildRefundRequest(
                    payment.getOrderNo(),
                    outRefundNo,
                    weChatPayUtil.convertToCents(refundAmount),
                    weChatPayUtil.convertToCents(payment.getAmount()),
                    reason,
                    weChatPayConfig.getNotifyUrl()
                );

            // 5. 调用微信退款接口
            // com.wechat.pay.java.service.refund.model.Refund refund = refundService.create(refundRequest);
            // log.info("微信退款请求成功，paymentNo={}, refundNo={}, refundId={}",
            //         paymentNo, outRefundNo, refund.getRefundId());

            // 6. TODO: 更新退款记录状态
            // 这里需要根据实际业务需求更新退款记录

            return outRefundNo;
        } catch (Exception e) {
            log.error("微信退款失败，paymentNo={}", paymentNo, e);
            throw new RuntimeException("微信退款失败：" + e.getMessage());
        }
    }

    @Override
    public String queryWechatRefundStatus(String refundNo) {
        log.info("查询微信退款状态，refundNo={}", refundNo);

        try {
            // 检查微信支付是否启用
            if (refundService == null) {
                log.warn("微信支付未启用，返回模拟状态");
                return "PROCESSING";
            }

            // TODO: SDK 0.2.17 退款查询 API 待确认，暂时返回模拟状态
            log.warn("SDK 0.2.17 退款查询 API 待确认，refundNo={}", refundNo);
            return "PROCESSING";

            // 调用微信退款查询接口
            // com.wechat.pay.java.service.refund.model.QueryRefundByOutRefundNoRequest queryRequest =
            //     new com.wechat.pay.java.service.refund.model.QueryRefundByOutRefundNoRequest();
            // queryRequest.setOutRefundNo(refundNo);
            //
            // com.wechat.pay.java.service.refund.model.Refund refund = refundService.queryByOutRefundNo(queryRequest);
            // log.info("退款状态查询成功，refundNo={}, status={}", refundNo, refund.getStatus());
            // return refund.getStatus();
        } catch (Exception e) {
            log.error("退款状态查询失败，refundNo={}", refundNo, e);
            throw new RuntimeException("退款状态查询失败：" + e.getMessage());
        }
    }

    /**
     * 生成支付单号
     */
    private String generatePaymentNo() {
        return "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    @Override
    public String handleWechatPayCallback(String body, String signature, String timestamp,
                                          String nonce, String serial) {
        log.info("处理微信支付回调");
        // 检查微信支付是否启用
        if (jsapiService == null) {
            log.warn("微信支付未启用，返回成功响应");
            return "{\"code\": \"SUCCESS\", \"message\": \"成功\"}";
        }

        // 调用回调处理服务
        // TODO: 实现回调处理逻辑
        log.warn("微信支付回调处理逻辑待实现");
        return "{\"code\": \"SUCCESS\", \"message\": \"成功\"}";
    }
}



