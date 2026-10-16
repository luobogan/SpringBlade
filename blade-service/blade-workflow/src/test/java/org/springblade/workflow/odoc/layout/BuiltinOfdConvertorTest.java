package org.springblade.workflow.odoc.layout;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.odoc.config.OdocProperties;
import org.springblade.workflow.spi.OdocConvertResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内置 OFD 写出器离线验证：构造套红 docx → 写出 OFD（GB/T 33190）ZIP 包，断言结构合法、
 * 关键 XML 入口齐备、字体已嵌入、元数据已写入 DocInfo。不依赖 DB / 厂商 SDK。
 */
public class BuiltinOfdConvertorTest {

	@Test
	public void renderRedHeadedDocxToOfd() throws Exception {
		try (XWPFDocument doc = new XWPFDocument()) {
			addRed(doc, "某某集团有限公司文件");
			addNormal(doc, "〔2026〕12号");
			addTitle(doc, "关于深入开展公文版式规范化工作的通知");
			addRecv(doc, "各部门、各子公司：");
			addBody(doc, "为进一步加强公文管理，依据《党政机关公文格式》（GB/T 9704-2012），现就有关事项通知如下：");
			addBody(doc, "一、提高思想认识。各单位要充分认识公文格式规范化的重大意义。");
			addBody(doc, "（一）加强组织领导。成立专项工作小组，统筹推进各项工作。");
			addBody(doc, "二、严格落实现有标准。定稿后正文冻结，转版式文件并归档。");

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			doc.write(bos);
			byte[] docx = bos.toByteArray();

			BuiltinOfdConvertor convertor = new BuiltinOfdConvertor(new OdocProperties());
			assertTrue(convertor.available(), "内置 OFD 写出器应始终可用");
			assertTrue(convertor.supports("13"), "应支持 OFD(13)");
			assertFalse(convertor.supports("12"), "不应处理 PDF(12)");

			OdocConvertResult r = convertor.toLayout(docx, "13", "test.docx");
			assertTrue(r.isSuccess(), "OFD 写出失败: " + r.getMessage());
			assertNotNull(r.getContent());
			assertTrue(r.getContent().length > 1000, "OFD 内容不应为空壳");

			// ZIP 魔数
			assertEquals((byte) 'P', (byte) r.getContent()[0]);
			assertEquals((byte) 'K', (byte) r.getContent()[1]);

			Set<String> entries = new HashSet<>();
			StringBuilder contentXml = new StringBuilder();
			StringBuilder ofdXml = new StringBuilder();
			try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(r.getContent()))) {
				ZipEntry e;
				while ((e = zis.getNextEntry()) != null) {
					entries.add(e.getName());
					byte[] data = readAll(zis);
					String s = new String(data, java.nio.charset.StandardCharsets.UTF_8);
					if (e.getName().endsWith(".xml")) {
						// 校验 XML 良构性（OFD 阅读器对结构敏感）
						javax.xml.parsers.DocumentBuilderFactory dbf =
							javax.xml.parsers.DocumentBuilderFactory.newInstance();
						dbf.setNamespaceAware(true);
						try {
							dbf.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(data));
						} catch (Exception ex) {
							throw new AssertionError("OFD 内 XML 非良构: " + e.getName(), ex);
						}
					}
					if (e.getName().equals("Doc_0/Pages/Page_0/Content.xml")) {
						contentXml.append(s);
					} else if (e.getName().equals("OFD.xml")) {
						ofdXml.append(s);
					}
				}
			}
			assertTrue(entries.contains("OFD.xml"), "应包含 OFD.xml");
			assertTrue(entries.contains("Doc_0/Document.xml"), "应包含 Document.xml");
			assertTrue(entries.contains("Doc_0/Pages/Page_0/Page_0.xml"), "应包含 Page_0.xml");
			assertTrue(entries.contains("Doc_0/Pages/Page_0/Content.xml"), "应包含 Content.xml");
			assertTrue(entries.contains("Doc_0/Res/Fonts/Font_0.ttf"), "应嵌入字体 Font_0.ttf");
			assertTrue(contentXml.toString().contains("TextObject"), "Content 应包含文本对象");
			assertTrue(contentXml.toString().contains("关于深入开展公文版式规范化工作"), "应包含正文文本");
			// 元数据注入 DocInfo
			assertTrue(ofdXml.toString().contains("<ofd:Title>"), "DocInfo 应包含标题元数据");
			assertTrue(ofdXml.toString().contains("某某集团有限公司文件"), "DocInfo 应包含发文机关元数据");

			Path out = Paths.get(System.getProperty("java.io.tmpdir"), "odoc-builtin-sample.ofd");
			Files.write(out, r.getContent());
			System.out.println("内置 OFD 写出器产物已写出: " + out.toAbsolutePath());
		}
	}

	@Test
	public void rejectPdfFormat() {
		BuiltinOfdConvertor convertor = new BuiltinOfdConvertor(new OdocProperties());
		OdocConvertResult r = convertor.toLayout(new byte[]{1, 2, 3}, "12", "test.docx");
		assertFalse(r.isSuccess(), "PDF 应由内置 PDF 渲染器处理，OFD 写出器应返回失败");
	}

	private byte[] readAll(ZipInputStream zis) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		int n;
		while ((n = zis.read(buf)) > 0) {
			bos.write(buf, 0, n);
		}
		return bos.toByteArray();
	}

	private void addRed(XWPFDocument doc, String text) {
		XWPFParagraph p = doc.createParagraph();
		p.setAlignment(ParagraphAlignment.CENTER);
		XWPFRun r = p.createRun();
		r.setText(text);
		r.setColor("FF0000");
		r.setBold(true);
		r.setFontSize(20);
	}

	private void addNormal(XWPFDocument doc, String text) {
		XWPFParagraph p = doc.createParagraph();
		p.setAlignment(ParagraphAlignment.CENTER);
		XWPFRun r = p.createRun();
		r.setText(text);
		r.setFontSize(16);
	}

	private void addTitle(XWPFDocument doc, String text) {
		XWPFParagraph p = doc.createParagraph();
		p.setAlignment(ParagraphAlignment.CENTER);
		XWPFRun r = p.createRun();
		r.setText(text);
		r.setBold(true);
		r.setFontSize(22);
	}

	private void addRecv(XWPFDocument doc, String text) {
		XWPFParagraph p = doc.createParagraph();
		XWPFRun r = p.createRun();
		r.setText(text);
		r.setFontSize(16);
	}

	private void addBody(XWPFDocument doc, String text) {
		XWPFParagraph p = doc.createParagraph();
		XWPFRun r = p.createRun();
		r.setText(text);
		r.setFontSize(16);
	}
}
