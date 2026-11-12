package org.springblade.doc.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springblade.common.wps.WpsCallbackSupport;
import org.springblade.common.wps.WpsFileMeta;
import org.springblade.common.wps.WpsFileProvider;
import org.springblade.common.wps.WpsProperties;
import org.springblade.common.wps.WpsUrlSigner;
import org.springblade.doc.config.DocProperties;
import org.springblade.doc.mapper.DocImageFileMapper;
import org.springblade.doc.sign.DocUrlSigner;
import org.springblade.doc.store.IDocFileStore;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WPS v3 回调协议编排层回归（P1-1.8）。
 *
 * <p>WPS 中台是外部依赖，本地无法跑通真实「编辑→保存」全链路；这里用 Mock Provider
 * 固化协议编排的可回归部分：端点响应格式、{@code WpsFileId} 按域路由、存量纯数字
 * fileId 兼容、uploadUrl 原样回带 wireFileId、token 校验口径。</p>
 *
 * <p>真实 WPS 中台联调（发起编辑→三阶段保存→历史版本）仍需在联调环境人工验收，
 * 见 {@code doc/md/收发文落地-编辑器WPS统一改造方案.md} P1.8。</p>
 */
class WpsCallbackProtocolTest {

	private static final String SECRET = "doc-dev-secret-change-me";

	private WpsCallbackController controller;
	private StubProvider docProvider;
	private WpsUrlSigner signer;

	@BeforeEach
	void setUp() {
		docProvider = new StubProvider();
		signer = new WpsUrlSigner(SECRET, 300);
		controller = buildController(true);
	}

	/** strict=true（生产默认硬校验）/ false（联调临时放行） */
	private WpsCallbackController buildController(boolean strict) {
		return buildController(strict, docProvider);
	}

	private WpsCallbackController buildController(boolean strict, WpsFileProvider... providers) {
		return buildController(strict, false, providers);
	}

	private WpsCallbackController buildController(boolean strict, boolean bindResource, WpsFileProvider... providers) {
		WpsProperties wps = new WpsProperties();
		wps.setCallbackAuthStrict(strict);
		wps.setCallbackAuthBindResource(bindResource);
		DocProperties props = new DocProperties();
		props.setUrlSignSecret(SECRET);
		return new WpsCallbackController(
			List.of(providers),
			new DocUrlSigner(new WpsProperties(), props), signer,
			mock(DocImageFileMapper.class), mock(IDocFileStore.class), mock(JdbcTemplate.class), wps);
	}

	private static MockHttpServletRequest req(String fileId) {
		MockHttpServletRequest r = new MockHttpServletRequest();
		r.setScheme("https");
		r.setServerName("www.zwymding.top");
		r.setServerPort(443);
		return r;
	}

	private MockHttpServletRequest authed(String wireFileId) {
		MockHttpServletRequest r = req(wireFileId);
		r.addHeader("X-WebOffice-Token", signer.sign("wps:" + wireFileId));
		return r;
	}

