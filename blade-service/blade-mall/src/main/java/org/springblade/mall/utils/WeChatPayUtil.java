package org.springblade.mall.utils;

import com.wechat.pay.java.core.util.NonceUtil;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import org.springblade.mall.config.WeChatPayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * 微信支付工具类
 * 提供签名生成、验签、支付参数生成等工具方法
 *
 * @author youpinmall
 * @date 2026-03-05
 */
@Component
public class WeChatPayUtil {

    private static final Logger log = LoggerFactory.getLogger(WeChatPayUtil.class);

    private final WeChatPayProperties properties;
    private PrivateKey privateKey;
    private X509Certificate platformCertificate;

    public WeChatPayUtil(WeChatPayProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取商户私钥（懒加载）
     */
    private PrivateKey getPrivateKey() throws Exception {
        if (privateKey == null) {
            String privateKeyPath = properties.getPrivateKeyPath();
            if (privateKeyPath.startsWith("classpath:")) {
                String resourcePath = privateKeyPath.substring("classpath:".length());
                InputStream inputStream = this.getClass().getResourceAsStream(resourcePath);
                if (inputStream == null) {
                    throw new RuntimeException("私钥文件不存在：" + privateKeyPath);
                }
                privateKey = loadPrivateKey(inputStream);
            } else {
                privateKey = loadPrivateKey(new FileInputStream(privateKeyPath));
            }
        }
        return privateKey;
    }

    /**
     * 从输入流加载私钥
     */
    private PrivateKey loadPrivateKey(InputStream inputStream) throws Exception {
        String privateKeyStr = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        privateKeyStr = privateKeyStr.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr);
        java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
        java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

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
     * 生成 JSAPI 支付签名
     *
     * @param appId     小程序 AppID
     * @param timeStamp 时间戳
     * @param nonceStr  随机字符串
     * @param packageValue 订单详情扩展字符串
     * @return 签名
     * @throws Exception 签名生成异常
     */
    public String generateJsapiPaySign(String appId, String timeStamp, String nonceStr, String packageValue)
            throws Exception {
        String message = appId + "\n" +
                timeStamp + "\n" +
                nonceStr + "\n" +
                packageValue + "\n";
        return generateSignature(message, getPrivateKey());
    }

    /**
     * 生成签名
     *
     * @param message 待签名消息
     * @param privateKey 私钥
     * @return Base64 编码的签名
     * @throws Exception 签名生成异常
     */
    public String generateSignature(String message, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    /**
     * 验证微信支付回调签名
     *
     * @param body      回调请求体
     * @param signature 签名
     * @param timestamp 时间戳
     * @param nonce     随机字符串
     * @param serial    证书序列号
     * @return 验证结果
     * @throws Exception 验签异常
     */
    public boolean verifyCallbackSignature(String body, String signature, String timestamp,
                                           String nonce, String serial) throws Exception {
        String message = timestamp + "\n" +
                nonce + "\n" +
                body + "\n";

        // 获取微信支付平台证书
        X509Certificate certificate = getPlatformCertificate();
        if (certificate == null) {
            log.error("未找到证书序列号：{}", serial);
            return false;
        }

        java.security.PublicKey publicKey = certificate.getPublicKey();
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(message.getBytes(StandardCharsets.UTF_8));

        boolean result = sig.verify(Base64.getDecoder().decode(signature));
        log.debug("微信支付回调签名验证结果：{}", result);
        return result;
    }

    /**
     * 解密微信支付回调数据
     *
     * @param ciphertext 密文
     * @param associatedData 附加数据
     * @param nonce 随机串
     * @param apiV3Key APIv3 密钥
     * @return 解密后的明文
     * @throws Exception 解密异常
     */
    public String decryptCallbackData(String ciphertext, String associatedData,
                                      String nonce, String apiV3Key) throws Exception {
        byte[] keyBytes = apiV3Key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);

        byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);
        byte[] plaintextBytes = cipher.doFinal(ciphertextBytes);

        return new String(plaintextBytes, StandardCharsets.UTF_8);
    }

