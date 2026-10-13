package org.springblade.workflow;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.springblade.workflow.odoc.compose.PoiOdocComposeEngine;
import org.springblade.workflow.odoc.service.impl.WfOdocAiServiceImpl;
import org.springblade.workflow.odoc.vo.OdocLayoutRuleVO;
import org.springblade.workflow.odoc.vo.OdocSampleVO;
import org.springblade.workflow.odoc.vo.OdocSensitiveHitVO;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 套红合成引擎（POI）+ AI L1 功能测试。
 *
 * <p>纯 JUnit，无 Spring 上下文 / 无 Nacos / 无 MySQL：在内存构建含书签的 docx，
 * 验证「书签提取 → 字段替换 + 正文插入 → 输出」全链路，直接覆盖最易错的 XMLBeams 段落操作。</p>
 */
class WfOdocComposeAiTest {

	private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

	private final PoiOdocComposeEngine engine = new PoiOdocComposeEngine();

	/** 在段落上包一个书签；text 非空则书签内放一个占位 run */
	private static void wrapWithBookmark(XWPFParagraph p, String name, int id, String text) {
		CTP ctP = p.getCTP();
		Document dom = ctP.getDomNode().getOwnerDocument();
		Element start = dom.createElementNS(W, "bookmarkStart");
		start.setAttributeNS(W, "w:name", name);
		start.setAttribute("w:id", String.valueOf(id));
		ctP.getDomNode().appendChild(start);
		if (text != null) {
			p.createRun().setText(text);
		}
		Element end = dom.createElementNS(W, "bookmarkEnd");
		end.setAttribute("w:id", String.valueOf(id));
		ctP.getDomNode().appendChild(end);
	}

	/** 构造套红模板：标题书签(占位) + 正文占位书签(空) + 文号书签(占位) */
	private byte[] templateDocx() throws Exception {
		try (XWPFDocument doc = new XWPFDocument()) {
			XWPFParagraph p1 = doc.createParagraph();
			wrapWithBookmark(p1, "fld_title", 1, "标题占位");
			XWPFParagraph p2 = doc.createParagraph();
			wrapWithBookmark(p2, "Body", 2, null);
			XWPFParagraph p3 = doc.createParagraph();
			wrapWithBookmark(p3, "fld_number", 3, "号占位");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			doc.write(out);
			return out.toByteArray();
		}
	}

	private byte[] bodyDocx(String... lines) throws Exception {
		try (XWPFDocument doc = new XWPFDocument()) {
			for (String l : lines) {
				doc.createParagraph().createRun().setText(l);
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			doc.write(out);
			return out.toByteArray();
		}
	}

	private List<String> texts(byte[] docx) throws Exception {
		try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docx))) {
			List<String> t = new ArrayList<>();
			for (XWPFParagraph p : doc.getParagraphs()) {
				t.add(p.getText());
			}
			return t;
		}
	}

	@Test
	void extractBookmarks_findsAll() throws Exception {
		List<String> bms = engine.extractBookmarks(templateDocx());
		assertTrue(bms.contains("fld_title"), "应含标题书签: " + bms);
		assertTrue(bms.contains("Body"), "应含正文书签: " + bms);
		assertTrue(bms.contains("fld_number"), "应含文号书签: " + bms);
	}

	@Test
	void compose_replacesFieldBookmarks_and_insertsBody() throws Exception {
		Map<String, String> values = new HashMap<>();
		values.put("fld_title", "关于测试的通知");
		values.put("fld_number", "X发〔2026〕1号");

		byte[] out = engine.compose(
			templateDocx(),
			bodyDocx("第一段正文。", "第二段正文。"),
			values,
			Set.of("Body"));

		List<String> t = texts(out);
		assertTrue(t.contains("关于测试的通知"), "标题书签应被替换为字段值: " + t);
		assertTrue(t.contains("X发〔2026〕1号"), "文号书签应被替换为字段值: " + t);
		assertTrue(t.contains("第一段正文。"), "正文应插入到 Body 书签处: " + t);
		assertTrue(t.contains("第二段正文。"), "正文应插入到 Body 书签处: " + t);
		assertFalse(t.contains("标题占位"), "占位符应被替换");
		assertFalse(t.contains("号占位"), "占位符应被替换");
	}

	@Test
	void compose_withoutBody_onlyReplacesFields() throws Exception {
		Map<String, String> values = new HashMap<>();
		values.put("fld_title", "仅字段替换");
		byte[] out = engine.compose(templateDocx(), new byte[0], values, Set.of("Body"));
		List<String> t = texts(out);
		assertTrue(t.contains("仅字段替换"), "无正文时仅替换字段: " + t);
	}

	@Test
	void ai_l1_layoutSampleSensitive() {
		WfOdocAiServiceImpl ai = new WfOdocAiServiceImpl();
		// 智能排版
		assertNotNull(ai.suggestLayout("TONGZHI"));
		List<OdocLayoutRuleVO> rules = ai.layoutRules("TONGZHI");
		assertFalse(rules.isEmpty(), "版式规则不应为空");
		assertTrue(rules.stream().anyMatch(r -> "title".equals(r.getElementType())), "应含标题要素");
		// 范文
		List<OdocSampleVO> samples = ai.samples("QINGSHI");
		assertFalse(samples.isEmpty());
		assertTrue(samples.stream().anyMatch(s -> "QINGSHI".equals(s.getDocType())), "应含请示专篇");
		// 敏感词
		List<OdocSensitiveHitVO> hits = ai.checkSensitive("这里包含密码和账号信息");
		assertTrue(hits.size() >= 2, "应命中密码/账号: " + hits);
		assertTrue(ai.checkSensitive("").isEmpty());
		assertTrue(ai.checkSensitive(null).isEmpty());
	}
}
