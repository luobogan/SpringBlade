package org.springblade.common.wps;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WPS WebOffice v3 回调协议工具（纯 JDK，无 Servlet/Spring 依赖）。
 *
 * <p>响应遵循 WPS 协议：成功 {@code {"code":0,"data":{...},"message":""}}，
 * 失败 {@code {"code":xxx,"message":...}}，故不使用 BladeX 的 R 包装。</p>
 *
 * <p><b>安全</b>：P3.3 起入口对带 fileId 的端点做 {@link #tokenValid} 硬校验；
 * 无 fileId 的辅助端点（{@code /users}、{@code /object/**}）走 {@link #tokenShapeValid}
 * 的形状 + 时效校验。服务间调用（blade-doc → blade-workflow）另用
 * {@link #HEADER_INTERNAL_TOKEN} + {@link #internalResource(String, Object)} 按资源签发/验签。</p>
 */
public final class WpsCallbackSupport {

	public static final int CODE_FILE_INVALID = 40005;
	public static final int CODE_SERVER_ERROR = 50001;
	/** 回调 token 校验失败（WPS 协议非 0 即失败，此处用独立码便于联调定位） */
	public static final int CODE_AUTH_FAILED = 40101;

	public static final String HEADER_TOKEN = "X-WebOffice-Token";
	public static final String HEADER_USER_QUERY = "X-User-Query";

	/** 辅助端点（/users、/object/**）绑定 resource 时，WPS 中台回调携带的 fileId 头 */
	public static final String HEADER_FILE_ID = "X-WebOffice-File-Id";

	/** 服务间调用头（blade-doc → blade-workflow 的 odoc 元数据/兜底保存） */
	public static final String HEADER_INTERNAL_TOKEN = "X-Wps-Internal-Token";

	/**
	 * 服务间调用 token 的 resource，格式 {@code wps:odoc:{op}:{bizId}}。
	 * op 取 {@code meta / versions / version-meta / save}，两侧必须逐字一致。
	 */
	public static String internalResource(String op, Object bizId) {
		return "wps:odoc:" + op + ":" + bizId;
	}

	private WpsCallbackSupport() {
	}

	public static Map<String, Object> ok(Object data) {
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("code", 0);
		r.put("data", data == null ? new LinkedHashMap<String, Object>() : data);
		r.put("message", "");
		return r;
	}

	public static Map<String, Object> err(int code, String message) {
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("code", code);
		r.put("message", message);
		return r;
	}

	/** 回调 token 校验：resource 统一为 {@code wps:{wireFileId}} */
	public static boolean tokenValid(WpsUrlSigner signer, String wireFileId, String token) {
		return signer != null && signer.verify("wps:" + wireFileId, token);
	}

	/**
	 * 辅助端点（{@code /users}、{@code /object/**}）的 token 形状 + 时效校验。
	 *
	 * <p>这些端点路径里没有 fileId，无法套用 {@code wps:{wireFileId}} 的 resource 绑定，
	 * 故只校验 token 是否为「{@code exp.64位hex}」且 exp 未过期——挡住无 token 的裸奔调用
	 * 与随手伪造的短字符串，但不宣称等价于 HMAC 验签。resource 绑定口径待 WPS 中台联调
	 * （确认回调头是否带 file_id）后收紧，见方案文档 P3 残留风险。</p>
	 */
	public static boolean tokenShapeValid(String token) {
		if (token == null || token.isBlank()) {
			return false;
		}
		int dot = token.indexOf('.');
		if (dot <= 0 || token.length() - dot - 1 != 64) {
			return false;
		}
		try {
			long exp = Long.parseLong(token.substring(0, dot));
			if (System.currentTimeMillis() / 1000 > exp) {
				return false;
			}
			for (int i = dot + 1; i < token.length(); i++) {
				if (Character.digit(token.charAt(i), 16) < 0) {
					return false;
				}
			}
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/** 解析 WPS 回调头 X-User-Query（URL query + 前端 init 的 customArgs 平铺） */
	public static Map<String, String> userQuery(String raw) {
		Map<String, String> m = new LinkedHashMap<>();
		if (raw == null || raw.isBlank()) {
			return m;
		}
		for (String kv : raw.split("&")) {
			int i = kv.indexOf('=');
			if (i <= 0) {
				continue;
			}
			String k = kv.substring(0, i);
			String v = URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8);
			m.put(k, v);
		}
		return m;
	}

	/**
	 * 公网 base：配置优先，缺省按请求推断（去掉尾斜杠）。
	 * WPS 中台按控制台登记的回调地址调用，download/upload 返回的绝对 URL 必须外网可达。
	 */
	public static String publicBase(String configured, String scheme, String host, int port) {
		if (configured != null && !configured.isBlank()) {
			return configured.trim().replaceAll("/+$", "");
		}
		boolean def = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
		return scheme + "://" + host + (def ? "" : ":" + port);
	}

	/** 把 WpsFileMeta 转为 WPS 协议 version 8 字段 */
	public static Map<String, Object> versionItem(WpsFileMeta meta) {
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("id", meta.getId());
		r.put("name", meta.getName());
		r.put("version", meta.getVersion());
		r.put("size", meta.getSize());
		r.put("create_time", meta.getCreateTime());
		r.put("modify_time", meta.getModifyTime());
		r.put("creator_id", meta.getCreatorId());
		r.put("modifier_id", meta.getModifierId());
		return r;
	}
}
