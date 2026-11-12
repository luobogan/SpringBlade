package org.springblade.common.wps;

/**
 * WPS 中台 {@code fileId} 结构化编解码。
 *
 * <p>知识管理与收发文的业务主键都是雪花 ID，若共用一个 WPS 应用与回调入口，
 * 纯数字 fileId 会撞车。统一格式为 {@code {domain}:{bizId}}，例如
 * {@code doc:123456}、{@code odoc:789}。</p>
 *
 * <p><b>兼容存量</b>：无域前缀的纯数字按 {@value #DOMAIN_DOC} 解析（知识管理
 * 既有 WPS 回调一直是纯数字），因此迁移期无需清洗历史链接。</p>
 */
public final class WpsFileId {

	public static final String DOMAIN_DOC = "doc";
	public static final String DOMAIN_ODOC = "odoc";

	private static final char SEP = ':';

	private final String domain;
	private final String bizId;
	private final boolean legacy;

	private WpsFileId(String domain, String bizId, boolean legacy) {
		this.domain = domain;
		this.bizId = bizId;
		this.legacy = legacy;
	}

	public static String of(String domain, String bizId) {
		return domain + SEP + bizId;
	}

	/** 解析，非法返回 null（供回调入口统一返回 40005 file_id invalid） */
	public static WpsFileId parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		String v = raw.trim();
		int i = v.indexOf(SEP);
		if (i > 0 && i < v.length() - 1) {
			String d = v.substring(0, i);
			if (DOMAIN_DOC.equals(d) || DOMAIN_ODOC.equals(d)) {
				return new WpsFileId(d, v.substring(i + 1), false);
			}
			return null;
		}
		if (isDigits(v)) {
			return new WpsFileId(DOMAIN_DOC, v, true);
		}
		return null;
	}

	/** 按指定域解析：域不匹配返回 null，防止跨域越权访问他人文件 */
	public static WpsFileId parseFor(String raw, String expectedDomain) {
		WpsFileId id = parse(raw);
		return (id != null && id.domain.equals(expectedDomain)) ? id : null;
	}

	public String domain() {
		return domain;
	}

	public String bizId() {
		return bizId;
	}

	/** 是否为存量纯数字 fileId */
	public boolean legacy() {
		return legacy;
	}

	/** 回传 WPS 中台的规范 fileId（legacy 保持原样，避免改变既有会话标识） */
	public String wireValue() {
		return legacy ? bizId : of(domain, bizId);
	}

	private static boolean isDigits(String s) {
		if (s.isEmpty()) {
			return false;
		}
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) < '0' || s.charAt(i) > '9') {
				return false;
			}
		}
		return true;
	}
}
