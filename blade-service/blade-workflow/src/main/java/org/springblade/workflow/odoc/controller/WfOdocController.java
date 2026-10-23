package org.springblade.workflow.odoc.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springblade.core.log.exception.ServiceException;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.odoc.entity.WfOdocDoc;
import org.springblade.workflow.odoc.entity.WfOdocDocVersion;
import org.springblade.workflow.odoc.entity.WfOdocMould;
import org.springblade.workflow.odoc.entity.WfOdocMouldBookmark;
import org.springblade.workflow.odoc.entity.WfOdocLayoutFile;
import org.springblade.workflow.odoc.entity.WfOdocMouldFieldMap;
import org.springblade.workflow.odoc.entity.WfOdocNumber;
import org.springblade.workflow.odoc.entity.WfOdocNumberReserved;
import org.springblade.workflow.odoc.entity.WfOdocProcessSet;
import org.springblade.workflow.odoc.entity.WfOdocSeal;
import org.springblade.workflow.odoc.gbt.Gbt9704Profile;
import org.springblade.workflow.odoc.service.IWfOdocAiService;
import org.springblade.workflow.odoc.service.IWfOdocDocService;
import org.springblade.workflow.odoc.service.IWfOdocFontService;
import org.springblade.workflow.odoc.service.IWfOdocLayoutService;
import org.springblade.workflow.odoc.service.IWfOdocMouldService;
import org.springblade.workflow.odoc.service.IWfOdocNumberReservedService;
import org.springblade.workflow.odoc.service.IWfOdocNumberService;
import org.springblade.workflow.odoc.service.IWfOdocProcessSetService;
import org.springblade.workflow.odoc.service.IWfOdocSealService;
import org.springblade.workflow.odoc.service.IWfOdocTaohongService;
import org.springblade.workflow.odoc.OdocUrlSigner;
import org.springblade.workflow.odoc.store.IOdocFileStore;
import org.springblade.workflow.spi.OdocSealRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springblade.workflow.odoc.vo.OdocLayoutRuleVO;
import org.springblade.workflow.odoc.vo.OdocFontStatusVO;
import org.springblade.workflow.odoc.vo.OdocSampleVO;
import org.springblade.workflow.odoc.vo.OdocSensitiveHitVO;
import org.springblade.workflow.odoc.vo.OdocDocInitVO;
import org.springblade.workflow.odoc.vo.OdocTaohongInfoVO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 公文正文控制器。
 *
 * <p>普通接口要求「已登录」({@link WorkflowConstant#HAS_AUTH})；但 {@code /content} 与
 * {@code /callback} 两个端点是 OnlyOffice 服务器直连调用（不带 Blade-Auth 头），
 * 方法级用 {@code permitAll()} 放行——正文涉密场景请改用 URL 签名 + 内网隔离加固。</p>
 */
@Slf4j
@RestController
@RequestMapping("/odoc")
@RequiredArgsConstructor
@Tag(name = "公文正文", description = "正文文档/版本/在线编辑")
public class WfOdocController {

	private final IWfOdocDocService docService;
	private final IWfOdocMouldService mouldService;
	private final IWfOdocTaohongService taohongService;
	private final IWfOdocProcessSetService processSetService;
	private final IWfOdocNumberService numberService;
	private final IWfOdocNumberReservedService reservedService;
	private final IWfOdocAiService aiService;
	private final IWfOdocLayoutService layoutService;
	private final IWfOdocSealService sealService;
	private final IWfOdocFontService fontService;
	private final IOdocFileStore fileStore;
	private final OdocUrlSigner signer;
	private final ObjectMapper objectMapper = new ObjectMapper();

	/** 公文密级字段名（表单字段）。可在 Nacos 配 blade.workflow.odoc.secret-level-field 覆盖，缺省 mj */
	@Value("${blade.workflow.odoc.secret-level-field:mj}")
	private String secretLevelFieldName;

	@PostMapping("/doc/init")
	@Operation(summary = "获取或创建正文", description = "按流程实例返回文档+版本+在线编辑配置")
	public R<OdocDocInitVO> init(@RequestParam Long instanceId,
								 @RequestParam(required = false) Long defId,
								 @RequestParam(required = false) String title,
								 @RequestParam(defaultValue = "false") boolean edit) {
		return R.data(docService.initDoc(instanceId, defId, title, edit));
	}

	@GetMapping("/doc/{docId}")
	@Operation(summary = "正文主记录")
	public R<WfOdocDoc> getDoc(@PathVariable Long docId) {
		return R.data(docService.getDoc(docId));
	}

	@GetMapping("/doc/{docId}/versions")
	@Operation(summary = "版本历史")
	public R<List<WfOdocDocVersion>> versions(@PathVariable Long docId) {
		return R.data(docService.listVersions(docId));
	}

	@GetMapping("/doc/picker")
	@Operation(summary = "公文文档选择器",
		description = "表单「相关文档」浏览按钮(24/26)的数据源：按正文标题关键字检索公文正文文档分页"
			+ "（排除测试数据；可传 excludeInstanceId 排除当前实例自身的文档，避免自关联）")
	public R<IPage<WfOdocDoc>> docPicker(
		@Parameter(description = "当前页，从 1 开始") @RequestParam(value = "current", required = false) Long current,
		@Parameter(description = "每页条数，默认 20") @RequestParam(value = "pageSize", required = false) Long pageSize,
		@Parameter(description = "正文标题关键字，模糊匹配") @RequestParam(value = "keyword", required = false) String keyword,
		@Parameter(description = "排除的所属实例ID（通常传当前实例ID）") @RequestParam(value = "excludeInstanceId", required = false) Long excludeInstanceId) {
		return R.data(docService.picker(current, pageSize, keyword, excludeInstanceId));
	}

	@PostMapping("/doc/version/save")
	@Operation(summary = "手动落库新版本", description = "前端先上传到文件服务取得 fileId，再调用本接口登记版本")
	public R<WfOdocDocVersion> saveVersion(@RequestParam Long docId,
										  @RequestParam Long fileId,
										  @RequestParam String fileName,
										  @RequestParam Integer docFileType,
										  @RequestParam(required = false) String versionDetail) {
		return R.data(docService.saveVersion(docId, fileId, fileName, docFileType, versionDetail));
	}

	@PostMapping("/doc/version/rollback")
	@Operation(summary = "回滚到指定版本")
	public R<WfOdocDocVersion> rollback(@RequestParam Long docId,
										@RequestParam Long toVersionId) {
		return R.data(docService.rollback(docId, toVersionId));
	}

	// ===================== 文号 =====================

	@PostMapping("/number/assign")
	@Operation(summary = "取号", description = "按流程定义解析规则，行锁自增 serial；定稿节点由出口监听自动调用")
	public R<String> assignNumber(@RequestParam Long defId) {
		return R.data(numberService.assign(defId));
	}

	@GetMapping("/number/preview")
	@Operation(summary = "预览下一文号", description = "不自增")
	public R<String> previewNumber(@RequestParam Long defId) {
		return R.data(numberService.preview(defId));
	}

	@GetMapping("/number/rules")
	@Operation(summary = "文号规则列表", description = "defId 命中或全局")
	public R<List<WfOdocNumber>> listNumberRules(@RequestParam(required = false) Long defId) {
		return R.data(numberService.listRules(defId));
	}

	@PostMapping("/number/rule")
	@Operation(summary = "新建文号规则")
	public R<WfOdocNumber> createNumberRule(@RequestBody WfOdocNumber rule) {
		return R.data(numberService.createRule(rule));
	}

	@PutMapping("/number/rule")
	@Operation(summary = "更新文号规则")
	public R<WfOdocNumber> updateNumberRule(@RequestBody WfOdocNumber rule) {
		return R.data(numberService.updateRule(rule));
	}

	@DeleteMapping("/number/rule/{id}")
	@Operation(summary = "删除文号规则")
	public R<Boolean> removeNumberRule(@PathVariable Long id) {
		numberService.removeRule(id);
		return R.data(true);
	}

	// ===================== 文号保留号 =====================

	@GetMapping("/number-reserved/list")
	@Operation(summary = "保留号列表", description = "按文号规则列出已保留流水")
	public R<List<WfOdocNumberReserved>> listReserved(@RequestParam Long numberId) {
		return R.data(reservedService.listByNumberId(numberId));
	}

	@PostMapping("/number-reserved")
	@Operation(summary = "保留号段", description = "按 [startSerial,endSerial] 批量保留流水，自动跳过已存在的；expireTime 为空=永久保留")
	public R<Integer> reserveNumber(@RequestParam Long numberId,
								   @RequestParam Integer startSerial,
								   @RequestParam Integer endSerial,
								   @RequestParam(required = false) String expireTime) {
		LocalDateTime exp = null;
		if (expireTime != null && !expireTime.isBlank()) {
			exp = LocalDateTime.parse(expireTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		}
		return R.data(reservedService.reserveRange(numberId, startSerial, endSerial, exp));
	}

	@DeleteMapping("/number-reserved/{id}")
	@Operation(summary = "释放保留号")
	public R<Boolean> releaseReserved(@PathVariable Long id) {
		return R.data(reservedService.removeReserved(id));
	}

	// ===================== 文件上传/下载（本地存储） =====================

	@PostMapping("/file/upload")
	@Operation(summary = "上传 odoc 文件", description = "本地存储到 odoc.upload-path，返回 fileId")
	public R<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ServiceException("文件不能为空");
		}
		try {
			Long fileId = fileStore.store(file.getBytes(), file.getOriginalFilename());
			Map<String, Object> d = new HashMap<>();
			d.put("id", fileId);
			d.put("fileName", file.getOriginalFilename());
			return R.data(d);
		} catch (java.io.IOException e) {
			throw new ServiceException("读取上传文件失败: " + e.getMessage());
		}
	}

	@PreAuth("permitAll()")
	@GetMapping("/file/{fileId}/content")
	@Operation(summary = "按 fileId 取文件内容", description = "签名 token 或已登录会话任一通过（闭合 DOC-P0-4 可枚举下载）")
	public ResponseEntity<byte[]> fileContent(@PathVariable Long fileId,
											  @RequestParam(required = false) String token) {
		if (!authorized(String.valueOf(fileId), token)) {
			throw new ServiceException("无权限访问该文件内容");
		}
		byte[] bytes = fileStore.download(fileId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		String name = fileStore.resolveFileName(fileId);
		if (name.isBlank()) {
			name = "odoc-" + fileId + ".bin";
		}
		headers.setContentDispositionFormData("attachment", name);
		return ResponseEntity.ok().headers(headers).body(bytes);
	}

	// ===================== 信创适配：国标字体自检 =====================
	@GetMapping("/font-check")
	@Operation(summary = "国标字体自检", description = "检测 GB/T 9704 必需/常用字体是否安装（信创最大坑）")
	public R<List<OdocFontStatusVO>> fontCheck() {
		return R.data(fontService.checkFonts());
	}

	// ===================== 版式文件（L2，经 SPI；未装配则 available=false） =====================

	@GetMapping("/layout/available")
	@Operation(summary = "版式转换是否可用", description = "前端据此显示/隐藏「转版式」入口")
	public R<Boolean> layoutAvailable() {
		return R.data(layoutService.layoutAvailable());
	}

	@PostMapping("/layout/to")
	@Operation(summary = "转版式", description = "docx 版本 → PDF/OFD，落库版式文件")
	public R<WfOdocLayoutFile> toLayout(@RequestParam Long docId,
										@RequestParam Long srcVersionId,
										@RequestParam Integer format) {
		return R.data(layoutService.toLayout(docId, srcVersionId, format));
	}

	@GetMapping("/layout/list")
	@Operation(summary = "版式文件列表")
	public R<List<WfOdocLayoutFile>> listLayout(@RequestParam Long docId) {
		return R.data(layoutService.list(docId));
	}

	@PreAuth("permitAll()")
	@GetMapping("/layout/{layoutFileId}/content")
	@Operation(summary = "版式文件内容", description = "签名 token 或已登录会话任一通过（闭合 DOC-P0-4 可枚举下载）")
	public ResponseEntity<byte[]> layoutContent(@PathVariable Long layoutFileId,
												@RequestParam(required = false) String token) {
		if (!authorized(String.valueOf(layoutFileId), token)) {
			throw new ServiceException("无权限访问该版式文件内容");
		}
		byte[] bytes = layoutService.content(layoutFileId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setContentDispositionFormData("attachment", layoutService.contentName(layoutFileId));
		return ResponseEntity.ok().headers(headers).body(bytes);
	}

	// ===================== 电子签章（L2，经 SPI；未装配则 available=false） =====================

	@GetMapping("/seal/available")
	@Operation(summary = "电子签章是否可用")
	public R<Boolean> sealAvailable() {
		return R.data(sealService.sealAvailable());
	}

	@PostMapping("/seal/sign")
	@Operation(summary = "电子签章", description = "对版式文件签章 + RFC3161 时间戳，落库签章记录")
	public R<WfOdocSeal> sealSign(@RequestParam Long docId,
								  @RequestParam Long layoutFileId,
								  @RequestBody OdocSealRequest request) {
		return R.data(sealService.sign(docId, layoutFileId, request));
	}

	@PostMapping("/seal/verify/{sealId}")
	@Operation(summary = "验章 + 篡改检测")
	public R<WfOdocSeal> sealVerify(@PathVariable Long sealId) {
		return R.data(sealService.verify(sealId));
	}

	@GetMapping("/seal/list")
	@Operation(summary = "签章记录列表")
	public R<List<WfOdocSeal>> listSeal(@RequestParam Long docId) {
		return R.data(sealService.list(docId));
	}

	// ===================== AI 增强（L1：规则库/内置库，无外部模型） =====================

	@GetMapping("/ai/layout")
	@Operation(summary = "智能排版：按文种取 GB/T 9704 版式")
	public R<Gbt9704Profile> aiLayout(@RequestParam(required = false) String docTypeCode) {
		return R.data(aiService.suggestLayout(docTypeCode));
	}

	@GetMapping("/ai/layout-rules")
	@Operation(summary = "智能排版：版式规则展平（前端展示排版建议）")
	public R<List<OdocLayoutRuleVO>> aiLayoutRules(@RequestParam(required = false) String docTypeCode) {
		return R.data(aiService.layoutRules(docTypeCode));
	}

	@GetMapping("/ai/samples")
	@Operation(summary = "范文：按文种取公文结构示例")
	public R<List<OdocSampleVO>> aiSamples(@RequestParam(required = false) String docTypeCode) {
		return R.data(aiService.samples(docTypeCode));
	}

	@PostMapping("/ai/sensitive-check")
	@Operation(summary = "敏感词扫描")
	public R<List<OdocSensitiveHitVO>> aiSensitive(@RequestParam("text") String text) {
		return R.data(aiService.checkSensitive(text));
	}

	// ===================== 流程建文设置 =====================

	@GetMapping("/process-set")
	@Operation(summary = "流程建文设置", description = "按流程定义取；无则返回空")
	public R<WfOdocProcessSet> getProcessSet(@RequestParam Long defId) {
		return R.data(processSetService.getByDefId(defId));
	}

	@PostMapping("/process-set")
	@Operation(summary = "保存流程建文设置", description = "按 defId 幂等 upsert")
	public R<WfOdocProcessSet> saveProcessSet(@RequestBody WfOdocProcessSet set) {
		return R.data(processSetService.save(set));
	}

	// ===================== 套红模板 =====================

	@PostMapping("/mould/create")
	@Operation(summary = "新建套红/编辑模板", description = "fileId 为已上传到文件服务的 .docx")
	public R<WfOdocMould> createMould(@RequestParam String mouldName,
									  @RequestParam Integer mouldType,
									  @RequestParam Long fileId,
									  @RequestParam(required = false) Long defId,
									  @RequestParam(required = false) String docTypeCode) {
		return R.data(mouldService.createMould(mouldName, mouldType, fileId, defId, docTypeCode));
	}

	@GetMapping("/mould/list")
	@Operation(summary = "模板列表", description = "defId 命中或全局；mouldType 1编辑 2套红 3打印 4版式")
	public R<List<WfOdocMould>> listMould(@RequestParam(required = false) Long defId,
										  @RequestParam(required = false) Integer mouldType) {
		return R.data(mouldService.listMoulds(defId, mouldType));
	}

	@GetMapping("/mould/{mouldId}")
	@Operation(summary = "模板详情")
	public R<WfOdocMould> getMould(@PathVariable Long mouldId) {
		return R.data(mouldService.getMould(mouldId));
	}

	@DeleteMapping("/mould/{mouldId}")
	@Operation(summary = "删除模板（级联书签/字段映射）")
	public R<Boolean> removeMould(@PathVariable Long mouldId) {
		mouldService.removeMould(mouldId);
		return R.data(true);
	}

	@PostMapping("/mould/{mouldId}/extract-bookmarks")
	@Operation(summary = "扫描模板书签", description = "解析 docx 书签并覆盖式重存")
	public R<List<WfOdocMouldBookmark>> extractBookmarks(@PathVariable Long mouldId) {
		return R.data(mouldService.extractBookmarks(mouldId));
	}

	@GetMapping("/mould/{mouldId}/bookmarks")
	@Operation(summary = "模板书签列表")
	public R<List<WfOdocMouldBookmark>> listBookmarks(@PathVariable Long mouldId) {
		return R.data(mouldService.listBookmarks(mouldId));
	}

	@PutMapping("/mould/{mouldId}/bookmarks")
	@Operation(summary = "更新书签属性", description = "isContent / descript / elementType，按 id 逐条更新")
	public R<Boolean> updateBookmarks(@PathVariable Long mouldId,
									  @RequestBody(required = false) List<WfOdocMouldBookmark> bookmarks) {
		mouldService.updateBookmarks(mouldId, bookmarks);
		return R.data(true);
	}

	@GetMapping("/mould/{mouldId}/field-maps")
	@Operation(summary = "模板字段映射（编辑回显）")
	public R<List<WfOdocMouldFieldMap>> listFieldMaps(@PathVariable Long mouldId) {
		return R.data(mouldService.listFieldMaps(mouldId));
	}

	@PostMapping("/mould/{mouldId}/field-maps")
	@Operation(summary = "保存书签→表单字段映射", description = "覆盖式保存")
	public R<Boolean> saveFieldMaps(@PathVariable Long mouldId,
									@RequestParam(required = false) Long defId,
									@RequestBody(required = false) List<WfOdocMouldFieldMap> maps) {
		mouldService.saveFieldMaps(mouldId, defId, maps);
		return R.data(true);
	}

	// ===================== 套红 =====================

	@PostMapping("/taohong")
	@Operation(summary = "执行套红", description = "用套红模板承载正文，生成套红版新版本")
	public R<WfOdocDocVersion> taohong(@RequestParam Long docId,
									   @RequestParam Long mouldId,
									   @RequestBody(required = false) Map<String, Object> fieldValues) {
		return R.data(taohongService.taohong(docId, mouldId, fieldValues));
	}

	@PostMapping("/cancel-taohong")
	@Operation(summary = "取消套红", description = "回退到最近一个未套红版本")
	public R<WfOdocDocVersion> cancelTaohong(@RequestParam Long docId) {
		return R.data(taohongService.cancelTaohong(docId));
	}

	@GetMapping("/taohong-info")
	@Operation(summary = "套红状态", description = "是否已套红 + 当前版本 + 可用模板 + 是否强制套红")
	public R<OdocTaohongInfoVO> taohongInfo(@RequestParam Long docId) {
		return R.data(taohongService.taohongInfo(docId));
	}

	/**
	 * 正文内容代理。OnlyOffice 服务器拉取文档与此端点，故放行 permitAll。
	 * 生产环境应替换为带签名的一次性 URL + 内网隔离。
	 */
	@PreAuth("permitAll()")
	@GetMapping("/doc/version/{versionId}/content")
	@Operation(summary = "版本内容（字节代理）", description = "OnlyOffice document.url 与前端均走签名 token（闭合 DOC-P0-4）")
	public ResponseEntity<byte[]> content(@PathVariable Long versionId,
										 @RequestParam(required = false) String token) {
		if (!authorized(String.valueOf(versionId), token)) {
			throw new ServiceException("无权限访问该版本内容");
		}
		byte[] bytes = docService.content(versionId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		String name = docService.contentFileName(versionId);
		if (name.isBlank()) {
			name = "odoc-" + versionId + ".bin";
		}
		headers.setContentDispositionFormData("attachment", name);
		return ResponseEntity.ok().headers(headers).body(bytes);
	}

	/** 签名 token 或已登录会话任一通过；闭合 DOC-P0-4 可枚举下载 */
	private boolean authorized(String resource, String token) {
		return signer.verify(resource, token) || SecureUtil.getUser() != null;
	}

	/**
	 * 签发内容/回调签名 URL：浏览器/前端先经此（已登录）换取带 token 的下载地址。
	 * 注意：当前仅做「已登录 + 签名」，细粒度归属校验（canDownload）为后续加固项。
	 */
	@GetMapping("/sign-url")
	@PreAuth(WorkflowConstant.HAS_AUTH)
	@Operation(summary = "签发内容/回调签名 URL", description = "闭合 DOC-P0-4：下载须持签名 token 或已登录会话")
	public R<String> signUrl(@RequestParam String type,
							 @RequestParam(required = false) Long id,
							 @RequestParam(required = false) Long docId,
							 @RequestParam(required = false) Long versionId) {
		String path;
		String resource;
		switch (type) {
			case "file":
				path = "/odoc/file/" + id + "/content";
				resource = String.valueOf(id);
				break;
			case "layout":
				path = "/odoc/layout/" + id + "/content";
				resource = String.valueOf(id);
				break;
			case "version":
				path = "/odoc/doc/version/" + id + "/content";
				resource = String.valueOf(id);
				break;
			case "callback":
				path = "/odoc/doc/callback?docId=" + docId + "&versionId=" + versionId;
				resource = docId + ":" + versionId;
				break;
			default:
				throw new ServiceException("未知签名类型: " + type);
		}
		return R.data(path + "?token=" + signer.sign(resource));
	}

	/**
	 * OnlyOffice 保存回调。服务器直连、不带鉴权头，放行 permitAll。
	 * OnlyOffice 要求返回 {"error":0}。
	 */
	@PreAuth("permitAll()")
	@PostMapping("/doc/callback")
	@Operation(summary = "OnlyOffice 保存回调", description = "OnlyOffice 编辑保存后回写新版本；校验回调签名 token")
	public Map<String, Object> callback(@RequestParam Long docId,
										@RequestParam Long versionId,
										@RequestParam(required = false) String token,
										@RequestBody(required = false) String body) {
		if (!signer.verify(docId + ":" + versionId, token)) {
			java.util.Map<String, Object> fail = new java.util.HashMap<>();
			fail.put("error", 1);
			return fail;
		}
		try {
			int status = 0;
			String url = null;
			if (body != null && !body.isBlank()) {
				JsonNode node = objectMapper.readTree(body);
				if (node.has("status")) {
					status = node.get("status").asInt();
				}
				if (node.has("url")) {
					url = node.get("url").asText();
				}
			}
			docService.handleCallback(docId, versionId, status, url);
		} catch (Exception e) {
			log.error("OnlyOffice 回调处理失败 docId={} versionId={}", docId, versionId, e);
			return Map.of("error", 1);
		}
		return Map.of("error", 0);
	}

	// ===================== 前端全局可配置项 =====================

	@GetMapping("/config")
	@Operation(summary = "公文前端全局配置", description = "返回前端可覆盖项（如密级字段名 secretLevelFieldName，缺省 mj）。" +
		"管理员可在 Nacos 配 blade.workflow.odoc.secret-level-field 覆盖，无需改前端代码")
	public R<Map<String, String>> config() {
		Map<String, String> m = new HashMap<>(2);
		m.put("secretLevelFieldName", secretLevelFieldName);
		return R.data(m);
	}
}
