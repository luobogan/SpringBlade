package org.springblade.workflow.odoc.layout;

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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 内置 OFD 写出器（L1 规则引擎落地，离线默认，对应格式 13）。
 *
 * <p>不依赖任何 OFD 厂商 SDK：用 Apache POI 读取「套红版 docx」，按 <b>GB/T 9704-2012</b> 布局，
 * 手写 <b>GB/T 33190-2016</b> OFD 包（ZIP 容器 + XML + 嵌入 CJK 字体）：A4 版心、标准页边距、
 * 红色发文机关标志、红色反线（版头/主体分隔）、仿宋正文（首行缩进 2 字）、黑体/楷体标题层级、
 * 页码（奇右偶左、一字线包裹），并把发文机关/文号/标题写入 OFD DocInfo（可检索元数据）。</p>
 *
 * <p>这是「三代套红方案」里 L1 规则引擎（确定性、无需 AI/厂商）的 OFD 落地；真正的厂商 OFD
 * （数科/永中/福昕）仍走 {@code IOdocFormatConvertor} SPI（{@code OfdVendorConvertor}）。
 * 注：本写出器为「最小合规」实现，字体以单个非 CID 嵌入 TrueType（Unicode 文本 + cmap 解析），
 * 需经标准 OFD 阅读器（数科/福昕/WPS）验证字形与排版；生产如需更严格的 CID/子集化可替换为厂商方案。</p>
 */
@Slf4j
@Component
@Order(101)
public class BuiltinOfdConvertor implements IOdocFormatConvertor {

	// OFD 坐标：1 单位 = 0.1mm。A4 = 2100 x 2970。
	private static final int PAGE_W = 2100;
	private static final int PAGE_H = 2970;
	private static final int M_LEFT = 280;
	private static final int M_RIGHT = 260;
	private static final int M_TOP = 370;
	private static final int M_BOTTOM = 350;
	private static final int CONTENT_LEFT = M_LEFT;
	private static final int CONTENT_RIGHT = PAGE_W - M_RIGHT;
	private static final int CONTENT_TOP = M_TOP;
	private static final int CONTENT_BOTTOM = PAGE_H - M_BOTTOM;
	private static final int CONTENT_WIDTH = CONTENT_RIGHT - CONTENT_LEFT;
	/** 1pt = 0.3528mm = 3.528 个 OFD 单位（0.1mm） */
	private static final double PT2UNIT = 3.528;
	/** OFD 文本 Size 以 0.1pt 为单位（16pt → 160） */
	private static final String OFD_NS = "http://www.ofdspec.org/2016";
	/** 嵌入字体候选（取首个存在者），优先单 TTF，避免 TTC 在部分阅读器不识别 */
	private static final String[] EMBED_FONT_CANDIDATES = {"simfang.ttf", "simhei.ttf", "simkai.ttf", "simsunb.ttf"};

	private final OdocProperties props;

	public BuiltinOfdConvertor(OdocProperties props) {
		this.props = props;
	}

	@Override
	public boolean available() {
		return true;
	}

	@Override
	public String name() {
		return "builtin-ofd";
	}

	@Override
	public boolean supports(String format) {
		return "13".equals(format);
	}

	@Override
	public OdocConvertResult toLayout(byte[] docxBytes, String format, String fileName) {
		if (!"13".equals(format)) {
			return OdocConvertResult.fail("内置 OFD 写出器仅处理 OFD(13)；PDF(12) 由内置 PDF 渲染器处理");
		}
		try {
			byte[] ofd = render(docxBytes);
			return OdocConvertResult.ok(ofd, format, withOfdExt(fileName), "builtin-ofd");
		} catch (Exception e) {
			log.warn("内置 OFD 写出失败: {}", e.getMessage(), e);
			return OdocConvertResult.fail("内置 OFD 写出失败: " + e.getMessage());
		}
	}

	// ===================== 渲染主流程 =====================

