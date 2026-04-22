package org.springblade.mall.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springblade.mall.config.WeChatPayProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * 微信支付回调处理服务
 * 
 * @author youpinmall
 * @date 2026-03-05
 */
@Service
@RequiredArgsConstructor
public class WechatPayCallbackService {

    private static final Logger log = LoggerFactory.getLogger(WechatPayCallbackService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WeChatPayProperties properties;
    private final OrderService orderService;
    private X509Certificate platformCertificate;

    /**
     * 获取微信支付平台证书（懒加载）
     */
    private X509Certificate getPlatformCertificate() throws Exception {
        if (platformCertificate == null) {
            String certPath = properties.getPlatformCertPath();
            if (certPath.startsWith("classpath:")) {
                String resourcePath = certPath.substring("classpath:".length());
                InputStream inputStream = this.getClass().getResourceAsStream(resourcePath);
                if (inputStream == null) {
                    throw new RuntimeException("证书文件不存在：" + certPath);
                }
                platformCertificate = loadCertificate(inputStream);
            } else {
                platformCertificate = loadCertificate(new FileInputStream(certPath));
            }
        }
        return platformCertificate;
    }

    /**
     * 从输入流加载证书
     */
    private X509Certificate loadCertificate(InputStream inputStream) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(inputStream);
    }

    /**
     * 处理支付回调通知
     * 
     * @param body      请求体
     * @param signature 签名
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param serial    证书序列号
     * @return 处理结果
     */
    public String handlePayNotify(String body, String signature, String timestamp, String nonce, String serial) {
        try {
            log.info("开始处理支付回调通知");

            // 1. 验证签名
            if (!verifySignature(body, signature, timestamp, nonce, serial)) {
                log.error("支付回调签名验证失败");
                return "{\"code\": \"FAIL\", \"message\": \"签名验证失败\"}";
            }

            // 2. 解析回调数据
            JsonNode rootNode = objectMapper.readTree(body);
            JsonNode resourceNode = rootNode.get("resource");
            String algorithm = resourceNode.get("algorithm").asText();
            String ciphertext = resourceNode.get("ciphertext").asText();
            String associatedData = resourceNode.get("associated_data").asText();
            String nonce_value = resourceNode.get("nonce").asText();

            log.debug("回调数据解析成功，加密算法：{}", algorithm);

            // 3. 解密数据
            String decryptedData = decryptData(ciphertext, associatedData, nonce_value);
            log.debug("数据解密成功");

            // 4. 解析解密后的数据
            JsonNode decryptedNode = objectMapper.readTree(decryptedData);
            String outTradeNo = decryptedNode.get("out_trade_no").asText();
            String transactionId = decryptedNode.get("transaction_id").asText();
            String tradeState = decryptedNode.get("trade_state").asText();
            String amountTotal = decryptedNode.get("amount").get("total").asText();
            String payerTotal = decryptedNode.get("amount").get("payer_total").asText();

            log.info("支付回调详情：orderNo={}, transactionId={}, tradeState={}, amount={}, payerAmount={}",
                    outTradeNo, transactionId, tradeState, amountTotal, payerTotal);

            // 5. 更新订单状态
            if ("SUCCESS".equals(tradeState)) {
                // 支付成功
                // TODO: 调用订单服务更新状态
                // orderService.updatePayStatus(outTradeNo, transactionId, "PAID");
                log.info("订单支付状态更新成功：{}", outTradeNo);
            } else if ("NOTPAY".equals(tradeState)) {
                // 未支付
                log.warn("订单未支付：{}", outTradeNo);
            } else {
                // 其他状态（已关闭、已撤销等）
                log.warn("订单支付状态异常：{}, tradeState={}", outTradeNo, tradeState);
            }

            // 6. 返回成功响应
            log.info("支付回调处理成功");
            return "{\"code\": \"SUCCESS\", \"message\": \"成功\"}";

        } catch (Exception e) {
            log.error("处理支付回调异常", e);
            return "{\"code\": \"FAIL\", \"message\": \"处理异常\"}";
        }
    }

