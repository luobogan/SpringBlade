package org.springblade.doc.online;

import org.junit.jupiter.api.Test;
import org.springblade.common.wps.WpsCallbackSupport;
import org.springblade.common.wps.WpsFileMeta;
import org.springblade.common.wps.WpsFileProvider;
import org.springblade.common.wps.WpsProperties;
import org.springblade.common.wps.WpsUrlSigner;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.api.IWpsOdocFileClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * odoc WPS 文件 Provider（Feign 代理）协议层测试。
 *
 * <p>固化：odoc 域路由、字节流直连 workflow 的绝对 URL 形态（含签名 token）、
 * 元数据经 Feign 代理且按 {@code wps:odoc:{op}:{bizId}} 逐资源签发服务间 token、
 * 对 {@code R} 包装解包、rename 对不可变版本为空操作、permission 透出 user_id。</p>
 */
class OdocWpsProviderTest {

	private static final String SECRET = "odoc-shared-secret";

	private final WpsUrlSigner signer = new WpsUrlSigner(SECRET, 300);

	private OdocWpsProvider build(IWpsOdocFileClient client) {
		WpsProperties props = new WpsProperties();
		props.setPublicBase("https://www.zwymding.top");
		return new OdocWpsProvider(client, signer, props);
	}

	private IWpsOdocFileClient stub(WpsFileMeta meta, List<WpsFileMeta> versions) {
		IWpsOdocFileClient client = mock(IWpsOdocFileClient.class);
		when(client.meta(eq(7L), anyString())).thenReturn(R.data(meta));
		when(client.versions(eq(7L), anyString())).thenReturn(R.data(versions));
		when(client.save(eq(7L), anyString(), anyString(), any(byte[].class))).thenReturn(R.data(meta));
		return client;
	}

	private WpsFileMeta meta() {
		WpsFileMeta m = new WpsFileMeta();
		m.setId("odoc:7");
		m.setName("a.docx");
		m.setVersion(3);
		return m;
	}

	/** token 含 exp，跨秒会变；用验签而非字符串相等，避免 flaky */
	private String signed(String op, Object bizId) {
		return argThat(t -> signer.verify(WpsCallbackSupport.internalResource(op, bizId), t));
	}

	@Test
	void domain_isOdoc() {
		WpsFileProvider p = build(stub(meta(), List.of(meta())));
		assertThat(p.domain()).isEqualTo("odoc");
	}

	@Test
	void downloadUrl_pointsToWorkflowWithSignedToken() {
		OdocWpsProvider p = build(stub(meta(), List.of(meta())));

		String dl = p.downloadUrl("7");

		assertThat(dl).startsWith(
			"https://www.zwymding.top/blade-workflow/odoc/wps/content?docId=7&version=3&token=");
		assertThat(dl).contains("token=" + signer.sign("7:3"));
	}

	@Test
	void uploadUrl_carriesWireFileIdAndEmbeddedToken() {
		OdocWpsProvider p = build(stub(meta(), List.of(meta())));

		String up = p.uploadUrl("odoc:7", "正文.docx");

		assertThat(up).contains("/blade-workflow/odoc/wps/upload/odoc:7");
		assertThat(up).contains("name=" + URLEncoder.encode("正文.docx", StandardCharsets.UTF_8));
		// token 内嵌在 URL 中，供 workflow WfOdocWpsController.upload 验签
		assertThat(up).contains("token=" + signer.sign("wps:odoc:7"));
	}

	@Test
	void infoAndVersions_unwrapR_andSignInternalTokenPerResource() {
		IWpsOdocFileClient client = stub(meta(), List.of(meta()));
		OdocWpsProvider p = build(client);

		assertThat(p.info("7").getId()).isEqualTo("odoc:7");
		assertThat(p.versions("7")).hasSize(1);

		verify(client).meta(eq(7L), signed("meta", 7L));
		verify(client).versions(eq(7L), signed("versions", 7L));
	}

	@Test
	void saveNewVersion_unwrapR_andSignInternalToken() {
		IWpsOdocFileClient client = stub(meta(), List.of(meta()));
		OdocWpsProvider p = build(client);

		assertThat(p.saveNewVersion("7", "hello".getBytes(), "a.docx").getId()).isEqualTo("odoc:7");

		verify(client).save(eq(7L), eq("a.docx"), signed("save", 7L), any(byte[].class));
	}

	@Test
	void versions_nullData_fallsBackToEmptyList() {
		IWpsOdocFileClient client = mock(IWpsOdocFileClient.class);
		when(client.versions(eq(7L), anyString())).thenReturn(R.data(null));

		assertThat(build(client).versions("7")).isEmpty();
	}

	@Test
	void info_nullResponse_returnsNull() {
		IWpsOdocFileClient client = mock(IWpsOdocFileClient.class);
		when(client.meta(eq(7L), anyString())).thenReturn(null);
		OdocWpsProvider p = build(client);

		assertThat(p.info("7")).isNull();
		assertThat(p.downloadUrl("7")).isNull();
	}

	@Test
	void rename_isNoopForImmutableVersions() {
		IWpsOdocFileClient untouched = mock(IWpsOdocFileClient.class);
		OdocWpsProvider p = build(untouched);

		p.rename("7", "改名.docx");

		verifyNoInteractions(untouched);
	}

	@Test
	void permission_exposesUserId() {
		OdocWpsProvider p = build(stub(meta(), Collections.emptyList()));

		Map<String, Object> perm = p.permission("7", Map.of("userId", "9", "userName", "张三"));

		assertThat(perm.get("user_id")).isEqualTo("9");
		assertThat(perm.get("update")).isEqualTo(1);
		assertThat(perm.get("saveas")).isEqualTo(0);
	}
}
