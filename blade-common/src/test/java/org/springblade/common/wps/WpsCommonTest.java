package org.springblade.common.wps;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WPS WebOffice 公共层单测（P1 验收）。
 *
 * <p>重点覆盖三条跨模块约束：
 * ① 签名串格式必须与存量 {@code DocUrlSigner}/{@code OdocUrlSigner} 完全一致；
 * ② 两侧必须使用同一密钥，否则跨服务签发的 URL 无法互验；
 * ③ 结构化 fileId 的域隔离，防止 odoc 请求越权读到知识管理的文件。</p>
 */
class WpsCommonTest {

	private static final String SECRET = "doc-dev-secret-change-me";

	// ───────────── WpsUrlSigner ─────────────

	@Test
	void sign_verify_roundTrip() {
		WpsUrlSigner signer = new WpsUrlSigner(SECRET, 300);
		String token = signer.sign("wps:doc:123");

		assertThat(token).contains(".");
		assertThat(signer.verify("wps:doc:123", token)).isTrue();
	}

	@Test
	void sign_format_compatibleWithLegacySigner() {
		// 存量实现：exp + "." + hex(HmacSHA256(resource:exp))，hex 为小写
		assertThat(new WpsUrlSigner(SECRET, 300).sign("abc"))
			.matches("^\\d+\\.[0-9a-f]{64}$");
	}

	@Test
	void verify_rejectsTamperedResourceOrToken() {
		WpsUrlSigner signer = new WpsUrlSigner(SECRET, 300);
		String token = signer.sign("wps:doc:123");

		assertThat(signer.verify("wps:doc:999", token)).isFalse();
		assertThat(signer.verify("wps:doc:123", token + "x")).isFalse();
		assertThat(signer.verify("wps:doc:123", "garbage")).isFalse();
		assertThat(signer.verify("wps:doc:123", null)).isFalse();
	}

	@Test
	void verify_rejectsDifferentSecret_acrossModules() {
		// 关键约束：blade-doc 与 blade-workflow 必须用同一 urlSignSecret
		String token = new WpsUrlSigner("doc-secret", 300).sign("wps:odoc:7");
		assertThat(new WpsUrlSigner("odoc-secret", 300).verify("wps:odoc:7", token)).isFalse();
		assertThat(new WpsUrlSigner("doc-secret", 300).verify("wps:odoc:7", token)).isTrue();
	}

	// ───────────── WpsFileId ─────────────

	@Test
	void fileId_structuredEncodeDecode() {
		WpsFileId doc = WpsFileId.parse(WpsFileId.of("doc", "123"));
		assertThat(doc.domain()).isEqualTo("doc");
		assertThat(doc.bizId()).isEqualTo("123");
		assertThat(doc.legacy()).isFalse();
		assertThat(doc.wireValue()).isEqualTo("doc:123");

		WpsFileId odoc = WpsFileId.parse("odoc:456");
		assertThat(odoc.domain()).isEqualTo("odoc");
		assertThat(odoc.bizId()).isEqualTo("456");
	}

	@Test
	void fileId_odocWireFormUsesUnderscore() {
		// WPS 中台要求 file_id 只能由数字/字母/下划线组成且不以下划线开头，
		// 故公文域下发 odoc_{docId}；知识管理域沿用纯数字 legacy 不受影响
		assertThat(WpsFileId.wireValue("odoc", "456")).isEqualTo("odoc_456");

		WpsFileId wire = WpsFileId.parse("odoc_456");
		assertThat(wire.domain()).isEqualTo("odoc");
		assertThat(wire.bizId()).isEqualTo("456");
		assertThat(wire.legacy()).isFalse();
		// 幂等：解析后再序列化仍是下划线形态
		assertThat(wire.wireValue()).isEqualTo("odoc_456");

		// 旧冒号格式仍可解析（迁移期兼容），但对外形态已切换为下划线
		WpsFileId legacyColon = WpsFileId.parse("odoc:456");
		assertThat(legacyColon.wireValue()).isEqualTo("odoc_456");

		// 非法下划线形态必须拒绝（域不匹配 / bizId 非数字）
		assertThat(WpsFileId.parse("doc_456")).isNull();
		assertThat(WpsFileId.parse("odoc_abc")).isNull();
		assertThat(WpsFileId.parse("_456")).isNull();
	}

	@Test
	void fileId_legacyNumericFallsBackToDoc() {
		// 知识管理既有 WPS 回调用纯数字 fileId，迁移期不清洗历史链接
		WpsFileId legacy = WpsFileId.parse("2113499007952486402");
		assertThat(legacy.domain()).isEqualTo("doc");
		assertThat(legacy.bizId()).isEqualTo("2113499007952486402");
		assertThat(legacy.legacy()).isTrue();
		assertThat(legacy.wireValue()).isEqualTo("2113499007952486402");
	}

	@Test
	void fileId_domainIsolation_blocksCrossDomainAccess() {
		// odoc 的 Provider 不得凭纯数字/ doc 前缀 fileId 读到知识管理文件
		assertThat(WpsFileId.parseFor("doc:123", "odoc")).isNull();
		assertThat(WpsFileId.parseFor("2113499007952486402", "odoc")).isNull();
		assertThat(WpsFileId.parseFor("odoc:123", "odoc")).isNotNull();
	}

