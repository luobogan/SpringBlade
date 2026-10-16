package org.springblade.workflow.odoc.layout;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.odoc.config.OdocProperties;
import org.springblade.workflow.odoc.layout.support.OdocDocxParser;
import org.springblade.workflow.odoc.layout.support.OdocFontResolver;
import org.springblade.workflow.odoc.layout.support.OdocPara;
import org.springblade.workflow.spi.IOdocFormatConvertor;
import org.springblade.workflow.spi.OdocConvertResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * 内置 PDF 渲染器（L1 规则引擎落地，离线默认）。
 *
 * <p>不依赖任何 OFD/CA 厂商 SDK：用 Apache POI 读取「套红版 docx」的段落与样式（含红头红字），
 * 用 iText 按 <b>GB/T 9704-2012</b> 规则渲染为合规 PDF：A4 版心、标准页边距、红色发文机关标志、
 * 红色反线（版头/主体分隔）、仿宋正文（首行缩进 2 字、两端对齐）、黑体/楷体标题层级、
 * 页码（4 号宋体、奇数页居右偶数页居左、一字线包裹）。并抽取公文元数据（发文机关/文号/标题）
 * 注入 PDF Info 字典。解析与字体定位由 {@code odoc.layout.support} 共享工具提供，供
 * {@link BuiltinOfdConvertor} 复用。</p>
 *
 * <p>OFD(13) 由 {@link BuiltinOfdConvertor} 处理；CA 签章仍走 {@code IOdocSealService} 厂商 SPI。</p>
 */
@Slf4j
@Component
@Order(100)
public class BuiltinPdfConvertor implements IOdocFormatConvertor {

	private final OdocProperties props;
	private final Map<String, BaseFont> fontCache = new HashMap<>();

	private static final float MM2PT = 2.834645669f;
	private static final float MARGIN_TOP_MM = 37f;
	private static final float MARGIN_BOTTOM_MM = 35f;
	private static final float MARGIN_LEFT_MM = 28f;
	private static final float MARGIN_RIGHT_MM = 26f;
	private static final float SIZE_BODY = 16f;
	private static final float SIZE_TITLE = 22f;
	private static final float SIZE_HEADER = 20f;
	private static final float SIZE_PAGE = 14f;

	public BuiltinPdfConvertor(OdocProperties props) {
		this.props = props;
	}

	@Override
	public boolean available() {
		return true;
	}

	@Override
	public String name() {
		return "builtin";
	}

	@Override
	public boolean supports(String format) {
		return "12".equals(format);
	}

	@Override
	public OdocConvertResult toLayout(byte[] docxBytes, String format, String fileName) {
		if (!"12".equals(format)) {
			return OdocConvertResult.fail(
				"内置 PDF 渲染器仅处理 PDF(12)；OFD(13) 由内置 OFD 写出器处理");
		}
		try {
			byte[] pdf = render(docxBytes);
			return OdocConvertResult.ok(pdf, format, withPdfExt(fileName), "builtin");
		} catch (Exception e) {
			log.warn("内置 PDF 渲染失败: {}", e.getMessage(), e);
			return OdocConvertResult.fail("内置 PDF 渲染失败: " + e.getMessage());
		}
	}

	// ===================== 渲染主流程 =====================

	private byte[] render(byte[] docxBytes) throws Exception {
		try (org.apache.poi.xwpf.usermodel.XWPFDocument xdoc =
				 new org.apache.poi.xwpf.usermodel.XWPFDocument(new ByteArrayInputStream(docxBytes))) {
			List<OdocPara> paras = OdocDocxParser.parse(xdoc);

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			com.itextpdf.text.Document doc = new com.itextpdf.text.Document(PageSize.A4,
				MARGIN_LEFT_MM * MM2PT, MARGIN_RIGHT_MM * MM2PT,
				MARGIN_TOP_MM * MM2PT, MARGIN_BOTTOM_MM * MM2PT);
			PdfWriter writer = PdfWriter.getInstance(doc, os);
			PageNumberEvent pageEvent = new PageNumberEvent(this);
			writer.setPageEvent(pageEvent);
			doc.open();

			StringBuilder title = new StringBuilder();
			StringBuilder author = new StringBuilder();
			StringBuilder docNo = new StringBuilder();

			boolean inHeader = true;
			boolean headerHadMark = false;
			boolean titleEmitted = false;
			for (OdocPara p : paras) {
				if (p.empty) {
					doc.add(new Paragraph(" "));
					continue;
				}
				if (inHeader && (p.red || p.isDocNumber())) {
					headerHadMark = true;
					if (p.red) {
						if (author.length() == 0) {
							author.append(p.text);
						}
						Font hf = songFont(p.first ? SIZE_HEADER : SIZE_BODY, Font.BOLD, BaseColor.RED);
						Paragraph ph = new Paragraph(p.text, hf);
						ph.setAlignment(Element.ALIGN_CENTER);
						ph.setSpacingAfter(4f);
						doc.add(ph);
					} else {
						Font nf = fangFont(SIZE_BODY);
						Paragraph np = new Paragraph(p.text, nf);
						np.setAlignment(Element.ALIGN_CENTER);
						np.setSpacingAfter(4f);
						doc.add(np);
					}
					continue;
				}
				if (inHeader) {
					inHeader = false;
					if (headerHadMark) {
						doc.add(new LineSeparator(1.5f, 100f, BaseColor.RED, Element.ALIGN_CENTER, 8f));
						doc.add(new Paragraph(" "));
					}
				}
				if (docNo.length() == 0) {
					Matcher m = OdocPara.DOC_NUMBER.matcher(p.text);
					if (m.find()) {
						docNo.append(m.group());
					}
				}
				boolean isTitle = !titleEmitted && !p.endsWithColon() && !p.isHeading()
					&& !p.isDocNumber() && p.text.length() <= 40 && !endsWithPunct(p.text);
				if (isTitle) {
					titleEmitted = true;
					if (title.length() == 0) {
						title.append(p.text);
					}
					Font tf = songFont(SIZE_TITLE, Font.BOLD, BaseColor.BLACK);
					Paragraph tp = new Paragraph(p.text, tf);
					tp.setAlignment(Element.ALIGN_CENTER);
					tp.setSpacingBefore(6f);
					tp.setSpacingAfter(10f);
					doc.add(tp);
					continue;
				}
				doc.add(renderBody(p));
			}

			if (title.length() > 0) {
				doc.addTitle(title.toString());
			}
			if (author.length() > 0) {
				doc.addAuthor(author.toString());
			}
			doc.addSubject("电子公文（GB/T 9704-2012）");
			if (docNo.length() > 0) {
				doc.addKeywords(docNo.toString());
			}
			doc.close();
			return os.toByteArray();
		}
	}

