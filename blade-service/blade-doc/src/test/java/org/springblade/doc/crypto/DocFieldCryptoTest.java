package org.springblade.doc.crypto;

import org.junit.jupiter.api.Test;
import org.springblade.doc.config.DocProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 字段加解密回归测试：守住 E9 §18.4「加密静默失效」不再复现。
 *
 * <p>两条断言：① 密文 ≠ 明文（加密确实生效，未被静默跳过）；
 * ② decrypt(encrypt(x)) == x（读与写口径一致，可还原）。</p>
 */
class DocFieldCryptoTest {

    private DocFieldCrypto crypto() {
        DocProperties p = new DocProperties();
        p.setFieldEncryptKey("doc-field-enc-dev-key");
        return new DocFieldCrypto(p);
    }

    @Test
    void roundTrip_encryptsAndRestores() {
        DocFieldCrypto c = crypto();
        String plain = "机密内容 Sensitive-123 　";
        String enc = c.encrypt(plain);
        assertNotEquals(plain, enc, "加密字段必须落密文，禁止静默跳过");
        assertEquals(plain, c.decrypt(enc), "读与写须同一加密口径，保证可还原");
    }

    @Test
    void nullSafe() {
        DocFieldCrypto c = crypto();
        assertNull(c.encrypt(null));
        assertNull(c.decrypt(null));
    }
}