    /**
     * 构建 JSAPI 预支付请求
     *
     * @param appId      小程序 AppID
     * @param mchId      商户号
     * @param outTradeNo 商户订单号
     * @param description 商品描述
     * @param amount     订单金额（单位：分）
     * @param notifyUrl  回调地址
     * @param openid     用户 openid
     * @return 预支付请求对象
     */
    public PrepayRequest buildJsapiPrepayRequest(String appId, String mchId, String outTradeNo,
                                                  String description, Integer amount,
                                                  String notifyUrl, String openid) {
        log.info("构建 JSAPI 预支付请求 - appId={}, mchId={}, outTradeNo={}, amount={}, description={}, openid={}",
                 appId, mchId, outTradeNo, amount, description, openid);

        // 参数验证
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("金额必须为正整数（单位：分），当前值：" + amount);
        }
        if (openid == null || openid.isEmpty()) {
            throw new IllegalArgumentException("JSAPI 支付必须提供 openid");
        }
        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException("商品描述不能为空");
        }

        PrepayRequest request = new PrepayRequest();
        request.setAppid(appId);
        request.setMchid(mchId);
        request.setDescription(description);
        request.setOutTradeNo(outTradeNo);
        request.setNotifyUrl(notifyUrl);

        Amount requestAmount = new Amount();
        requestAmount.setTotal(amount);
        requestAmount.setCurrency("CNY");
        request.setAmount(requestAmount);

        Payer payer = new Payer();
        payer.setOpenid(openid);
        request.setPayer(payer);

        log.info("JSAPI 预支付请求构建成功 - 金额（分）：{}, 商品描述：{}", amount, description);
        return request;
    }

    /**
     * 构建 Native 支付预支付请求
     *
     * @param appId      小程序 AppID
     * @param mchId      商户号
     * @param outTradeNo 商户订单号
     * @param description 商品描述
     * @param amount     订单金额（单位：分）
     * @param notifyUrl  回调地址
     * @return 预支付请求对象
     */
    public com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest buildNativePrepayRequest(
            String appId, String mchId, String outTradeNo,
            String description, Integer amount, String notifyUrl) {
        log.info("构建 Native 支付预支付请求 - appId={}, mchId={}, outTradeNo={}, amount={}, description={}",
                 appId, mchId, outTradeNo, amount, description);

        // 参数验证
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("金额必须为正整数（单位：分），当前值：" + amount);
        }
        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException("商品描述不能为空");
        }

        com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest request =
            new com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest();
        request.setAppid(appId);
        request.setMchid(mchId);
        request.setDescription(description);
        request.setOutTradeNo(outTradeNo);
        request.setNotifyUrl(notifyUrl);

        com.wechat.pay.java.service.payments.nativepay.model.Amount requestAmount =
            new com.wechat.pay.java.service.payments.nativepay.model.Amount();
        requestAmount.setTotal(amount);
        requestAmount.setCurrency("CNY");
        request.setAmount(requestAmount);

        log.info("Native 支付预支付请求构建成功 - 金额（分）：{}, 商品描述：{}", amount, description);
        return request;
    }

    /**
     * 构建退款请求
     *
     * @param outTradeNo   商户订单号
     * @param outRefundNo  商户退款单号
     * @param refundAmount 退款金额（单位：分）
     * @param totalAmount  原订单金额（单位：分）
     * @param reason       退款原因
     * @param notifyUrl    退款回调地址
     * @return 退款请求对象
     * @deprecated SDK 0.2.17 退款API待确认，暂时返回null
     */
    @Deprecated
    public Object buildRefundRequest(String outTradeNo, String outRefundNo,
                                                   Integer refundAmount, Integer totalAmount,
                                                   String reason, String notifyUrl) {
        log.warn("SDK 0.2.17 退款API待确认，暂时返回null");
        return null;
        // TODO: 待确认 SDK 0.2.17 正确的退款请求类名
        // RefundRequest request = new RefundRequest();
        // request.setOutTradeNo(outTradeNo);
        // request.setOutRefundNo(outRefundNo);
        // request.setNotifyUrl(notifyUrl);
        //
        // if (reason != null && !reason.isEmpty()) {
        //     request.setReason(reason);
        // }
        //
        // com.wechat.pay.java.service.refund.model.Amount refundReqAmount =
        //     new com.wechat.pay.java.service.refund.model.Amount();
        // refundReqAmount.setRefund(refundAmount);
        // refundReqAmount.setTotal(totalAmount);
        // refundReqAmount.setCurrency("CNY");
        // request.setAmount(refundReqAmount);
        //
        // return request;
    }

    /**
     * 生成随机字符串
     *
     * @return 随机字符串
     */
    public String generateNonceStr() {
        return NonceUtil.createNonce(32);
    }

    /**
     * 生成退款单号
     *
     * @param paymentNo 支付单号
     * @return 退款单号
     */
    public String generateRefundNo(String paymentNo) {
        return "REF" + paymentNo.substring(3) + System.currentTimeMillis();
    }

    /**
     * 获取支付状态描述
     *
     * @param tradeState 支付状态码
     * @return 状态描述
     */
    public String getTradeStateDesc(String tradeState) {
        if (tradeState == null) {
            return "未知状态";
        }
        switch (tradeState) {
            case "SUCCESS":
                return "支付成功";
            case "REFUND":
                return "转入退款";
            case "NOTPAY":
                return "未支付";
            case "CLOSED":
                return "已关闭";
            case "REVOKED":
                return "已撤销（付款码支付）";
            case "USERPAYING":
                return "用户支付中（付款码支付）";
            case "PAYERROR":
                return "支付失败（其他原因，如银行返回失败）";
            default:
                return tradeState;
        }
    }

    /**
     * 获取退款状态描述
     *
     * @param refundStatus 退款状态码
     * @return 状态描述
     */
    public String getRefundStatusDesc(String refundStatus) {
        if (refundStatus == null) {
            return "未知状态";
        }
        switch (refundStatus) {
            case "SUCCESS":
                return "退款成功";
            case "EXCEPTION":
                return "退款异常";
            case "CHANGE":
                return "退款转银行处理";
            default:
                return refundStatus;
        }
    }

    /**
     * 将金额（元）转换为分
     *
     * @param amount 金额（元）
     * @return 金额（分）
     */
    public Integer convertToCents(java.math.BigDecimal amount) {
        return amount.multiply(new java.math.BigDecimal("100")).intValue();
    }

    /**
     * 将金额（分）转换为元
     *
     * @param cents 金额（分）
     * @return 金额（元）
     */
    public java.math.BigDecimal convertToYuan(Integer cents) {
        return new java.math.BigDecimal(cents).divide(new java.math.BigDecimal("100"));
    }
}
