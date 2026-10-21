package org.springblade.formmode.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 泛微 E9 原生字段类型 ↔ 本项目自有编码 · 双向映射（单一来源）
 *
 * <p>为什么需要：本项目自有编码（1–9 及预留 10）与 E9 原生 {@code fieldhtmltype}
 * （全集 1–9）**不是同一套**，且存在语义错位：</p>
 * <ul>
 *   <li>E9 {@code 8=公共选择框(radio)}、{@code 9=位置}；自有 8/9 却是「布局组件 / 正文」；</li>
 *   <li>E9 日期/时间用「浏览框 3 + fieldType 2/19」，本项目用「3 + 98/99」；</li>
 *   <li>自有「布局(8)/正文(9)」在 E9 无直接对应。</li>
 * </ul>
 * <p>因此**严禁按数字直接等同**，一切互转必须走本类。</p>
 *
 * <p>⚠️ 重要：浏览按钮子类型的映射以<b>本部署实测语义</b>为准
 * （对齐 {@code ecology.EcologyFormImportServiceImpl#mapBrowserType}），
 * 与 E9 标准字典存在差异（字典 4=文档，实测 4=部门；9/37=文档；17=多人力资源而非多部门；
 * 164=分部而非「自定义浏览按钮」）。禁止照搬标准字典。</p>
 *
 * <p>说明：本类为**新增公共服务**，供后续通用字段类型服务、导出/对接使用；
 * 既有的 {@code EcologyFormImportServiceImpl} 私有映射保持不动（避免影响既有导入业务），
 * 后续迭代可将其收敛到本类。</p>
 */
public final class E9FieldTypeMapper {

	private E9FieldTypeMapper() {
	}

	// ==================== 本项目自有编码（对齐前端 fieldTypes.ts / constant.FieldTypeMeta） ====================

	public static final int HTML_TEXT = 1;      // 文本字段
	public static final int HTML_MULTILINE = 2; // 多行文本
	public static final int HTML_BROWSER = 3;   // 浏览按钮
	public static final int HTML_SELECT = 4;    // 选择框（1下拉 2单选 3复选）
	public static final int HTML_AFFIX = 5;     // 附件上传
	public static final int HTML_CHECKBOX = 6;  // 复选框
	public static final int HTML_SPECIAL = 7;   // 特殊字段（1链接 2描述 3日期 4时间）
	public static final int HTML_LAYOUT = 8;    // 布局组件（E9 无对应）
	public static final int HTML_ODOC = 9;      // 正文（E9 无对应）
	/** 预留：对应 E9「位置(9)」，当前仅有映射关系，渲染控件待补齐 */
	public static final int HTML_POSITION = 10;

	// ==================== E9 原生 fieldhtmltype（全集 1–9） ====================
	// 注：E9 的 FieldValue.java 仅对 3/4/5/6 做显示值转换，1/2/0/7/8/9 走 else 原样返回。

	public static final int E9_TEXT = 1;           // 单行文本
	public static final int E9_TEXTAREA = 2;       // 多行文本
	public static final int E9_BROWSER = 3;        // 浏览框
	public static final int E9_CHECKBOX = 4;       // 复选框
	public static final int E9_SELECT = 5;         // 选择框 select
	public static final int E9_AFFIX = 6;          // 附件
	public static final int E9_ESPECIAL = 7;       // 特殊字段
	public static final int E9_PUBLIC_SELECT = 8;  // 公共选择框 radio
	public static final int E9_POSITION = 9;       // 位置

	// ==================== 浏览按钮类型（自有侧常用值） ====================

	public static final int BROWSER_HRM = 1;
	public static final int BROWSER_MULTI_HRM = 161;
	public static final int BROWSER_DEPT = 2;
	public static final int BROWSER_SUBCOMPANY = 18;
	public static final int BROWSER_DOC = 24;
	public static final int BROWSER_DATE = 98;
	public static final int BROWSER_TIME = 99;

	/** E9 浏览按钮 TYPE → 自有浏览按钮类型（实测映射表） */
	private static final Map<Integer, Integer> E9_BROWSER_MAP = new HashMap<>();

	static {
		E9_BROWSER_MAP.put(1, BROWSER_HRM);        // 人员 → 人力资源
		E9_BROWSER_MAP.put(2, BROWSER_DEPT);       // 组织 → 部门（非日期场景）
		E9_BROWSER_MAP.put(3, 3);                  // 角色
		E9_BROWSER_MAP.put(4, BROWSER_DEPT);       // 本库经办/报销部门均用 4
		E9_BROWSER_MAP.put(7, 8);                  // 项目
		E9_BROWSER_MAP.put(8, 7);                  // 资产
		E9_BROWSER_MAP.put(9, BROWSER_DOC);        // 文档（相关文档/正文/合同文件）
		E9_BROWSER_MAP.put(12, BROWSER_HRM);       // 集成 → 兜底人力资源
		E9_BROWSER_MAP.put(16, 30);                // 相关客户/流程 → 流程
		E9_BROWSER_MAP.put(17, BROWSER_MULTI_HRM); // 本部署 17 = 多人力资源（非多部门）
		E9_BROWSER_MAP.put(18, BROWSER_SUBCOMPANY);// 分部
		E9_BROWSER_MAP.put(19, 19);                // 分权单部门（日期型 19 走时间分支）
		E9_BROWSER_MAP.put(20, 20);
		E9_BROWSER_MAP.put(21, 21);
		E9_BROWSER_MAP.put(22, 22);
		E9_BROWSER_MAP.put(23, 23);
		E9_BROWSER_MAP.put(37, BROWSER_DOC);       // 文档（自定义：合同文档/PDF 正文）
		E9_BROWSER_MAP.put(164, BROWSER_SUBCOMPANY);// 本部署 164 = 分部（非自定义浏览按钮）
	}

	// ==================== 映射结果（自有侧） ====================

	public static class SelfType {
		/** 自有 fieldHtmlType */
		public final int htmlType;
		/** 自有 fieldType（子类型） */
		public final Integer type;
		/** 浏览按钮类型（htmlType=3 时有效） */
		public final Integer browserType;
		/** 存在语义降级时为 true，详见 note（绝不静默丢信息） */
		public final boolean lossy;
		/** 降级/歧义说明 */
		public final String note;

		SelfType(int htmlType, Integer type, Integer browserType, boolean lossy, String note) {
			this.htmlType = htmlType;
			this.type = type;
			this.browserType = browserType;
			this.lossy = lossy;
			this.note = note;
		}
	}

	// ==================== 映射结果（E9 侧） ====================

	public static class E9Type {
		/** E9 fieldhtmltype（1–9） */
		public final int e9HtmlType;
		/** E9 fieldType（子类型 / 浏览按钮类型） */
		public final Integer e9FieldType;
		public final boolean lossy;
		public final String note;

		E9Type(int e9HtmlType, Integer e9FieldType, boolean lossy, String note) {
			this.e9HtmlType = e9HtmlType;
			this.e9FieldType = e9FieldType;
			this.lossy = lossy;
			this.note = note;
		}
	}

	// ==================== E9 → 自有 ====================

	/**
	 * E9 原生编码 → 自有编码。
	 *
	 * @param e9HtmlType  E9 fieldhtmltype（1–9）
	 * @param e9FieldType E9 fieldType（子类型），可为 null
	 * @param dbType      E9 侧数据库类型串（如 char(10)/datetime），用于区分「日期 vs 部门」（二者 TYPE 均为 2）
	 * @return 自有侧映射；无法映射返回 null
	 */
	public static SelfType fromE9(Integer e9HtmlType, Integer e9FieldType, String dbType) {
		int e9 = e9HtmlType == null ? 0 : e9HtmlType;
		int ft = e9FieldType == null ? 0 : e9FieldType;

		switch (e9) {
			case E9_TEXT:
				// E9：1文本 2整数 3浮点 4金额转换 5金额千分位
				return new SelfType(HTML_TEXT, ft == 2 ? 4 : ft == 3 ? 5 : ft == 4 ? 6 : ft == 5 ? 7 : 1,
					null, false, null);
			case E9_TEXTAREA:
				return new SelfType(HTML_MULTILINE, 1, null, false, null);
			case E9_BROWSER:
				return fromE9Browser(ft, dbType);
			case E9_CHECKBOX:
				return new SelfType(HTML_CHECKBOX, 1, null, false, null);
			case E9_SELECT:
				return new SelfType(HTML_SELECT, 1, null, false, null);
			case E9_AFFIX:
				return new SelfType(HTML_AFFIX, 1, null, false, null);
			case E9_ESPECIAL:
				return new SelfType(HTML_SPECIAL, ft == 1 ? 1 : 2, null, false, null);
			case E9_PUBLIC_SELECT:
				// 复用自有「选择框(4)+单选(2)」表达
				return new SelfType(HTML_SELECT, 2, null, true, "E9 公共选择框复用自有「选择框+单选」表达");
			case E9_POSITION:
				return new SelfType(HTML_POSITION, null, null, true, "E9 位置字段映射至预留类型 10，渲染控件待补齐");
			default:
				return null;
		}
	}

	private static SelfType fromE9Browser(int ft, String dbType) {
		// 1) 日期/时间优先按 dbType 判定（TYPE=2→日期；TYPE=3/19→时间）
		if (isDateLike(dbType)) {
			if (ft == 2) {
				return new SelfType(HTML_BROWSER, BROWSER_DATE, BROWSER_DATE, false, null);
			}
			if (ft == 3 || ft == 19) {
				return new SelfType(HTML_BROWSER, BROWSER_TIME, BROWSER_TIME, false, null);
			}
		}
		// 2) 未传 dbType：按 E9 标准语义兜底并标注歧义
		if (ft == 2 && dbType == null) {
			return new SelfType(HTML_BROWSER, BROWSER_DATE, BROWSER_DATE, true,
				"未传 dbType，TYPE=2 按「日期」处理；若实际为部门请传 dbType 消歧");
		}
		if (ft == 19 && dbType == null) {
			return new SelfType(HTML_BROWSER, BROWSER_TIME, BROWSER_TIME, true,
				"未传 dbType，TYPE=19 按「时间」处理；若实际为分权单部门请传 dbType 消歧");
		}
		Integer mapped = E9_BROWSER_MAP.get(ft);
		int browserType = mapped != null ? mapped : (ft > 0 ? ft : BROWSER_HRM);
		return new SelfType(HTML_BROWSER, browserType, browserType, mapped == null && ft > 0,
			mapped == null && ft > 0 ? "未知浏览按钮类型 " + ft + "，按原值兜底" : null);
	}

	// ==================== 自有 → E9 ====================

	/**
	 * 自有编码 → E9 原生编码（用于导出 / 与泛微对接）。
	 *
	 * @param htmlType    自有 fieldHtmlType（1–10）
	 * @param type        自有 fieldType（子类型），可为 null
	 * @param browserType 浏览按钮类型（htmlType=3 时优先），可为 null
	 */
	public static E9Type toE9(Integer htmlType, Integer type, Integer browserType) {
		int ht = htmlType == null ? 0 : htmlType;
		int t = type == null ? 0 : type;
		int bt = browserType == null ? 0 : browserType;

		switch (ht) {
			case HTML_TEXT:
				if (t == 2) {
					return new E9Type(E9_TEXTAREA, 1, false, null);
				}
				if (t == 3) {
					return new E9Type(E9_TEXT, 1, true, "E9 无「保密字段」子类型，降级为单行文本");
				}
				// 4整数→2、5浮点→3、6金额转换→4、7金额千分位→5、默认→1
				return new E9Type(E9_TEXT, t == 4 ? 2 : t == 5 ? 3 : t == 6 ? 4 : t == 7 ? 5 : 1, false, null);
			case HTML_MULTILINE:
				return new E9Type(E9_TEXTAREA, 1, false, null);
			case HTML_BROWSER: {
				int e9Browser = bt > 0 ? bt : t;
				if (e9Browser == BROWSER_DATE) {
					return new E9Type(E9_BROWSER, 2, false, null);
				}
				if (e9Browser == BROWSER_TIME) {
					return new E9Type(E9_BROWSER, 19, false, null);
				}
				return new E9Type(E9_BROWSER, e9Browser > 0 ? e9Browser : 1, false, null);
			}
			case HTML_SELECT:
				if (t == 2) {
					return new E9Type(E9_PUBLIC_SELECT, 1, false, null);
				}
				if (t == 3) {
					return new E9Type(E9_CHECKBOX, null, true, "多选下拉在 E9 无直接对应，降级为复选框");
				}
				return new E9Type(E9_SELECT, 1, false, null);
			case HTML_AFFIX:
				return new E9Type(E9_AFFIX, t == 2 ? 2 : 1, false, null);
			case HTML_CHECKBOX:
				return new E9Type(E9_CHECKBOX, null, false, null);
			case HTML_SPECIAL:
				if (t == 3) {
					return new E9Type(E9_BROWSER, 2, false, null);
				}
				if (t == 4) {
					return new E9Type(E9_BROWSER, 19, false, null);
				}
				return new E9Type(E9_ESPECIAL, t == 1 ? 1 : 2, false, null);
			case HTML_POSITION:
				return new E9Type(E9_POSITION, null, false, null);
			default:
				// 8 布局组件、9 正文：E9 无直接对应
				return new E9Type(E9_TEXT, 1, true, "自有类型 " + ht + "（布局/正文）在 E9 无直接对应，降级为单行文本");
		}
	}

	// ==================== 工具 ====================

	/**
	 * 判断 E9 侧数据库类型是否像「日期/时间」。
	 *
	 * <p>必要性：E9 中「部门」与「日期」同为 TYPE=2，只能靠 dbType 区分，
	 * 否则会把部门误判成日期（对齐 {@code EcologyFormImportServiceImpl#isDateLike}）。</p>
	 */
	public static boolean isDateLike(String dbType) {
		if (dbType == null || dbType.trim().isEmpty()) {
			return false;
		}
		String r = dbType.toLowerCase().replaceAll("\\s+", "");
		if (r.contains("date") || r.contains("datetime") || r.contains("smalldatetime")) {
			return true;
		}
		Matcher m = Pattern.compile("char\\((\\d+)\\)").matcher(r);
		if (m.find()) {
			int len = Integer.parseInt(m.group(1));
			return len >= 8 && len <= 11;
		}
		return false;
	}
}
