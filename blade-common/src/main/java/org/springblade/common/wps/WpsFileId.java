package org.springblade.common.wps;

/**
 * WPS 中台 {@code fileId} 结构化编解码。
 *
 * <p>知识管理与收发文的业务主键都是雪花 ID，若共用一个 WPS 应用与回调入口，
 * 纯数字 fileId 会撞车。因此引入域前缀区分两侧，并按「下发形态」分两种编解码：</p>
 *
 * <p><b>内部形态</b>：{@code {domain}:{bizId}}（如 {@code odoc:789}），仅用于本系统内部
 * 标识与历史兼容解析；</p>
 *
 * <p><b>下发形态（wire）</b>：发给 WPS 中台的 {@code file_id}。<b>WPS 中台要求
 * {@code file_id} 只能由数字、字母、下划线组成，且不能以下划线开头</b>
 * （2026-11-12 实测：含冒号会返回 403
 * {@code 字段file_id, 只能由数字、字母、下划线组成, 且不能以下划线开头}），
 * 故公文域下发 {@code odoc_{bizId}}（下划线），知识管理域沿用<b>纯数字 legacy</b>
 * （不改变已下发给 WPS 中台的形态，存量会话零影响）。</p>
 *
 * <p><b>兼容存量</b>：无域前缀的纯数字按 {@value #DOMAIN_DOC} 解析（知识管理
 * 既有 WPS 回调一直是纯数字）；{@code odoc:{bizId}} 旧冒号格式亦可解析，
 * 迁移期两种格式并行不冲突。</p>
 */
public final class WpsFileId {

	public static final String DOMAIN_DOC = "doc";
	public static final String DOMAIN_ODOC = "odoc";

	/** 内部形态分隔符（冒号），仅用于历史兼容解析，不再下发给 WPS 中台 */
	private static final char SEP = ':';

	/**
	 * 下发形态分隔符（下划线）。WPS 中台的 {@code file_id} 只接受数字/字母/下划线，
	 * 冒号非法；下划线合法且不能位于首位（域前缀在首位，故安全）。
	 */
	public static final char WIRE_SEP = '_';

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

	/**
	 * 下发给 WPS 中台的 fileId（wire 形态）。
	 *
	 * <p>公文域用下划线（{@code odoc_{bizId}}）以满足中台字符集约束；
	 * 知识管理域沿用内部形态（实际下发走纯数字 docId，见
	 * {@code WpsDocOnlineEditor#wc.setFileId(String.valueOf(docId))}）。</p>
	 *
	 * @param domain 业务域（{@link #DOMAIN_DOC} / {@link #DOMAIN_ODOC}）
	 * @param bizId  业务主键（雪花 ID）
	 */
	public static String wireValue(String domain, String bizId) {
		if (DOMAIN_ODOC.equals(domain)) {
			return DOMAIN_ODOC + WIRE_SEP + bizId;
		}
		return of(domain, bizId);
	}

	/** 解析，非法返回 null（供回调入口统一返回 40005 file_id invalid） */
	public static WpsFileId parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		String v = raw.trim();
		// ① 下发形态：odoc_{digits}（WPS 中台合法 file_id，当前公文域唯一产出形态）
		int u = v.indexOf(WIRE_SEP);
		if (u > 0 && u < v.length() - 1) {
			String d = v.substring(0, u);
			String b = v.substring(u + 1);
			if (DOMAIN_ODOC.equals(d)) {
				return isDigits(b) ? new WpsFileId(d, b, false) : null;
			}
			return null;
		}
		// ② 旧内部形态：domain:bizId（迁移期兼容）
		int i = v.indexOf(SEP);
		if (i > 0 && i < v.length() - 1) {
			String d = v.substring(0, i);
			if (DOMAIN_DOC.equals(d) || DOMAIN_ODOC.equals(d)) {
				return new WpsFileId(d, v.substring(i + 1), false);
			}
			return null;
		}
		// ③ 存量纯数字 fileId（知识管理 legacy）
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

	/**
	 * 回传 WPS 中台的规范 fileId。
	 *
	 * <p>legacy（纯数字）保持原样，避免改变既有会话标识；
	 * 公文域返回下划线形态（{@code odoc_{bizId}}），满足中台字符集约束；
	 * 知识管理域保持内部形态不变。</p>
	 */
	public String wireValue() {
		if (legacy) {
			return bizId;
		}
		return wireValue(domain, bizId);
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
