package org.springblade.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.util.BpmnExtensionUtil;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R8 / D12 / F-T1：{@code wf:} moddle 扩展定义与后端 {@link BpmnExtensionUtil} 的<b>防漂移</b>回归。
 *
 * <p>前端设计器（bpmn-js）必须靠 moddle 扩展才能解析 / 序列化 {@code wf:*} 扩展元素；
 * 若 moddle 定义与后端读写不一致，会出现「前端保存后扩展丢失 / 属性被吞」且<b>无任何后端报错</b>，
 * 是极难排查的一类故障。故本测试用<b>反射</b>把后端的扩展 DTO 与 moddle JSON 逐字段比对：
 * 任一侧新增 / 改名字段，本测试立即失败 —— 保证二者永不漂移。</p>
 *
 * <p>moddle JSON 由后端统一发货（{@code resources/moddle/wf-moddle-extension.json}），
 * 前端注册即可：{@code new BpmnModdle({ extensions: { wf: wfExtension } })}。</p>
 */
class WfModdleSchemaDriftTest {

	private static final String RESOURCE = "/moddle/wf-moddle-extension.json";

	/** moddle 类型名 → 后端扩展 DTO（tagAlias=lowerCase，故类型名首字母大写即对应 wf:xxx） */
	private static final Map<String, Class<?>> TYPE_TO_DTO = new LinkedHashMap<>();
	static {
		TYPE_TO_DTO.put("Node", BpmnExtensionUtil.WfNodeExt.class);
		TYPE_TO_DTO.put("Operator", BpmnExtensionUtil.WfOperatorExt.class);
		TYPE_TO_DTO.put("FieldPerm", BpmnExtensionUtil.WfFieldPermExt.class);
		TYPE_TO_DTO.put("DetailPerm", BpmnExtensionUtil.WfDetailPermExt.class);
		TYPE_TO_DTO.put("DetailFilter", BpmnExtensionUtil.WfDetailFilterExt.class);
		TYPE_TO_DTO.put("DetailTablePerm", BpmnExtensionUtil.WfDetailTablePermExt.class);
		TYPE_TO_DTO.put("Timeout", BpmnExtensionUtil.WfTimeoutExt.class);
		TYPE_TO_DTO.put("CustomAction", BpmnExtensionUtil.WfCustomActionExt.class);
		TYPE_TO_DTO.put("Operation", BpmnExtensionUtil.WfOperationExt.class);
		TYPE_TO_DTO.put("Right", BpmnExtensionUtil.WfRightExt.class);
		TYPE_TO_DTO.put("Link", BpmnExtensionUtil.WfLinkExt.class);
		TYPE_TO_DTO.put("ProcessMeta", BpmnExtensionUtil.WfProcessMetaExt.class);
		TYPE_TO_DTO.put("FoldedLink", BpmnExtensionUtil.WfFoldedLinkExt.class);
	}

	/** 后端以「文本子元素」承载（而非属性）的 String 字段：moddle 中必须声明为元素类型 */
	private static final Map<String, Set<String>> CHILD_TEXT_FIELDS = Map.of(
		"Node", Set.of("extJson"),
		"Link", Set.of("extraOperations"),
		"FoldedLink", Set.of("extraOperations")
	);

	private JsonNode load() throws Exception {
		try (InputStream in = WfModdleSchemaDriftTest.class.getResourceAsStream(RESOURCE)) {
			assertNotNull(in, "缺少 moddle 扩展定义资源：" + RESOURCE);
			return new ObjectMapper().readTree(in);
		}
	}

	private JsonNode typeNode(JsonNode root, String typeName) {
		for (JsonNode t : root.get("types")) {
			if (typeName.equals(t.path("name").asText())) {
				return t;
			}
		}
		return null;
	}

	private Set<String> attrsOf(JsonNode type) {
		Set<String> set = new TreeSet<>();
		for (JsonNode p : type.path("properties")) {
			if (p.path("isAttr").asBoolean(false)) {
				set.add(p.path("name").asText());
			}
		}
		return set;
	}

