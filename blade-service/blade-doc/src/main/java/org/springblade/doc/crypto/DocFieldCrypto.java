package org.springblade.doc.crypto;

import org.springblade.doc.config.DocProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 自定义字段加解密（AES）。
 *
 * <p><b>修复 E9 §18.4「加密静默失效」</b>：E9 因写入用 {@code HrmCustomFieldByInfoType}、
 * 读取用 {@code DocCustomFieldBySecCategory}，两处 scope 不一致导致加密被静默跳过、明文入库。
 * 本实现读与写共用同一 {@link DocFieldCrypto}（同一 key/口径），不可能出现 scope 错配；
 * 并由 {@code DocFieldCryptoTest} 断言「确实加密 + 可还原」，防止再次静默失效。</p>
 */
@Component
public class DocFieldCrypto {

    private final DocProperties properties;

    public DocFieldCrypto(DocProperties properties) {
        this.properties = properties;
    }

    public String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey());
            return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("字段加密失败", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey());
            return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("字段解密失败", e);
        }
    }

    private SecretKeySpec secretKey() {
        byte[] raw = properties.getFieldEncryptKey().getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[16];
        System.arraycopy(raw, 0, key, 0, Math.min(raw.length, 16));
        return new SecretKeySpec(key, "AES");
    }
}