	private byte[] render(byte[] docxBytes) throws IOException {
		List<OdocPara> paras = OdocDocxParser.parse(
			new org.apache.poi.xwpf.usermodel.XWPFDocument(new ByteArrayInputStream(docxBytes)));

		List<List<String>> pages = new ArrayList<>();
		List<String> cur = new ArrayList<>();
		pages.add(cur);

		StringBuilder title = new StringBuilder();
		StringBuilder author = new StringBuilder();
		StringBuilder docNo = new StringBuilder();

		double y = CONTENT_TOP;
		boolean inHeader = true;
		boolean headerHadMark = false;
		boolean titleEmitted = false;

		for (OdocPara p : paras) {
			if (p.empty) {
				if (!inHeader) {
					y += 28;
					if (y > CONTENT_BOTTOM) {
						cur = new ArrayList<>();
						pages.add(cur);
						y = CONTENT_TOP;
					}
				}
				continue;
			}
			boolean docNumber = p.isDocNumber();
			boolean isTitle = !titleEmitted && !docNumber && !p.endsWithColon()
				&& !p.isHeading() && p.text.length() <= 40 && !endsWithPunct(p.text);

			// 版头：发红字的发文机关标志 + 标题 + 发文字号 同属版头；
			// 红色反线（版头/主体分隔线）画在版头结束处，即文号之后、正文之前（GB/T 9704-2012）。
			if (inHeader && (p.red || docNumber || isTitle)) {
				headerHadMark = true;
				if (p.red && author.length() == 0) {
					author.append(p.text);
				}
				if (docNumber && docNo.length() == 0) {
					java.util.regex.Matcher m = OdocPara.DOC_NUMBER.matcher(p.text);
					if (m.find()) {
						docNo.append(m.group());
					}
				}
				int pt = p.red ? (p.first ? 20 : 16) : (docNumber ? 16 : 22);
				double fofd = pt * PT2UNIT;
				double lineH = fofd * 1.5;
				List<String> lines = wrap(p.text, fofd, 0);
				for (String line : lines) {
					if (y + lineH > CONTENT_BOTTOM) {
						cur = new ArrayList<>();
						pages.add(cur);
						y = CONTENT_TOP;
					}
					double lineW = lineWidth(line, fofd);
					double x = CONTENT_LEFT + (CONTENT_WIDTH - lineW) / 2.0;
					addText(cur, x, y, Math.max(lineW, 1), fofd, pt, fofd * 0.8, line, p.red);
					y += lineH;
				}
				if (isTitle) {
					titleEmitted = true;
					if (title.length() == 0) {
						title.append(p.text);
					}
				}
				continue;
			}

			// 进入正文：结束版头，绘制红色反线（位于文号之后）
			if (inHeader) {
				inHeader = false;
				if (headerHadMark) {
					addRedLine(cur, y);
					y += 20;
				}
			}

			if (docNumber && docNo.length() == 0) {
				java.util.regex.Matcher m = OdocPara.DOC_NUMBER.matcher(p.text);
				if (m.find()) {
					docNo.append(m.group());
				}
			}
			int pt;
			boolean indentFirst;
			if (p.isHeading()) {
				pt = 16;
				indentFirst = false;
			} else if (p.endsWithColon() && p.text.length() <= 40) {
				pt = 16; // 主送机关，不缩进
				indentFirst = false;
			} else {
				pt = 16; // 正文，首行缩进 2 字
				indentFirst = true;
			}
			double fofd = pt * PT2UNIT;
			double lineH = fofd * 1.5;
			double indent = indentFirst ? 2 * fofd : 0;
			List<String> lines = wrap(p.text, fofd, indent);
			for (int li = 0; li < lines.size(); li++) {
				String line = lines.get(li);
				if (y + lineH > CONTENT_BOTTOM) {
					cur = new ArrayList<>();
					pages.add(cur);
					y = CONTENT_TOP;
				}
				double lineW = lineWidth(line, fofd);
				double x;
				if (p.align == OdocPara.HAlign.CENTER) {
					x = CONTENT_LEFT + (CONTENT_WIDTH - lineW) / 2.0;
				} else if (p.align == OdocPara.HAlign.RIGHT) {
					x = CONTENT_RIGHT - lineW;
				} else {
					x = CONTENT_LEFT + (li == 0 ? indent : 0);
				}
				addText(cur, x, y, Math.max(lineW, 1), fofd, pt, fofd * 0.8, line, false);
				y += lineH;
			}
		}

		// 页码（4 号=14pt）：奇右偶左、一字线包裹，末页也计
		for (int i = 0; i < pages.size(); i++) {
			int pageNo = i + 1;
			double fofd = 14 * PT2UNIT;
			String num = "— " + pageNo + " —";
			double lineW = lineWidth(num, fofd);
			double x = (pageNo % 2 == 1) ? CONTENT_RIGHT - lineW : CONTENT_LEFT;
			addText(pages.get(i), x, CONTENT_BOTTOM + 20, Math.max(lineW, 1), fofd, 14, fofd * 0.8, num, false);
		}

		// 组装 OFD 包
		String embedFont = resolveEmbedFont();
		byte[] fontBytes = OdocFontResolver.readFontBytes(props, embedFont);
		return assemble(pages, fontBytes, title.toString(), author.toString(), docNo.toString());
	}