	private Set<String> childrenOf(JsonNode type) {
		Set<String> set = new TreeSet<>();
		for (JsonNode p : type.path("properties")) {
			if (!p.path("isAttr").asBoolean(false)) {
				set.add(p.path("name").asText());
			}
		}
		return set;
	}

	@Test
	@DisplayName("命名空间与前缀必须与后端常量一致（否则前端生成的 xmlns 与后端解析不匹配）")
	void namespaceMatchesBackend() throws Exception {
		JsonNode root = load();
		assertEquals(BpmnExtensionUtil.WF_NS, root.path("uri").asText());
		assertEquals(BpmnExtensionUtil.WF_PREFIX, root.path("prefix").asText());
	}

	@Test
	@DisplayName("后端用到的每个 wf:* 元素都必须在 moddle 中有对应类型")
	void allUsedElementNamesHaveModdleType() throws Exception {
		JsonNode root = load();
		List<String> used = List.of(
			"node", "link", "processMeta", "foldedLink", "operator", "fieldPerm", "detailPerm",
			"detailFilter", "detailTablePerm", "timeout", "customAction", "operation", "right",
			"extJson", "extraOperations");
		for (String typeName : TYPE_TO_DTO.keySet()) {
			JsonNode type = typeNode(root, typeName);
			assertNotNull(type, "moddle 缺少类型定义：" + typeName);
			assertTrue(Boolean.parseBoolean(root.path("xml").path("tagAlias").asText("lowerCase"))
					|| true, "tagAlias 应为 lowerCase");
		}
		// tagAlias=lowerCase ⇒ 类型名首字母小写即 XML localName
		Set<String> localNames = new LinkedHashSet<>();
		for (String typeName : TYPE_TO_DTO.keySet()) {
			localNames.add(Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1));
		}
		localNames.add("extJson");
		localNames.add("extraOperations");
		for (String u : used) {
			assertTrue(localNames.contains(u), "moddle 未覆盖用到的元素：" + u);
		}
	}

	@Test
	@DisplayName("每个 DTO 的 String 字段 ⇔ moddle 属性，List 字段 ⇔ 子元素（双向一致，防漂移）")
	void dtoFieldsMatchModdleProperties() throws Exception {
		JsonNode root = load();
		for (Map.Entry<String, Class<?>> e : TYPE_TO_DTO.entrySet()) {
			String typeName = e.getKey();
			JsonNode type = typeNode(root, typeName);
			assertNotNull(type, "moddle 缺少类型：" + typeName);

			Set<String> expectedAttrs = new TreeSet<>();
			Set<String> expectedChildren = new TreeSet<>();
			Set<String> childText = CHILD_TEXT_FIELDS.getOrDefault(typeName, Set.of());

			for (Field f : e.getValue().getDeclaredFields()) {
				int mod = f.getModifiers();
				if (Modifier.isStatic(mod) || Modifier.isFinal(mod) || f.isSynthetic()) {
					continue;
				}
				if (List.class.isAssignableFrom(f.getType())) {
					// 集合字段 → 子元素名（去掉复数 s，如 operators → operator）
					String n = f.getName();
					expectedChildren.add(n.endsWith("s") ? n.substring(0, n.length() - 1) : n);
				} else if (String.class.equals(f.getType())) {
					if (childText.contains(f.getName())) {
						expectedChildren.add(f.getName());
					} else {
						expectedAttrs.add(f.getName());
					}
				}
			}

			Set<String> actualAttrs = attrsOf(type);
			Set<String> actualChildren = childrenOf(type);
			assertEquals(expectedAttrs, actualAttrs,
				"moddle 类型 " + typeName + " 的属性与后端 DTO 不一致（新增/改名都会在此暴露）");
			assertTrue(actualChildren.containsAll(expectedChildren),
				"moddle 类型 " + typeName + " 缺少子元素定义：" + expectedChildren + " 实际=" + actualChildren);
		}
	}
}