    /**
     * 处理退款回调通知
     * 
     * @param body      请求体
     * @param signature 签名
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param serial    证书序列号
     * @return 处理结果
     */
    public String handleRefundNotify(String body, String signature, String timestamp, String nonce, String serial) {
        try {
            log.info("开始处理退款回调通知");

            // 1. 验证签名
            if (!verifySignature(body, signature, timestamp, nonce, serial)) {
                log.error("退款回调签名验证失败");
                return "{\"code\": \"FAIL\", \"message\": \"签名验证失败\"}";
            }

            // 2. 解析回调数据
            JsonNode rootNode = objectMapper.readTree(body);
            JsonNode resourceNode = rootNode.get("resource");
            String algorithm = resourceNode.get("algorithm").asText();
            String ciphertext = resourceNode.get("ciphertext").asText();
            String associatedData = resourceNode.get("associated_data").asText();
            String nonce_value = resourceNode.get("nonce").asText();

            log.debug("回调数据解析成功，加密算法：{}", algorithm);

            // 3. 解密数据
            String decryptedData = decryptData(ciphertext, associatedData, nonce_value);
            log.debug("数据解密成功");

            // 4. 解析解密后的数据
            JsonNode decryptedNode = objectMapper.readTree(decryptedData);
            String outRefundNo = decryptedNode.get("out_refund_no").asText();
            String outTradeNo = decryptedNode.get("out_trade_no").asText();
            String refundStatus = decryptedNode.get("status").asText();
            String refundId = decryptedNode.get("refund_id").asText();
            
            // 获取退款金额信息
            JsonNode amountNode = decryptedNode.get("amount");
            Integer total = amountNode.get("total").asInt();
            Integer refund = amountNode.get("refund").asInt();
            String currency = amountNode.get("currency").asText();

            log.info("退款回调详情：refundNo={}, orderId={}, refundId={}, status={}, amount={}, currency={}",
                    outRefundNo, outTradeNo, refundId, refundStatus, refund, currency);

            // 5. 更新退款状态
            if ("SUCCESS".equals(refundStatus)) {
                // 退款成功
                log.info("退款成功：outRefundNo={}, refundId={}", outRefundNo, refundId);
                // TODO: 更新数据库中的退款记录状态
                // refundService.updateRefundStatus(outRefundNo, "SUCCESS", refundId);
            } else if ("ABNORMAL".equals(refundStatus)) {
                // 退款异常
                log.error("退款异常：outRefundNo={}, refundId={}", outRefundNo, refundId);
                // TODO: 更新数据库中的退款记录状态，并发送告警
                // refundService.updateRefundStatus(outRefundNo, "ABNORMAL", refundId);
            } else if ("PROCESSING".equals(refundStatus)) {
                // 退款处理中
                log.info("退款处理中：outRefundNo={}, refundId={}", outRefundNo, refundId);
            }

            // 6. 返回成功响应
            log.info("退款回调处理成功");
            return "{\"code\": \"SUCCESS\", \"message\": \"成功\"}";

        } catch (Exception e) {
            log.error("处理退款回调异常", e);
            return "{\"code\": \"FAIL\", \"message\": \"处理异常\"}";
        }
    }

    /**
     * 验证签名
     * 
     * @param body      请求体
     * @param signature 签名
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param serial    证书序列号
     * @return 签名是否有效
     */
    private boolean verifySignature(String body, String signature, String timestamp, String nonce, String serial) throws Exception {
        // 构建验签信息
        String message = timestamp + "\n" + nonce + "\n" + body + "\n";

        // 获取微信支付平台证书
        X509Certificate certificate = getPlatformCertificate();
        if (certificate == null) {
            log.error("未找到证书，serial={}", serial);
            return false;
        }

        // 验证签名
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(certificate.getPublicKey());
        sig.update(message.getBytes(StandardCharsets.UTF_8));
        
        boolean verified = sig.verify(Base64.getDecoder().decode(signature));
        log.debug("签名验证结果：{}", verified);
        return verified;
    }

    /**
     * 解密数据
     * 
     * @param ciphertext     密文
     * @param associatedData 附加数据
     * @param nonce          随机数
     * @return 解密后的数据
     */
    private String decryptData(String ciphertext, String associatedData, String nonce) throws Exception {
        // 使用 AES-256-GCM 解密
        byte[] keyBytes = properties.getApiV3Key().getBytes(StandardCharsets.UTF_8);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);

        byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);
        byte[] plaintextBytes = cipher.doFinal(ciphertextBytes);

        return new String(plaintextBytes, StandardCharsets.UTF_8);
    }
}