	// ===================== 文本/行宽/换行 =====================

	private double charWidth(char ch, double fofd) {
		if (ch <= ' ') {
			return fofd * 0.4;
		}
		boolean wide = ch > 0x2E7F; // CJK 及全角标点按整字宽
		return wide ? fofd : fofd * 0.5;
	}

	private double lineWidth(String s, double fofd) {
		double w = 0;
		for (int i = 0; i < s.length(); i++) {
			w += charWidth(s.charAt(i), fofd);
		}
		return w;
	}

	/** 简单按字宽折行；indentFirst>0 时首行预留缩进。 */
	private List<String> wrap(String text, double fofd, double indent) {
		List<String> lines = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		double w = 0;
		double avail = CONTENT_WIDTH - indent;
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			double cw = charWidth(ch, fofd);
			if (sb.length() > 0 && w + cw > avail) {
				lines.add(sb.toString());
				sb = new StringBuilder();
				w = 0;
				avail = CONTENT_WIDTH;
			}
			sb.append(ch);
			w += cw;
		}
		if (sb.length() > 0) {
			lines.add(sb.toString());
		}
		if (lines.isEmpty()) {
			lines.add("");
		}
		return lines;
	}

	// ===================== OFD 对象 XML =====================

	private void addText(List<String> cur, double x, double y, double w, double h,
						 int pt, double baseline, String text, boolean red) {
		int id = cur.size() + 1;
		// GB/T 33190：TextObject.Size 单位为 pt（实数），非 0.1pt
		String sizeVal = String.valueOf(pt);
		StringBuilder sb = new StringBuilder();
		// 估算字宽留 4 单位余量，避免个别字符略宽被边界裁剪
		double bw = w + 4.0;
		sb.append("<ofd:TextObject ID=\"").append(id)
			.append("\" Boundary=\"").append(fmt(x)).append(' ').append(fmt(y)).append(' ')
			.append(fmt(bw)).append(' ').append(fmt(h))
			.append("\" Font=\"1\" Size=\"").append(sizeVal).append("\" CTM=\"1 0 0 1 0 0\">");
		if (red) {
			sb.append("<ofd:FillColor ColorSpace=\"RGB\" Value=\"255 0 0\"/>");
		}
		sb.append("<ofd:TextCode X=\"0\" Y=\"").append(fmt(baseline)).append("\">")
			.append(xmlEscape(text)).append("</ofd:TextCode></ofd:TextObject>");
		cur.add(sb.toString());
	}

	private void addRedLine(List<String> cur, double y) {
		int id = cur.size() + 1;
		double h = 5;
		StringBuilder sb = new StringBuilder();
		sb.append("<ofd:PathObject ID=\"").append(id)
			.append("\" Boundary=\"").append(fmt(CONTENT_LEFT)).append(' ').append(fmt(y))
			.append(' ').append(fmt(CONTENT_WIDTH)).append(' ').append(fmt(h))
			.append("\" CTM=\"1 0 0 1 0 0\">")
			.append("<ofd:AbbreviatedData>M 0 ").append(fmt(h / 2)).append(" L ")
			.append(fmt(CONTENT_WIDTH)).append(' ').append(fmt(h / 2)).append("</ofd:AbbreviatedData>")
			.append("<ofd:StrokeColor ColorSpace=\"RGB\" Value=\"255 0 0\"/>")
			.append("<ofd:LineWidth>5</ofd:LineWidth></ofd:PathObject>");
		cur.add(sb.toString());
	}

	// ===================== OFD 包组装 =====================

	private byte[] assemble(List<List<String>> pages, byte[] fontBytes,
							String title, String author, String docNo) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(bos)) {
			// OFD.xml 置首，部分阅读器按首入口解析
			putEntry(zos, "OFD.xml",
				ofdXml(title, author, docNo).getBytes(StandardCharsets.UTF_8));
			putEntry(zos, "Doc_0/Document.xml",
				documentXml(pages.size()).getBytes(StandardCharsets.UTF_8));
			for (int i = 0; i < pages.size(); i++) {
				String dir = "Doc_0/Pages/Page_" + i;
				putEntry(zos, dir + "/Page_" + i + ".xml", pageXml().getBytes(StandardCharsets.UTF_8));
				putEntry(zos, dir + "/Content.xml", contentXml(pages.get(i)).getBytes(StandardCharsets.UTF_8));
			}
			putEntry(zos, "Doc_0/Res/Fonts/Font_0.ttf", fontBytes);
		}
		return bos.toByteArray();
	}

	private void putEntry(ZipOutputStream zos, String name, byte[] data) throws IOException {
		zos.putNextEntry(new ZipEntry(name));
		zos.write(data);
		zos.closeEntry();
	}

	private String contentXml(List<String> objects) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<ofd:PageBlock xmlns:ofd=\"").append(OFD_NS)
			.append("\" Boundary=\"0 0 ").append(PAGE_W).append(' ').append(PAGE_H).append("\">\n");
		for (String o : objects) {
			sb.append("  ").append(o).append('\n');
		}
		sb.append("</ofd:PageBlock>\n");
		return sb.toString();
	}

	private String pageXml() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<ofd:Page xmlns:ofd=\"" + OFD_NS + "\" Area=\"0 0 " + PAGE_W + ' ' + PAGE_H
			+ "\" BaseLoc=\"Content.xml\"/>\n";
	}

	private String documentXml(int pageCount) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<ofd:Document xmlns:ofd=\"").append(OFD_NS)
			.append("\" PageArea=\"0 0 ").append(PAGE_W).append(' ').append(PAGE_H).append("\">\n");
		sb.append("  <ofd:CommonData>\n");
		sb.append("    <ofd:Font FontName=\"SimFang\" FamilyName=\"FangSong\" ID=\"1\">\n");
		sb.append("      <ofd:FontFile FileLoc=\"../Res/Fonts/Font_0.ttf\"/>\n");
		sb.append("    </ofd:Font>\n");
		sb.append("  </ofd:CommonData>\n");
		sb.append("  <ofd:Pages>\n");
		for (int i = 0; i < pageCount; i++) {
			sb.append("    <ofd:Page ID=\"").append(i + 1).append("\" BaseLoc=\"Pages/Page_")
				.append(i).append("/Page_").append(i).append(".xml\"/>\n");
		}
		sb.append("  </ofd:Pages>\n");
		sb.append("</ofd:Document>\n");
		return sb.toString();
	}

	private String ofdXml(String title, String author, String docNo) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<ofd:OFD xmlns:ofd=\"").append(OFD_NS)
			.append("\" Version=\"1.1\" DocType=\"OFD\">\n");
		sb.append("  <ofd:DocBody>\n");
		sb.append("    <ofd:DocInfo>\n");
		sb.append("      <ofd:DocID>").append(java.util.UUID.randomUUID()).append("</ofd:DocID>\n");
		if (!title.isEmpty()) {
			sb.append("      <ofd:Title>").append(xmlEscape(title)).append("</ofd:Title>\n");
		}
		if (!author.isEmpty()) {
			sb.append("      <ofd:Author>").append(xmlEscape(author)).append("</ofd:Author>\n");
		}
		sb.append("      <ofd:Subject>电子公文（GB/T 9704-2012）</ofd:Subject>\n");
		if (!docNo.isEmpty()) {
			sb.append("      <ofd:Keywords>").append(xmlEscape(docNo)).append("</ofd:Keywords>\n");
		}
		sb.append("      <ofd:Creator>SpringBlade</ofd:Creator>\n");
		sb.append("    </ofd:DocInfo>\n");
		sb.append("    <ofd:DocRoot>Doc_0/Document.xml</ofd:DocRoot>\n");
		sb.append("  </ofd:DocBody>\n");
		sb.append("</ofd:OFD>\n");
		return sb.toString();
	}

	private String resolveEmbedFont() {
		for (String f : EMBED_FONT_CANDIDATES) {
			if (OdocFontResolver.resolveFontFile(props, f) != null) {
				return f;
			}
		}
		return EMBED_FONT_CANDIDATES[0];
	}

	// ===================== 工具 =====================

	private boolean endsWithPunct(String s) {
		if (s == null || s.isEmpty()) {
			return false;
		}
		char last = s.charAt(s.length() - 1);
		return last == '。' || last == '，' || last == '；' || last == '：' || last == '、'
			|| last == '”' || last == '）' || last == '.' || last == ',';
	}

	private static String fmt(double v) {
		return String.format(Locale.US, "%.2f", v);
	}

	private static String xmlEscape(String s) {
		if (s == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '&': sb.append("&amp;"); break;
				case '<': sb.append("&lt;"); break;
				case '>': sb.append("&gt;"); break;
				case '"': sb.append("&quot;"); break;
				case '\'': sb.append("&apos;"); break;
				default: sb.append(c);
			}
		}
		return sb.toString();
	}

	private String withOfdExt(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			return "odoc.ofd";
		}
		int i = fileName.lastIndexOf('.');
		return (i > 0 ? fileName.substring(0, i) : fileName) + ".ofd";
	}
}
