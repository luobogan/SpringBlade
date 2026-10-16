package org.springblade.workflow.odoc.layout.support;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.util.ArrayList;
import java.util.List;

/**
 * 套红 docx → 段落模型解析（PDF / OFD 共用）。
 *
 * <p>抽取每段文本，以及代表性样式（加粗、红字、字号、对齐），并标记空行与首段。红字判定依据
 * 运行色（R 高、G/B 低），用于识别红头；字号取自首个非空 run，缺失时回落 16pt（三号）。</p>
 */
public final class OdocDocxParser {

	private OdocDocxParser() {
	}

	public static List<OdocPara> parse(XWPFDocument doc) {
		List<OdocPara> list = new ArrayList<>();
		List<XWPFParagraph> ps = doc.getParagraphs();
		for (int i = 0; i < ps.size(); i++) {
			XWPFParagraph p = ps.get(i);
			String text = p.getText();
			if (text == null) {
				text = "";
			}
			text = text.trim();
			if (text.isEmpty()) {
				list.add(new OdocPara("", false, false, 16f, OdocPara.HAlign.LEFT, true, i == 0));
				continue;
			}
			boolean bold = false;
			boolean red = false;
			float size = 0f;
			OdocPara.HAlign align = alignOf(p);
			for (XWPFRun r : p.getRuns()) {
				if (r.getText(0) == null || r.getText(0).isEmpty()) {
					continue;
				}
				if (r.isBold()) {
					bold = true;
				}
				if (isRed(r.getColor())) {
					red = true;
				}
				int fs = r.getFontSize();
				if (fs > 0) {
					size = fs;
				}
			}
			if (size <= 0) {
				size = 16f;
			}
			list.add(new OdocPara(text, bold, red, size, align, false, i == 0));
		}
		return list;
	}

	public static OdocPara.HAlign alignOf(XWPFParagraph p) {
		ParagraphAlignment a = p.getAlignment();
		if (a == null) {
			return OdocPara.HAlign.LEFT;
		}
		switch (a) {
			case CENTER:
				return OdocPara.HAlign.CENTER;
			case RIGHT:
				return OdocPara.HAlign.RIGHT;
			case BOTH:
			case DISTRIBUTE:
				return OdocPara.HAlign.JUSTIFIED;
			default:
				return OdocPara.HAlign.LEFT;
		}
	}

	/** 运行色是否偏红（R>=180 且 G/B<=120），用于识别红头红字。 */
	public static boolean isRed(String c) {
		if (c == null || c.length() != 6) {
			return false;
		}
		try {
			int r = Integer.parseInt(c.substring(0, 2), 16);
			int g = Integer.parseInt(c.substring(2, 4), 16);
			int b = Integer.parseInt(c.substring(4, 6), 16);
			return r >= 180 && g <= 120 && b <= 120;
		} catch (Exception e) {
			return false;
		}
	}
}
