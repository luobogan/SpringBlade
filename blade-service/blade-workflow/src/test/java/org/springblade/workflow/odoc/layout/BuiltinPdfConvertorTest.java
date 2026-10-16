package org.springblade.workflow.odoc.layout;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.odoc.config.OdocProperties;
import org.springblade.workflow.spi.OdocConvertResult;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内置版式渲染器离线验证：构造套红 docx → 渲染 PDF，断言 PDF 合法且可落盘肉眼检查。
 * 不依赖 DB / 外部服务 / 厂商 SDK。
 */
public class BuiltinPdfConvertorTest {

	@Test
	public void renderRedHeadedDocxToPdf() throws Exception {
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

			BuiltinPdfConvertor convertor = new BuiltinPdfConvertor(new OdocProperties());
			assertTrue(convertor.available(), "内置渲染器应始终可用");

			OdocConvertResult r = convertor.toLayout(docx, "12", "test.docx");
			assertTrue(r.isSuccess(), "渲染失败: " + r.getMessage());
			assertNotNull(r.getContent());
			assertTrue(r.getContent().length > 0, "PDF 内容不应为空");

			String head = new String(r.getContent(), 0, 5, StandardCharsets.US_ASCII);
			assertEquals("%PDF-", head, "应为合法 PDF 文件头");

			Path out = Paths.get(System.getProperty("java.io.tmpdir"), "odoc-builtin-sample.pdf");
			Files.write(out, r.getContent());
			System.out.println("内置渲染器 PDF 已写出: " + out.toAbsolutePath());
		}
	}

	@Test
	public void rejectOfdFormat() {
		BuiltinPdfConvertor convertor = new BuiltinPdfConvertor(new OdocProperties());
		OdocConvertResult r = convertor.toLayout(new byte[]{1, 2, 3}, "13", "test.docx");
		assertTrue(!r.isSuccess(), "OFD 应由厂商 SPI 处理，内置渲染器应返回失败");
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