	private Paragraph renderBody(OdocPara p) {
		Font font;
		Paragraph para;
		if (p.isHeading()) {
			if (OdocPara.H2.matcher(p.text).find() || OdocPara.H3.matcher(p.text).find()) {
				font = kaiFont(SIZE_BODY);
			} else {
				font = heiFont(SIZE_BODY);
			}
			para = new Paragraph(p.text, font);
			para.setAlignment(Element.ALIGN_LEFT);
			para.setSpacingBefore(4f);
		} else if (p.endsWithColon() && p.text.length() <= 40) {
			font = fangFont(SIZE_BODY);
			para = new Paragraph(p.text, font);
			para.setAlignment(Element.ALIGN_LEFT);
			para.setSpacingBefore(2f);
		} else {
			font = fangFont(SIZE_BODY);
			para = new Paragraph(p.text, font);
			int a = p.align == OdocPara.HAlign.CENTER ? Element.ALIGN_CENTER
				: p.align == OdocPara.HAlign.RIGHT ? Element.ALIGN_RIGHT
				: p.align == OdocPara.HAlign.JUSTIFIED ? Element.ALIGN_JUSTIFIED
				: Element.ALIGN_LEFT;
			para.setAlignment(a);
			para.setFirstLineIndent(SIZE_BODY * 2f);
			para.setSpacingAfter(2f);
		}
		return para;
	}

	private boolean endsWithPunct(String s) {
		if (s == null || s.isEmpty()) {
			return false;
		}
		char last = s.charAt(s.length() - 1);
		return last == '。' || last == '，' || last == '；' || last == '：' || last == '、'
			|| last == '”' || last == '）' || last == '.' || last == ',';
	}

	// ===================== 字体（CJK TTF 嵌入） =====================

	private BaseFont baseFont(String file, int index) {
		String cacheKey = file + "#" + index;
		BaseFont cached = fontCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}
		try {
			String path = OdocFontResolver.resolveFontFile(props, file);
			if (path == null) {
				throw new IllegalStateException("字体文件未找到: " + file
					+ "，请配置 odoc.pdf-font-path 指向含 CJK 字体的目录");
			}
			String full = file.toLowerCase().endsWith(".ttc") ? path + "," + index : path;
			BaseFont bf = BaseFont.createFont(full, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
			fontCache.put(cacheKey, bf);
			return bf;
		} catch (Exception e) {
			throw new IllegalStateException("加载字体失败: " + file, e);
		}
	}

	private Font songFont(float size, int style, BaseColor color) {
		return new Font(baseFont("simsun.ttc", 0), size, style, color);
	}

	private Font fangFont(float size) {
		try {
			return new Font(baseFont("simfang.ttf", 0), size, Font.NORMAL, BaseColor.BLACK);
		} catch (Exception e) {
			return songFont(size, Font.NORMAL, BaseColor.BLACK);
		}
	}

	private Font heiFont(float size) {
		try {
			return new Font(baseFont("simhei.ttf", 0), size, Font.NORMAL, BaseColor.BLACK);
		} catch (Exception e) {
			return songFont(size, Font.BOLD, BaseColor.BLACK);
		}
	}

	private Font kaiFont(float size) {
		try {
			return new Font(baseFont("simkai.ttf", 0), size, Font.NORMAL, BaseColor.BLACK);
		} catch (Exception e) {
			return songFont(size, Font.NORMAL, BaseColor.BLACK);
		}
	}

	private String withPdfExt(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			return "odoc.pdf";
		}
		int i = fileName.lastIndexOf('.');
		return (i > 0 ? fileName.substring(0, i) : fileName) + ".pdf";
	}

	// ===================== 页码事件 =====================

	private static final class PageNumberEvent extends PdfPageEventHelper {
		private final BuiltinPdfConvertor owner;

		PageNumberEvent(BuiltinPdfConvertor owner) {
			this.owner = owner;
		}

		@Override
		public void onEndPage(PdfWriter writer, Document document) {
			int n = writer.getPageNumber();
			String text = "— " + n + " —";
			PdfContentByte cb = writer.getDirectContent();
			cb.saveState();
			Font f = owner.songFont(SIZE_PAGE, Font.NORMAL, BaseColor.BLACK);
			float y = document.bottom() - 16f;
			int align = (n % 2 == 1) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT;
			float x = (n % 2 == 1) ? document.right() : document.left();
			ColumnText.showTextAligned(cb, align, new Phrase(text, f), x, y, 0);
			cb.restoreState();
		}
	}
}
