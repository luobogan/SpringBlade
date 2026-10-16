package org.springblade.workflow.odoc.layout.support;

import java.util.regex.Pattern;

/**
 * 公文段落模型（PDF / OFD 渲染器共用）。
 *
 * <p>由 {@link OdocDocxParser} 从套红 docx 解析得到：文本、是否加粗、是否红字、字号(pt)、
 * 水平对齐、是否为空行、是否首段。含公文常见判定（文号 / 标题层级），供两种渲染器复用，
 * 避免 PDF 与 OFD 两套实现各写一遍解析与规则。</p>
 */
public class OdocPara {

	public enum HAlign {
		LEFT, CENTER, RIGHT, JUSTIFIED
	}

	/** 发文字号：〔2026〕12号 或 [2026]12号 */
	public static final Pattern DOC_NUMBER = Pattern.compile("[〔\\[]\\d{4}[〕\\]]\\s*\\d+\\s*号");
	/** 一级标题：一、二、 */
	public static final Pattern H1 = Pattern.compile("^[一二三四五六七八九十百零]+、");
	/** 二级标题：（一）（二） */
	public static final Pattern H2 = Pattern.compile("^（[一二三四五六七八九十]+）");
	/** 三级标题：(一)(二) */
	public static final Pattern H3 = Pattern.compile("^\\(\\s*[一二三四五六七八九十]+\\s*\\)");

	public final String text;
	public final boolean bold;
	public final boolean red;
	public final float sizePt;
	public final HAlign align;
	public final boolean empty;
	public final boolean first;

	public OdocPara(String text, boolean bold, boolean red, float sizePt, HAlign align,
				   boolean empty, boolean first) {
		this.text = text;
		this.bold = bold;
		this.red = red;
		this.sizePt = sizePt;
		this.align = align;
		this.empty = empty;
		this.first = first;
	}

	/** 是否发文字号行 */
	public boolean isDocNumber() {
		return !empty && DOC_NUMBER.matcher(text).find();
	}

	/** 是否以冒号结尾（主送机关特征） */
	public boolean endsWithColon() {
		if (empty) {
			return false;
		}
		char last = text.charAt(text.length() - 1);
		return last == '：' || last == ':';
	}

	/** 是否标题层级行（一、/（一）/ (一)） */
	public boolean isHeading() {
		return !empty && (H1.matcher(text).find() || H2.matcher(text).find() || H3.matcher(text).find());
	}
}
