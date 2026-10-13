package org.springblade.workflow.odoc.compose;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于 Apache POI 的套红合成引擎。
 *
 * <p>书签识别走 DOM 遍历（w:bookmarkStart 的 w:name），不依赖 POI 精简 schema 的
 * CTBookmarkStart 类（poi-ooxml-lite 未内置），兼容性更好。实现要点：</p>
 * <ul>
 *   <li>字段书签：把书签所在段落的文字替换为字段值（约定每个书签独占一段/行，
 *       红头静态文字不落在书签内，故不受影响）。</li>
 *   <li>正文书签：在正文占位书签所在段落处插入正文 docx 的全部段落，并删除占位段落。</li>
 * </ul>
 * 说明：真实红头模板书签结构以贵社模板为准，接入后请用样例 docx 验证一次。
 */
@Slf4j
@Component
public class PoiOdocComposeEngine implements IOdocComposeEngine {

	private static final String W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

	@Override
	public List<String> extractBookmarks(byte[] docxBytes) {
		try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
			Set<String> names = new LinkedHashSet<>();
			for (XWPFParagraph p : doc.getParagraphs()) {
				names.addAll(bookmarkNames(p.getCTP().getDomNode()));
			}
			return new ArrayList<>(names);
		} catch (Exception e) {
			throw new IllegalStateException("解析 docx 书签失败", e);
		}
	}

	@Override
	public byte[] compose(byte[] templateBytes, byte[] bodyBytes,
						  Map<String, String> bookmarkValues, Set<String> contentBookmarks) {
		if (templateBytes == null || templateBytes.length == 0) {
			throw new IllegalArgumentException("套红模板为空");
		}
		try (XWPFDocument template = new XWPFDocument(new ByteArrayInputStream(templateBytes))) {
			// 1) 先做字段书签替换（操作 run，尚未做结构变更，引用有效）
			replaceFieldBookmarks(template, bookmarkValues, contentBookmarks);

			// 2) 再插入正文：用 DOM importNode/insertBefore/removeChild，
			// 避免 XMLBeans 的 insertNewP/setPArray/removeP 断开既有 run 引用。
			String contentBm = firstContentBookmark(contentBookmarks);
			if (contentBm != null && bodyBytes != null && bodyBytes.length > 0) {
				insertBodyAtBookmark(template, contentBm, bodyBytes);
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			template.write(out);
			return out.toByteArray();
		} catch (Exception e) {
			throw new IllegalStateException("套红合成失败", e);
		}
	}

	private String firstContentBookmark(Set<String> contentBookmarks) {
		if (contentBookmarks == null) {
			return null;
		}
		for (String b : contentBookmarks) {
			if (b != null && !b.isBlank()) {
				return b;
			}
		}
		return null;
	}

	/** 在正文占位书签所在段落处插入正文全部段落，并删除占位段落（纯 DOM 操作） */
	private void insertBodyAtBookmark(XWPFDocument template, String contentBm, byte[] bodyBytes) throws Exception {
		List<CTP> pList = template.getDocument().getBody().getPList();
		CTP placeholder = null;
		for (CTP p : pList) {
			if (bookmarkNames(p.getDomNode()).contains(contentBm)) {
				placeholder = p;
				break;
			}
		}
		if (placeholder == null) {
			log.warn("套红模板未标注正文占位书签[{}]，跳过正文插入", contentBm);
			return;
		}
		org.w3c.dom.Node phNode = placeholder.getDomNode();
		org.w3c.dom.Node parent = phNode.getParentNode();
		org.w3c.dom.Document tdoc = parent.getOwnerDocument();
		int inserted = 0;
		try (XWPFDocument bodyDoc = new XWPFDocument(new ByteArrayInputStream(bodyBytes))) {
			for (CTP src : bodyDoc.getDocument().getBody().getPList()) {
				org.w3c.dom.Node copy = tdoc.importNode(src.getDomNode(), true);
				parent.insertBefore(copy, phNode);
				inserted++;
			}
		}
		parent.removeChild(phNode);
		log.info("套红正文插入完成 contentBm={} 插入段落数={}", contentBm, inserted);
	}

	/** 字段书签：替换书签所在段落文字为字段值 */
	private void replaceFieldBookmarks(XWPFDocument template, Map<String, String> values, Set<String> contentBookmarks) {
		if (values == null || values.isEmpty()) {
			return;
		}
		for (XWPFParagraph p : template.getParagraphs()) {
			List<String> bms = bookmarkNames(p.getCTP().getDomNode());
			String target = null;
			for (String b : bms) {
				if (contentBookmarks != null && contentBookmarks.contains(b)) {
					continue;
				}
				if (values.containsKey(b)) {
					target = b;
					break;
				}
			}
			if (target == null) {
				continue;
			}
			String val = values.get(target);
			List<XWPFRun> runs = p.getRuns();
			if (!runs.isEmpty()) {
				runs.get(0).setText(val, 0);
				for (int i = 1; i < runs.size(); i++) {
					runs.get(i).setText("", 0);
				}
			} else {
				p.createRun().setText(val);
			}
		}
	}

	/** 从一段（w:p）DOM 中提取书签名（w:bookmarkStart/@w:name），忽略 _GoBack 等内置书签 */
	private List<String> bookmarkNames(Node paragraph) {
		List<String> names = new ArrayList<>();
		if (paragraph == null) {
			return names;
		}
		NodeList children = paragraph.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			if (!"bookmarkStart".equals(n.getLocalName())) {
				continue;
			}
			Element el = (Element) n;
			String nm = el.getAttributeNS(W_NS, "name");
			if (nm == null || nm.isBlank() || nm.startsWith("_GoBack")) {
				// 兼容非命名空间属性的退化 DOM
				nm = el.getAttribute("w:name");
			}
			if (nm != null && !nm.isBlank() && !nm.startsWith("_GoBack")) {
				names.add(nm);
			}
		}
		return names;
	}
}