	@Test
	void fileId_rejectsInvalid() {
		assertThat(WpsFileId.parse(null)).isNull();
		assertThat(WpsFileId.parse("  ")).isNull();
		assertThat(WpsFileId.parse("unknown:1")).isNull();
		assertThat(WpsFileId.parse("doc:")).isNull();
		assertThat(WpsFileId.parse(":123")).isNull();
		assertThat(WpsFileId.parse("abc")).isNull();
	}

	// ───────────── WpsCallbackSupport ─────────────

	@Test
	void callback_protocolEnvelope() {
		Map<String, Object> ok = WpsCallbackSupport.ok(Map.of("url", "https://x/y"));
		assertThat(ok.get("code")).isEqualTo(0);
		assertThat(ok.get("message")).isEqualTo("");
		assertThat(ok.get("data")).isInstanceOf(Map.class);

		Map<String, Object> err = WpsCallbackSupport.err(40005, "file_id invalid");
		assertThat(err.get("code")).isEqualTo(40005);
		assertThat(err).doesNotContainKey("data");
	}

	@Test
	void callback_tokenValid_isHardCheck() {
		WpsUrlSigner signer = new WpsUrlSigner(SECRET, 300);
		String token = signer.sign("wps:doc:123");

		assertThat(WpsCallbackSupport.tokenValid(signer, "doc:123", token)).isTrue();
		assertThat(WpsCallbackSupport.tokenValid(signer, "doc:123", null)).isFalse();
		assertThat(WpsCallbackSupport.tokenValid(signer, "doc:999", token)).isFalse();
	}

	@Test
	void callback_tokenShapeValid_rejectsMissingExpiredAndMalformed() {
		WpsUrlSigner signer = new WpsUrlSigner(SECRET, 300);
		assertThat(WpsCallbackSupport.tokenShapeValid(signer.sign("wps:doc:123"))).isTrue();

		assertThat(WpsCallbackSupport.tokenShapeValid(null)).isFalse();
		assertThat(WpsCallbackSupport.tokenShapeValid("")).isFalse();
		assertThat(WpsCallbackSupport.tokenShapeValid("forged")).isFalse();
		assertThat(WpsCallbackSupport.tokenShapeValid("123.deadbeef")).isFalse();
		assertThat(WpsCallbackSupport.tokenShapeValid("not-a-number." + "a".repeat(64))).isFalse();
		assertThat(WpsCallbackSupport.tokenShapeValid(
			(System.currentTimeMillis() / 1000 - 10) + "." + "a".repeat(64))).isFalse();
		assertThat(WpsCallbackSupport.tokenShapeValid(
			(System.currentTimeMillis() / 1000 + 300) + "." + "z".repeat(64))).isFalse();
	}

	@Test
	void callback_internalResource_isStableAndOpScoped() {
		WpsUrlSigner signer = new WpsUrlSigner(SECRET, 300);
		String resource = WpsCallbackSupport.internalResource("meta", 7L);
		assertThat(resource).isEqualTo("wps:odoc:meta:7");
		assertThat(signer.verify(resource, signer.sign(resource))).isTrue();

		// 不同 op / 不同 bizId 的 token 不能互用，防止服务间端点被按 docId 枚举
		String token = signer.sign(resource);
		assertThat(signer.verify(WpsCallbackSupport.internalResource("versions", 7L), token)).isFalse();
		assertThat(signer.verify(WpsCallbackSupport.internalResource("meta", 8L), token)).isFalse();
		assertThat(WpsCallbackSupport.HEADER_INTERNAL_TOKEN).isEqualTo("X-Wps-Internal-Token");
	}

	@Test
	void callback_userQuery_decodesUtf8() {
		Map<String, String> q = WpsCallbackSupport.userQuery("userId=3&userName=%E5%BC%A0%E4%B8%89");
		assertThat(q).containsEntry("userId", "3").containsEntry("userName", "张三");
		assertThat(WpsCallbackSupport.userQuery(null)).isEmpty();
	}

	@Test
	void callback_publicBase_stripsTrailingSlash() {
		assertThat(WpsCallbackSupport.publicBase("https://www.zwymding.top///", "http", "localhost", 80))
			.isEqualTo("https://www.zwymding.top");
		assertThat(WpsCallbackSupport.publicBase("", "http", "localhost", 8120))
			.isEqualTo("http://localhost:8120");
		assertThat(WpsCallbackSupport.publicBase(null, "https", "www.x.com", 443))
			.isEqualTo("https://www.x.com");
	}

	// ───────────── WpsProperties ─────────────

	@Test
	void properties_isConfigured_requiresBothCredentials() {
		WpsProperties p = new WpsProperties();
		assertThat(p.isConfigured()).isFalse();
		p.setAppId("id");
		assertThat(p.isConfigured()).isFalse();
		p.setAppSecret("secret");
		assertThat(p.isConfigured()).isTrue();
		assertThat(p.isCallbackAuthStrict()).isTrue();
	}
}