	/** 形状合法（真实签发）但 resource 与当前会话无关的 token，辅助端点只验形状 */
	private MockHttpServletRequest auxAuthed() {
		MockHttpServletRequest r = req(null);
		r.addHeader("X-WebOffice-Token", signer.sign("wps:any-session"));
		return r;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> body(ResponseEntity<Map<String, Object>> resp) {
		return resp.getBody();
	}

	// ───────────── 路由 ─────────────

	@Test
	void info_legacyNumericFileId_routesToDocProvider() {
		ResponseEntity<Map<String, Object>> resp = controller.info(authed("123"), "123");
		Map<String, Object> b = body(resp);

		assertThat(b.get("code")).isEqualTo(0);
		Map<String, Object> data = (Map<String, Object>) b.get("data");
		assertThat(data.get("id")).isEqualTo("123");
		assertThat(data.get("name")).isEqualTo("a.docx");
		assertThat(docProvider.lastBizId).isEqualTo("123");
	}

	@Test
	void info_structuredDocFileId_routesToDocProvider() {
		ResponseEntity<Map<String, Object>> resp = controller.info(authed("doc:456"), "doc:456");
		Map<String, Object> data = (Map<String, Object>) body(resp).get("data");

		assertThat(body(resp).get("code")).isEqualTo(0);
		assertThat(data.get("id")).isEqualTo("doc:456");
		assertThat(docProvider.lastBizId).isEqualTo("456");
	}

	@Test
	void info_odocDomainNotRegistered_returns40005() {
		// P2 才会注册 odoc Provider；P1 阶段未知域必须拒绝，且不得触达 doc Provider
		String before = docProvider.lastBizId;
		ResponseEntity<Map<String, Object>> resp = controller.info(authed("odoc:7"), "odoc:7");

		assertThat(body(resp).get("code")).isEqualTo(40005);
		assertThat(docProvider.lastBizId).isEqualTo(before);
	}

	@Test
	void info_invalidFileId_returns40005() {
		for (String bad : new String[]{"unknown:1", "doc:", "abc", ":9"}) {
			assertThat(body(controller.info(authed(bad), bad)).get("code")).isEqualTo(40005);
		}
	}

	// ───────────── 协议响应 ─────────────

	@Test
	void download_returnsAbsoluteUrl() {
		ResponseEntity<Map<String, Object>> resp = controller.download(authed("123"), "123");
		Map<String, Object> data = (Map<String, Object>) body(resp).get("data");

		assertThat(data.get("url")).asString().startsWith("https://www.zwymding.top/doc/file/");
		assertThat(data.get("url")).asString().contains("token=");
	}

	@Test
	void permission_passesUserQuery() {
		MockHttpServletRequest r = authed("123");
		r.addHeader("X-User-Query", "userId=3&userName=%E5%BC%A0%E4%B8%89");
		Map<String, Object> data = (Map<String, Object>) body(controller.permission(r, "123")).get("data");

		assertThat(data.get("user_id")).isEqualTo("3");
		assertThat(data.get("update")).isEqualTo(1);
		assertThat(docProvider.lastUserQuery).containsEntry("userName", "张三");
	}

	@Test
	void prepare_returnsSha1DigestTypes() {
		Map<String, Object> data = (Map<String, Object>) body(controller.prepare(authed("123"), "123")).get("data");
		assertThat(data.get("digest_types")).isEqualTo(List.of("sha1"));
	}

	@Test
	void address_returnsPutUrlWithOriginalWireFileId() {
		// 关键：WPS 中台会话里是纯数字 fileId，address 不得改成 doc:123
		Map<String, Object> data = (Map<String, Object>) body(
			controller.address(authed("123"), "123", Map.of("name", "正文.docx"))).get("data");

		assertThat(data.get("method")).isEqualTo("PUT");
		assertThat(data.get("url")).asString().contains("/v3/3rd/files/123/upload");
		assertThat(data.get("url")).asString().doesNotContain("doc%3A").doesNotContain("doc:");
		assertThat(data.get("url")).asString().contains("name=");
		assertThat(docProvider.lastWireFileId).isEqualTo("123");
	}

	@Test
	void uploadPut_emptyBody_rejectedWithoutCallingProvider() {
		ResponseEntity<Map<String, Object>> resp = controller.uploadPut(authed("123"), "123", null, new byte[0]);

		assertThat(body(resp).get("code")).isEqualTo(40005);
		assertThat(docProvider.saved).isFalse();
	}

	@Test
	void uploadPut_delegatesSaveAndReturnsVersionData() {
		ResponseEntity<Map<String, Object>> resp = controller.uploadPut(
			authed("123"), "123", "新版.docx", "hello".getBytes());
		Map<String, Object> data = (Map<String, Object>) body(resp).get("data");

		assertThat(body(resp).get("code")).isEqualTo(0);
		assertThat(docProvider.saved).isTrue();
		assertThat(docProvider.savedBizId).isEqualTo("123");
		assertThat(data.get("id")).isEqualTo("123");
		assertThat(data.get("version")).isEqualTo(2);
	}

	@Test
	void rename_andVersions_followProtocol() {
		assertThat(body(controller.rename(authed("123"), "123", Map.of("name", "改名.docx"))).get("code"))
			.isEqualTo(0);
		assertThat(docProvider.renamedTo).isEqualTo("改名.docx");

		ResponseEntity<Map<String, Object>> versions = controller.versions(authed("123"), "123", 0, 10);
		List<Map<String, Object>> arr = (List<Map<String, Object>>) body(versions).get("data");
		assertThat(arr).hasSize(2);
		assertThat(arr.get(0).get("id")).isEqualTo("123");
		assertThat(arr.get(1).get("version")).isEqualTo(2);
	}

	@Test
	void versionInfo_outOfRange_returns40005() {
		assertThat(body(controller.versionInfo(authed("123"), "123", 99)).get("code")).isEqualTo(40005);
	}

	@Test
	void unknownDomain_allFileEndpointsRejected() {
		assertThat(body(controller.download(authed("odoc:1"), "odoc:1")).get("code")).isEqualTo(40005);
		assertThat(body(controller.permission(authed("odoc:1"), "odoc:1")).get("code")).isEqualTo(40005);
		assertThat(body(controller.address(authed("odoc:1"), "odoc:1", null)).get("code")).isEqualTo(40005);
		assertThat(body(controller.complete(authed("odoc:1"), "odoc:1")).get("code")).isEqualTo(40005);
	}

	// ───────────── P3.3 / P3.6 鉴权与越权 ─────────────

	@Test
	void strict_rejectsMissingToken_beforeProviderRouting() {
		MockHttpServletRequest r = req("123");
		assertThat(body(controller.info(r, "123")).get("code")).isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);
		assertThat(body(controller.download(r, "123")).get("code")).isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);
		assertThat(body(controller.permission(r, "123")).get("code")).isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);
		assertThat(body(controller.prepare(r, "123")).get("code")).isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);
		assertThat(body(controller.address(r, "123", null)).get("code")).isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);
		assertThat(body(controller.uploadPut(r, "123", null, "x".getBytes())).get("code"))
			.isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);
		assertThat(body(controller.complete(r, "123")).get("code")).isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);
		assertThat(body(controller.rename(r, "123", Map.of("name", "a.docx"))).get("code"))
			.isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);
		assertThat(body(controller.versions(r, "123", 0, 10)).get("code"))
			.isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);
		assertThat(body(controller.versionInfo(r, "123", 1)).get("code"))
			.isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);
		assertThat(body(controller.versionDownload(r, "123", 1)).get("code"))
			.isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);

		// 拒绝必须发生在 Provider 路由之前，未授权请求不得触达业务存储
		assertThat(docProvider.lastBizId).isNull();
		assertThat(docProvider.saved).isFalse();
	}

	@Test
	void strict_rejectsForgedExpiredAndWrongResourceTokens() {
		long expired = System.currentTimeMillis() / 1000 - 10;
		String expiredToken = expired + "." + "a".repeat(64);
		for (String bad : new String[]{"forged", "123.deadbeef", expiredToken, signer.sign("wps:999")}) {
			MockHttpServletRequest r = req("123");
			r.addHeader(WpsCallbackSupport.HEADER_TOKEN, bad);
			assertThat(body(controller.info(r, "123")).get("code"))
				.as("token=%s", bad).isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);
		}
		assertThat(docProvider.lastBizId).isNull();
	}

	@Test
	void lenient_allowsMissingTokenWhenStrictFalse() {
		// 联调排障用；此时严禁打开网关 /v3/3rd/** 白名单
		WpsCallbackController lenient = buildController(false);
		assertThat(body(lenient.info(req("123"), "123")).get("code")).isEqualTo(0);
		assertThat(docProvider.lastBizId).isEqualTo("123");
	}

	@Test
	void auxEndpoints_rejectMissingToken() {
		MockHttpServletRequest r = req(null);
		assertThat(body(controller.objectPut(r, "k1", "a.png", "x".getBytes())).get("code"))
			.isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);
		assertThat(body(controller.objectUrl(r, "k1")).get("code"))
			.isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);
		assertThat(body(controller.objectCopy(r, Map.of("key_dict", Map.of("a", "b")))).get("code"))
			.isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);
		assertThat(body(controller.users(r, List.of("3"))).get("code"))
			.isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);
	}

	@Test
	void auxEndpoints_acceptShapedToken() {
		MockHttpServletRequest r = auxAuthed();
		assertThat(body(controller.users(r, List.of("3"))).get("code")).isEqualTo(0);
		assertThat(body(controller.objectPut(r, "k1", "a.png", "x".getBytes())).get("code")).isEqualTo(0);
		// 鉴权通过后因 mock 无附件记录返回「object not found」，而非鉴权失败
		assertThat(body(controller.objectUrl(r, "k1")).get("code"))
			.isEqualTo(WpsCallbackSupport.CODE_FILE_INVALID);
	}

	@Test
	void auxBindResource_acceptsTokenMatchingCallbackFileId() {
		WpsCallbackController c = buildController(true, true, docProvider);
		MockHttpServletRequest r = req(null);
		r.addHeader(WpsCallbackSupport.HEADER_FILE_ID, "doc:456");
		r.addHeader(WpsCallbackSupport.HEADER_TOKEN, signer.sign("wps:doc:456"));

		assertThat(body(c.users(r, List.of("3"))).get("code")).isEqualTo(0);
	}

	@Test
	void auxBindResource_rejectsMismatchedTokenAndMissingFileId() {
		WpsCallbackController c = buildController(true, true, docProvider);
		// file_id 与 token 签发的 resource 不符
		MockHttpServletRequest bad = req(null);
		bad.addHeader(WpsCallbackSupport.HEADER_FILE_ID, "doc:456");
		bad.addHeader(WpsCallbackSupport.HEADER_TOKEN, signer.sign("wps:doc:999"));
		assertThat(body(c.users(bad, List.of("3"))).get("code"))
			.isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);

		// 形状合法但不带 file_id：不允许留旁路
		assertThat(body(c.users(auxAuthed(), List.of("3"))).get("code"))
			.isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);
	}

	@Test
	void auxBindResource_queryFileIdFallback() {
		WpsCallbackController c = buildController(true, true, docProvider);
		MockHttpServletRequest r = req(null);
		r.setParameter("file_id", "123");
		r.addHeader(WpsCallbackSupport.HEADER_TOKEN, signer.sign("wps:123"));

		assertThat(body(c.users(r, List.of("3"))).get("code")).isEqualTo(0);
	}

	@Test
	void crossDomainFileId_neverReachesOtherProvider() {
		StubProvider odocProvider = new StubProvider("odoc");
		WpsCallbackController c = buildController(true, docProvider, odocProvider);

		// 拿 doc 域会话的 token 打 odoc fileId：resource 不匹配，必须拒
		MockHttpServletRequest cross = req("odoc:7");
		cross.addHeader(WpsCallbackSupport.HEADER_TOKEN, signer.sign("wps:doc:456"));
		assertThat(body(c.info(cross, "odoc:7")).get("code"))
			.isEqualTo(WpsCallbackSupport.CODE_AUTH_FAILED);
		assertThat(odocProvider.lastBizId).isNull();
		assertThat(docProvider.lastBizId).isNull();

		assertThat(body(c.info(authed("odoc:7"), "odoc:7")).get("code")).isEqualTo(0);
		assertThat(odocProvider.lastBizId).isEqualTo("7");
		assertThat(docProvider.lastBizId).isNull();

		assertThat(body(c.info(authed("doc:456"), "doc:456")).get("code")).isEqualTo(0);
		assertThat(docProvider.lastBizId).isEqualTo("456");
	}

	// ───────────── Stub Provider ─────────────

	private static WpsFileMeta meta(int version) {
		WpsFileMeta m = new WpsFileMeta();
		m.setId("doc:123");
		m.setName("a.docx");
		m.setVersion(version);
		m.setSize(10L);
		return m;
	}

	private static class StubProvider implements WpsFileProvider {
		private final String domain;
		String lastBizId;
		String lastWireFileId;
		String renamedTo;
		Map<String, String> lastUserQuery;
		boolean saved;
		String savedBizId;

		StubProvider() { this("doc"); }
		StubProvider(String domain) { this.domain = domain; }

		@Override public String domain() { return domain; }
		@Override public String publicBase() { return "https://www.zwymding.top"; }

		@Override public WpsFileMeta info(String bizId) { lastBizId = bizId; return meta(1); }
		@Override public String downloadUrl(String bizId) { lastBizId = bizId; return "/doc/file/1/content?token=t"; }
		@Override public String uploadUrl(String wireFileId, String name) {
			lastWireFileId = wireFileId;
			return "/v3/3rd/files/" + wireFileId + "/upload?name=" + (name == null ? "" : name);
		}
		@Override public WpsFileMeta saveNewVersion(String bizId, byte[] content, String name) {
			saved = true; savedBizId = bizId; return meta(2);
		}
		@Override public List<WpsFileMeta> versions(String bizId) {
			List<WpsFileMeta> l = new ArrayList<>(); l.add(meta(1)); l.add(meta(2)); return l;
		}
		@Override public String versionDownloadUrl(String bizId, int version) {
			return version == 1 ? "/doc/file/1/content?token=t" : null;
		}
		@Override public void rename(String bizId, String name) { renamedTo = name; }
		@Override public Map<String, Object> permission(String bizId, Map<String, String> userQuery) {
			lastUserQuery = userQuery;
			Map<String, Object> r = new LinkedHashMap<>();
			r.put("update", 1);
			r.put("user_id", userQuery.getOrDefault("userId", "0"));
			return r;
		}
	}
}
