package org.springblade.doc.online;

import lombok.extern.slf4j.Slf4j;
import org.springblade.common.wps.WpsCallbackSupport;
import org.springblade.common.wps.WpsFileId;
import org.springblade.common.wps.WpsFileMeta;
import org.springblade.common.wps.WpsFileProvider;
import org.springblade.common.wps.WpsProperties;
import org.springblade.common.wps.WpsSettings;
import org.springblade.common.wps.WpsSettingsResolver;
import org.springblade.common.wps.WpsUrlSigner;
import org.springblade.core.tool.api.R;
import org.springblade.doc.config.WpsSettingStore;
import org.springblade.workflow.api.IWpsOdocFileClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 收发文（odoc）WPS 文件存储 SPI 实现 —— 经 Feign 代理到 blade-workflow。
 *
 * <p>WPS 统一改造 P2-2：与知识管理 {@link DocWpsProvider} 共用 blade-doc 单回调入口，
 * 按 {@link WpsFileId#DOMAIN_ODOC} 路由。本实现不访问任何业务表——元数据经
 * {@link IWpsOdocFileClient} 代理到 workflow，字节流（下载/上传）直接指向
 * workflow 的 {@code /odoc/wps/**} 端点，绝不经 blade-doc 回调入口中转。</p>
 *
 * <p>签名：与 workflow 侧的 {@code OdocUrlSigner} 共用一把共享密钥
 * （{@code blade.wps.url-sign-secret}），否则 odoc 回调 token / content 签名在
 * blade-doc 入口验签会失败。服务间 Feign 调用另按
 * {@link WpsCallbackSupport#internalResource(String, Object)} 逐资源签发 token。</p>
 */
@Slf4j
@Component
public class OdocWpsProvider implements WpsFileProvider {

	private final IWpsOdocFileClient client;
	private final WpsUrlSigner signer;
	private final Supplier<WpsSettings> settings;

	@Autowired
	public OdocWpsProvider(IWpsOdocFileClient client, WpsUrlSigner signer, WpsSettingStore store) {
		this(client, signer, store::current);
	}

	/** 兼容构造器：协议层单测脱离 Spring 容器时注入固定兜底配置 */
	public OdocWpsProvider(IWpsOdocFileClient client, WpsUrlSigner signer, WpsProperties fallback) {
		this(client, signer, () -> WpsSettingsResolver.resolve(key -> null, fallback));
	}

	private OdocWpsProvider(IWpsOdocFileClient client, WpsUrlSigner signer, Supplier<WpsSettings> settings) {
		this.client = client;
		this.signer = signer;
		this.settings = settings;
	}

	@Override
	public String domain() {
		return WpsFileId.DOMAIN_ODOC;
	}

	@Override
	public String publicBase() {
		return settings.get().getPublicBase();
	}

	/** workflow 可达 base（WPS 中台视角）：public-base + 网关路由前缀 /blade-workflow */
	private String base() {
		String b = publicBase();
		if (b == null || b.isBlank()) {
			return "/blade-workflow";
		}
		return b.replaceAll("/+$", "") + "/blade-workflow";
	}

	/** 服务间调用 token：resource 必须与 workflow 侧逐字一致 */
	private String internalToken(String op, Object bizId) {
		return signer.sign(WpsCallbackSupport.internalResource(op, bizId));
	}

	private String contentUrl(String bizId, int version) {
		return base() + "/odoc/wps/content?docId=" + bizId
			+ "&version=" + version + "&token=" + signer.sign(bizId + ":" + version);
	}

	@Override
	public WpsFileMeta info(String bizId) {
		Long id = Long.valueOf(bizId);
		R<WpsFileMeta> r = client.meta(id, internalToken("meta", id));
		return r == null ? null : r.getData();
	}

	@Override
	public String downloadUrl(String bizId) {
		WpsFileMeta meta = info(bizId);
		if (meta == null) {
			return null;
		}
		return contentUrl(bizId, meta.getVersion());
	}

	@Override
	public String uploadUrl(String wireFileId, String name) {
		String url = base() + "/odoc/wps/upload/" + wireFileId;
		if (name != null && !name.isBlank()) {
			url += "?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
		}
		// 嵌入 PUT 端点验签所需的 token（workflow WfOdocWpsController.upload 用同样口径校验）
		url += (name != null && !name.isBlank() ? "&" : "?") + "token=" + signer.sign("wps:" + wireFileId);
		return url;
	}

	@Override
	public WpsFileMeta saveNewVersion(String bizId, byte[] content, String name) {
		// 正常 WPS 三阶段保存走 workflow 直连 PUT，不经此处；此处仅作 Feign 防御性兜底
		Long id = Long.valueOf(bizId);
		R<WpsFileMeta> r = client.save(id, name, internalToken("save", id), content);
		return r == null ? null : r.getData();
	}

	@Override
	public List<WpsFileMeta> versions(String bizId) {
		Long id = Long.valueOf(bizId);
		R<List<WpsFileMeta>> r = client.versions(id, internalToken("versions", id));
		List<WpsFileMeta> list = r == null ? null : r.getData();
		return list == null ? Collections.emptyList() : list;
	}

	@Override
	public String versionDownloadUrl(String bizId, int version) {
		return contentUrl(bizId, version);
	}

	@Override
	public void rename(String bizId, String name) {
		// odoc 版本记录不可变，重命名不改历史；WPS 文档名称以当前版本为准，前端另管，此处留空
		log.debug("odoc WPS rename 忽略（版本不可变）docId={} name={}", bizId, name);
	}

	@Override
	public Map<String, Object> permission(String bizId, Map<String, String> userQuery) {
		Map<String, Object> r = new java.util.LinkedHashMap<>();
		r.put("read", 1);
		r.put("update", 1);
		r.put("download", 1);
		r.put("rename", 1);
		r.put("history", 1);
		r.put("copy", 1);
		r.put("print", 1);
		r.put("saveas", 0);
		r.put("comment", 1);
		r.put("user_id", userQuery.getOrDefault("userId", "0"));
		return r;
	}
}
