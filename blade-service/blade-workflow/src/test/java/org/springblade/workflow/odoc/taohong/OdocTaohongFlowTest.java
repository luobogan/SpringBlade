package org.springblade.workflow.odoc.taohong;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 发文流程（odoc_fawen）套红端到端集成测试。
 *
 * <p>覆盖 {@code POST /instance/start} 以给定 defId 发起流程实例，以及
 * {@code POST /odoc/taohong} 套红后<b>合成文档的内容正确性</b>：
 * 红头（模板静态内容与版式）保留、标题/文号按书签→字段映射落地、正文段落完整插入；
 * 并校验关键响应字段与参数异常时的错误处理。</p>
 *
 * <p>运行前提：网关已启动。登录需前端 SM2 加密密码，Java 侧复刻成本高，
 * 故 token 由外部传入：</p>
 * <pre>
 * mvn -pl blade-workflow test -Dtest=OdocTaohongFlowTest \
 *     -Dsword.baseUrl=http://192.168.1.5:8000/api -Dsword.token=&lt;token&gt;
 * </pre>
 * <p>token 缺失或服务不可达时测试自动跳过（不会误报失败）。</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OdocTaohongFlowTest {

	private static final String PREFIX = "/blade-workflow";

	/** 流程定义 2109915450423955457（发文流程 odoc_fawen） */
	private static final long DEF_ID = 2109915450423955457L;
	/** 表单 2109677294030196738 */
	private static final long FORM_ID = 2109677294030196738L;
	/** 套红模板 2110003737889161217（lt-rh2，mouldType=2） */
	private static final long MOULD_ID = 2110003737889161217L;

	private static final String TITLE = "测试公文标题-套红验证";
	private static final String NUMBER = "发[2026]第001号";
	private static final String BODY_P1 = "正文第一段：这是用于校验套红正文插入的首段文字。";
	private static final String BODY_P2 = "正文第二段：这是用于校验套红正文插入的尾段文字。";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static HttpClient http;
	private static String baseUrl;
	private static String token;

	// 用例间共享的懒加载状态（保证用例可独立、顺序无关）
	private static Long instanceId;
	private static Long docId;
	private static Long composedVersionId;
	private static byte[] composedBytes;
	private static JsonNode taohongData;

	@BeforeAll
	static void setUp() {
		token = firstNonBlank(sysProp("sword.token"), System.getenv("SWORD_TOKEN"));
		baseUrl = firstNonBlank(sysProp("sword.baseUrl"), System.getenv("SWORD_BASE_URL"), "http://localhost:8000/api");
		Assumptions.assumeTrue(token != null,
			"缺少 sword.token（系统属性 -Dsword.token 或环境变量 SWORD_TOKEN），跳过套红流程集成测试");

		// 强制 HTTP/1.1：网关（Reactor Netty）对 HTTP/2 升级握手会直接断连，
		// 表现为 "HTTP/1.1 header parser received no bytes"
		http = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1)
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();
		try {
			// 连通性/鉴权预检：拿不到 200 说明环境不具备，整体跳过
			Resp probe = get("/odoc/mould/" + MOULD_ID);
			Assumptions.assumeTrue(probe.status < 500 && probe.ok(),
				"网关不可用或鉴权失败（baseUrl=" + baseUrl + "），跳过");
		} catch (Exception e) {
			Assumptions.abort("网关不可达（" + baseUrl + "）：" + e.getMessage());
		}
	}

	// ==================== 用例 ====================

	@Test
	@Order(1)
	void startInstance_withGivenDefId_createsInstance() throws Exception {
		// 1) 先建业务数据行拿 dataId（form/save：只存不流转）
		Map<String, Object> fieldValues = new LinkedHashMap<>();
		fieldValues.put("fwlcbt", TITLE);
		fieldValues.put("wh1", NUMBER);
		fieldValues.put("mj", "秘密");
		fieldValues.put("jjcd", 0);

		Map<String, Object> saveDto = new LinkedHashMap<>();
		saveDto.put("defId", DEF_ID);
		saveDto.put("formId", FORM_ID);
		saveDto.put("fieldValues", fieldValues);

		Resp saved = postJson("/form/save", MAPPER.writeValueAsString(saveDto));
		// 表单建模未就绪时允许失败：start 会自行建业务行
		String dataId = (saved.ok() && saved.data() != null) ? saved.data().asText("") : "";

		// 2) 发起流程
		Map<String, Object> startDto = new LinkedHashMap<>();
		startDto.put("defId", DEF_ID);
		startDto.put("formId", FORM_ID);
		if (!dataId.isBlank()) {
			startDto.put("dataId", Long.parseLong(dataId));
		}
		startDto.put("title", TITLE);
		startDto.put("fieldValues", fieldValues);

		Resp started = postJson("/instance/start", MAPPER.writeValueAsString(startDto));
		assertTrue(started.ok(), "发起流程应成功，实际：" + started);

		// 关键响应字段：data 为实例ID（字符串，19位雪花）
		String instId = started.data() == null ? "" : started.data().asText("");
		assertTrue(instId.matches("\\d{15,25}"), "实例ID应为雪花数字串，实际：" + instId);

		instanceId = Long.parseLong(instId);
	}

	@Test
	@Order(2)
	void taohong_composesRedHeaderAndBody() throws Exception {
		setupTaohong();

		// ---- 校验套红响应关键字段 ----
		assertNotNull(taohongData, "套红响应 data 不应为空");
		String fileName = taohongData.path("fileName").asText("");
		int docFileType = taohongData.path("docFileType").asInt(0);
		String detail = taohongData.path("versionDetail").asText("");
		assertTrue(fileName.endsWith(".docx"), "套红产物应是 docx，实际：" + fileName);
		assertTrue(docFileType == 7, "套红产物 docFileType 应为 7(docx)，实际：" + docFileType);
		assertTrue(detail.contains("套红"), "版本说明应含「套红」，实际：" + detail);

		// ---- 校验合成文档内容 ----
		List<String> composedTexts = paragraphTexts(composedBytes);
		String all = String.join("\n", composedTexts);

		// 标题/文号：按书签→字段映射（fld_title→gwbt、fld_number→bt）落地
		assertTrue(all.contains(TITLE), "合成文档应含标题[" + TITLE + "]，实际段落：" + composedTexts);
		assertTrue(all.contains(NUMBER), "合成文档应含文号[" + NUMBER + "]，实际段落：" + composedTexts);

		// 正文段落：完整插入且顺序正确
		int i1 = indexOfContains(composedTexts, BODY_P1);
		int i2 = indexOfContains(composedTexts, BODY_P2);
		assertTrue(i1 >= 0, "合成文档应含正文第一段，实际段落：" + composedTexts);
		assertTrue(i2 >= 0, "合成文档应含正文第二段，实际段落：" + composedTexts);
		assertTrue(i1 < i2, "正文段落顺序应保持，实际下标 " + i1 + "→" + i2);
		assertTrue(composedTexts.get(i1).contains(BODY_P1) && composedTexts.get(i2).contains(BODY_P2),
			"正文段落文字应完整（不被截断）");

		// 红头/版式：模板静态（非书签）内容应原样保留
		List<String> staticTexts = templateStaticTexts();
		// 守护：模板必须真有静态红头/版式内容，否则下面这条校验会退化为空跑
		assertFalse(staticTexts.isEmpty(),
			"套红模板应含非书签的静态红头/版式内容，实际为空：" + MOULD_ID);
		for (String st : staticTexts) {
			assertTrue(all.contains(st), "红头静态内容应保留：[" + st + "]，实际段落：" + composedTexts);
		}
	}

	@Test
	@Order(3)
	void taohongInfo_reportsUsedTempletAndMustTaohong() throws Exception {
		setupTaohong();

		Resp info = get("/odoc/taohong-info?docId=" + docId);
		assertTrue(info.ok(), "taohong-info 应成功，实际：" + info);
		JsonNode d = info.data();
		assertNotNull(d, "taohong-info 应返回 data");

		assertTrue(d.path("hasUsedTemplet").asInt(0) == 1, "套红后 hasUsedTemplet 应为 1，实际：" + d);
		assertTrue(d.path("mustTaohong").asInt(0) == 1, "发文流程本节点应强制套红，实际：" + d);

		// 可用套红模板应包含本次使用的模板
		JsonNode moulds = d.path("moulds");
		boolean contains = false;
		if (moulds.isArray()) {
			for (JsonNode m : moulds) {
				if (m.path("id").asLong(0) == MOULD_ID) {
					contains = true;
					break;
				}
			}
		}
		assertTrue(contains, "可用套红模板应包含 " + MOULD_ID + "，实际：" + moulds);
	}

	@Test
	@Order(4)
	void taohong_withInvalidParams_reportsErrors() throws Exception {
		// 1) docId 不存在
		Resp notExistDoc = postJson("/odoc/taohong?docId=999999999999999999&mouldId=" + MOULD_ID, "{}");
		assertFalse(notExistDoc.ok(), "不存在的正文应报错，实际：" + notExistDoc);
		assertTrue(notExistDoc.msg().contains("正文不存在"), "应提示正文不存在，实际：" + notExistDoc.msg());

		// 2) mouldId 不存在 / 非套红类型
		long doc = ensureDoc();
		Resp badMould = postJson("/odoc/taohong?docId=" + doc + "&mouldId=999999999999999999", "{}");
		assertFalse(badMould.ok(), "非法模板应报错，实际：" + badMould);
		assertTrue(badMould.msg().contains("套红模板不存在或类型不符"), "应提示模板不符，实际：" + badMould.msg());

		// 3) 正文无任何版本
		long emptyDoc = initDoc(uniqueInstanceId(), false);
		Resp noVersion = postJson("/odoc/taohong?docId=" + emptyDoc + "&mouldId=" + MOULD_ID, "{}");
		assertFalse(noVersion.ok(), "无版本的正文应报错，实际：" + noVersion);
		assertTrue(noVersion.msg().contains("正文暂无版本"), "应提示暂无版本，实际：" + noVersion.msg());

		// 4) 缺 docId 参数：应被参数绑定拦截（4xx）
		Resp missing = postJson("/odoc/taohong?mouldId=" + MOULD_ID, "{}");
		assertTrue(missing.status >= 400 || !missing.ok(), "缺 docId 应报参数错误，实际：" + missing);
	}

	@Test
	@Order(5)
	void startInstance_withInvalidDefId_reportsError() throws Exception {
		Map<String, Object> dto = new LinkedHashMap<>();
		dto.put("defId", 999999999999999999L);
		dto.put("formId", FORM_ID);
		dto.put("title", "不存在的流程定义");

		Resp r = postJson("/instance/start", MAPPER.writeValueAsString(dto));
		assertFalse(r.ok(), "不存在的流程定义应报错，实际：" + r);
		// 后端对「解析不到流程定义」统一报错，文案含「流程定义」
		assertTrue(r.msg().contains("流程定义"), "错误应指向流程定义，实际：" + r.msg());
	}

	@Test
	@Order(6)
	void taohongInfo_withInvalidDocId_returnsEmpty() throws Exception {
		Resp r = get("/odoc/taohong-info?docId=999999999999999999");
		assertTrue(r.ok(), "未知 docId 的 taohong-info 应正常返回空态，实际：" + r);
		// 空态：docId 缺失，或被 blade nullToEmpty 序列化为 -1；都不得是真实正文ID
		JsonNode d = r.data();
		if (d != null) {
			long got = d.path("docId").asLong(0);
			assertTrue(got <= 0, "未知 docId 不应回带真实正文ID，实际：" + d);
		}
	}

	// ==================== 共享准备 ====================

	/** 建实例 → 建正文 → 上传正文版本 → 执行套红 → 下载合成产物（懒加载一次） */
	private static void setupTaohong() throws Exception {
		if (composedBytes != null) {
			return;
		}
		long doc = ensureDoc();

		// 上传正文（.docx）
		byte[] bodyDocx = buildDocx(BODY_P1, BODY_P2);
		Resp up = postFile("/odoc/file/upload", "正文.docx", bodyDocx);
		assertTrue(up.ok(), "上传正文应成功，实际：" + up);
		long fileId = Long.parseLong(up.data().path("id").asText());

		// 存为正文字段的一个版本
		Map<String, String> sv = new LinkedHashMap<>();
		sv.put("docId", String.valueOf(doc));
		sv.put("fileId", String.valueOf(fileId));
		sv.put("fileName", "正文.docx");
		sv.put("docFileType", "7");
		sv.put("versionDetail", "初始正文");
		Resp saved = postForm("/odoc/doc/version/save", sv);
		assertTrue(saved.ok(), "保存正文版本应成功，实际：" + saved);

		// 执行套红：fieldValues 显式传字段值，供书签→字段映射取用
		Map<String, Object> values = new LinkedHashMap<>();
		values.put("gwbt", TITLE);
		values.put("bt", NUMBER);
		values.put("fwlcbt", TITLE);
		values.put("wh1", NUMBER);

		Resp th = postJson("/odoc/taohong?docId=" + doc + "&mouldId=" + MOULD_ID,
			MAPPER.writeValueAsString(values));
		assertTrue(th.ok(), "套红应成功，实际：" + th);

		taohongData = th.data();
		composedVersionId = taohongData.path("id").asLong(0);
		assertTrue(composedVersionId > 0, "套红应返回新版本ID，实际：" + taohongData);
		composedBytes = getBytes("/odoc/doc/version/" + composedVersionId + "/content");
		assertTrue(composedBytes.length > 0, "应能下载到套红产物");
	}

	private static long ensureDoc() throws Exception {
		if (docId == null) {
			docId = initDoc(ensureInstance(), true);
		}
		return docId;
	}

	private static long ensureInstance() throws Exception {
		if (instanceId == null) {
			Map<String, Object> dto = new LinkedHashMap<>();
			dto.put("defId", DEF_ID);
			dto.put("formId", FORM_ID);
			dto.put("title", TITLE);
			Resp r = postJson("/instance/start", MAPPER.writeValueAsString(dto));
			assertTrue(r.ok(), "发起流程应成功，实际：" + r);
			instanceId = Long.parseLong(r.data().asText(""));
		}
		return instanceId;
	}

	private static long initDoc(long instId, boolean edit) throws Exception {
		Map<String, String> p = new LinkedHashMap<>();
		p.put("instanceId", String.valueOf(instId));
		p.put("defId", String.valueOf(DEF_ID));
		p.put("title", "正文");
		p.put("edit", String.valueOf(edit));
		Resp r = postForm("/odoc/doc/init", p);
		assertTrue(r.ok(), "初始化正文应成功，实际：" + r);
		return Long.parseLong(r.data().path("docId").asText(""));
	}

	// ==================== 文档内容工具 ====================

	/** 构造一个仅含若干段落的最小 docx（作为正文） */
	private static byte[] buildDocx(String... paragraphs) throws Exception {
		try (XWPFDocument doc = new XWPFDocument();
			 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			for (String t : paragraphs) {
				XWPFParagraph p = doc.createParagraph();
				p.createRun().setText(t);
			}
			doc.write(out);
			return out.toByteArray();
		}
	}

	private static List<String> paragraphTexts(byte[] docx) throws Exception {
		List<String> texts = new ArrayList<>();
		try (XWPFDocument d = new XWPFDocument(new ByteArrayInputStream(docx))) {
			for (XWPFParagraph p : d.getParagraphs()) {
				texts.add(p.getText() == null ? "" : p.getText());
			}
		}
		return texts;
	}

	/**
	 * 套红模板中被保留的<b>静态（非书签）</b>文本：红头与版式内容。
	 * 书签段落会被替换（fld_title/fld_number）或移除（Body 占位），故排除。
	 */
	private static List<String> templateStaticTexts() throws Exception {
		Resp mould = get("/odoc/mould/" + MOULD_ID);
		long tplFileId = mould.data().path("fileId").asLong(0);
		assertTrue(tplFileId > 0, "应能取到模板文件ID，实际：" + mould.data());

		byte[] tpl = getBytes("/odoc/file/" + tplFileId + "/content");
		List<String> statics = new ArrayList<>();
		try (XWPFDocument d = new XWPFDocument(new ByteArrayInputStream(tpl))) {
			for (XWPFParagraph p : d.getParagraphs()) {
				if (bookmarkNames(p.getCTP().getDomNode()).isEmpty()) {
					String t = p.getText();
					if (t != null && !t.isBlank()) {
						statics.add(t);
					}
				}
			}
		}
		return statics;
	}

	/** 取段落（w:p）DOM 下的书签名（w:bookmarkStart/@w:name） */
	private static List<String> bookmarkNames(Node paragraph) {
		List<String> names = new ArrayList<>();
		if (paragraph == null) {
			return names;
		}
		NodeList children = paragraph.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE || !"bookmarkStart".equals(n.getLocalName())) {
				continue;
			}
			Element el = (Element) n;
			String nm = el.getAttributeNS(
				"http://schemas.openxmlformats.org/wordprocessingml/2006/main", "name");
			if (nm == null || nm.isBlank()) {
				nm = el.getAttribute("w:name");
			}
			if (nm != null && !nm.isBlank()) {
				names.add(nm);
			}
		}
		return names;
	}

	private static int indexOfContains(List<String> texts, String sub) {
		for (int i = 0; i < texts.size(); i++) {
			if (texts.get(i) != null && texts.get(i).contains(sub)) {
				return i;
			}
		}
		return -1;
	}

	// ==================== HTTP 工具 ====================

	private static Resp get(String path) throws Exception {
		return send(req(path).GET().build());
	}

	private static byte[] getBytes(String path) throws Exception {
		HttpResponse<byte[]> r = http.send(req(path).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
		return r.body();
	}

	private static Resp postJson(String path, String json) throws Exception {
		return send(req(path)
			.header("Content-Type", "application/json; charset=UTF-8")
			.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
			.build());
	}

	private static Resp postForm(String path, Map<String, String> params) throws Exception {
		String body = params.entrySet().stream()
			.map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
			.collect(Collectors.joining("&"));
		return send(req(path)
			.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
			.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
			.build());
	}

	/** multipart/form-data 上传，表单字段名固定为 file（对齐 @RequestParam("file")） */
	private static Resp postFile(String path, String fileName, byte[] content) throws Exception {
		String boundary = "----taohongTest" + System.nanoTime();
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		bao.write(("--" + boundary + "\r\n"
			+ "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
			+ "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
		bao.write(content);
		bao.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

		return send(req(path)
			.header("Content-Type", "multipart/form-data; boundary=" + boundary)
			.POST(HttpRequest.BodyPublishers.ofByteArray(bao.toByteArray()))
			.build());
	}

	private static HttpRequest.Builder req(String path) {
		return HttpRequest.newBuilder(URI.create(baseUrl + PREFIX + path))
			.header("Tenant-Id", "000000")
			.header("Authorization", "Basic c3dvcmQ6c3dvcmRfc2VjcmV0")
			.header("blade-auth", "bearer " + token)
			.timeout(java.time.Duration.ofSeconds(60));
	}

	private static Resp send(HttpRequest request) throws Exception {
		HttpResponse<byte[]> r = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
		return new Resp(r.statusCode(), r.body());
	}

	private static String enc(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	private static long uniqueInstanceId() {
		return 900000000000000000L + (System.nanoTime() % 1000000L);
	}

	private static String sysProp(String key) {
		return System.getProperty(key);
	}

	private static String firstNonBlank(String... candidates) {
		if (candidates == null) {
			return null;
		}
		for (String c : candidates) {
			if (c != null && !c.isBlank()) {
				return c;
			}
		}
		return null;
	}

	/** 统一响应：{code, success, msg, data} */
	static final class Resp {
		final int status;
		final JsonNode root;

		Resp(int status, byte[] body) {
			this.status = status;
			JsonNode parsed = null;
			try {
				parsed = MAPPER.readTree(body == null ? new byte[0] : body);
			} catch (IOException ignored) {
				// 非 JSON（如网关直接返回的 HTML 错误页）视作空响应
			}
			this.root = parsed == null || parsed.isMissingNode() ? MAPPER.createObjectNode() : parsed;
		}

		int code() {
			return root.path("code").asInt(-1);
		}

		String msg() {
			return root.path("msg").asText("");
		}

		boolean ok() {
			return code() == 200;
		}

		JsonNode data() {
			JsonNode d = root.get("data");
			return (d == null || d.isNull()) ? null : d;
		}

		@Override
		public String toString() {
			return "status=" + status + ", code=" + code() + ", msg=" + msg();
		}
	}
}
