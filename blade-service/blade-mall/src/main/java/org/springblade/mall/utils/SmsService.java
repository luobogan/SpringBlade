package org.springblade.mall.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;



/**
 * 短信通知服务
 * 用于发送支付、退款等通知短信
 */
@Service
@ConditionalOnProperty(name = "sms.enabled", havingValue = "true")
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    /**
     * 发送支付成功短信
     * @param phoneNumber 手机号
     * @param orderNo 订单号
     * @param amount 支付金额
     */
    public void sendPaymentSuccess(String phoneNumber, String orderNo, String amount) {
        log.info("发送支付成功短信: phone={}, orderNo={}, amount={}", phoneNumber, orderNo, amount);

        try {
            // TODO: 集成真实的短信服务（阿里云、腾讯云等）

            // 阿里云短信示例
            // aliyunSmsService.sendSms(phoneNumber, templateCode, params);

            // 腾讯云短信示例
            // tencentSmsService.sendSms(phoneNumber, templateId, params);

            log.info("支付成功短信发送成功: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("支付成功短信发送失败: orderNo={}", orderNo, e);
        }
    }

    /**
     * 发送退款成功短信
     * @param phoneNumber 手机号
     * @param orderNo 订单号
     * @param refundAmount 退款金额
     */
    public void sendRefundSuccess(String phoneNumber, String orderNo, String refundAmount) {
        log.info("发送退款成功短信: phone={}, orderNo={}, amount={}", phoneNumber, orderNo, refundAmount);

        try {
            // TODO: 集成真实的短信服务

            log.info("退款成功短信发送成功: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("退款成功短信发送失败: orderNo={}", orderNo, e);
        }
    }

    /**
     * 发送订单发货短信
     * @param phoneNumber 手机号
     * @param orderNo 订单号
     * @param expressNo 快递单号
     */
    public void sendOrderShipped(String phoneNumber, String orderNo, String expressNo) {
        log.info("发送订单发货短信: phone={}, orderNo={}, expressNo={}", phoneNumber, orderNo, expressNo);

        try {
            // TODO: 集成真实的短信服务

            log.info("订单发货短信发送成功: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("订单发货短信发送失败: orderNo={}", orderNo, e);
        }
    }

    /**
     * 发送验证码短信
     * @param phoneNumber 手机号
     * @param code 验证码
     */
    public void sendVerificationCode(String phoneNumber, String code) {
        log.info("发送验证码短信: phone={}, code={}", phoneNumber, code);

        try {
            // TODO: 集成真实的短信服务

            log.info("验证码短信发送成功: phone={}", phoneNumber);
        } catch (Exception e) {
            log.error("验证码短信发送失败: phone={}", phoneNumber, e);
        }
    }

    // ==================== 阿里云短信实现示例（注释） ====================
    /*
    @Autowired
    private DefaultAcsClient acsClient;

    @Value("${aliyun.sms.sign-name}")
    private String signName;

    @Value("${aliyun.sms.template-code.payment}")
    private String paymentTemplateCode;

    private void sendAliyunSms(String phoneNumber, String templateCode, Map<String, String> params) {
        SendSmsRequest request = new SendSmsRequest();
        request.setPhoneNumbers(phoneNumber);
        request.setSignName(signName);
        request.setTemplateCode(templateCode);

        // 构建模板参数
        StringBuilder sb = new StringBuilder("{");
        params.forEach((k, v) -> sb.append("\"").append(k).append("\":\"").append(v).append("\","));
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        request.setTemplateParam(sb.toString());

        try {
            SendSmsResponse response = acsClient.getAcsResponse(request);
            if ("OK".equals(response.getCode())) {
                log.info("阿里云短信发送成功: phone={}", phoneNumber);
            } else {
                log.error("阿里云短信发送失败: phone={}, code={}, message={}", phoneNumber, response.getCode(), response.getMessage());
            }
        } catch (Exception e) {
            log.error("阿里云短信发送异常: phone={}", phoneNumber, e);
        }
    }
    */

    // ==================== 腾讯云短信实现示例（注释） ====================
    /*
    @Autowired
    private SmsClient smsClient;

    @Value("${tencent.sms.app-id}")
    private int appId;

    @Value("${tencent.sms.sign-name}")
    private String signName;

    private void sendTencentSms(String phoneNumber, String templateId, String[] params) {
        try {
            SendSmsRequest request = new SendSmsRequest();
            request.setSmsSdkAppId(appId);
            request.setPhoneNumberSet(new String[]{"+86" + phoneNumber});
            request.setSignName(signName);
            request.setTemplateId(templateId);
            request.setTemplateParamSet(params);

            SendSmsResponse response = smsClient.SendSms(request);
            if (response != null && "Ok".equals(response.getRequestId())) {
                log.info("腾讯云短信发送成功: phone={}", phoneNumber);
            } else {
                log.error("腾讯云短信发送失败: phone={}", phoneNumber);
            }
        } catch (Exception e) {
            log.error("腾讯云短信发送异常: phone={}", phoneNumber, e);
        }
    }
    */
}
