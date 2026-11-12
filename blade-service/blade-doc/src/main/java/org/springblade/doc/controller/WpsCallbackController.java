package org.springblade.doc.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springblade.common.wps.WpsCallbackSupport;
import org.springblade.common.wps.WpsFileId;
import org.springblade.common.wps.WpsFileMeta;
import org.springblade.common.wps.WpsFileProvider;
import org.springblade.common.wps.WpsProperties;
import org.springblade.common.wps.WpsUrlSigner;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.doc.entity.DocImageFile;
import org.springblade.doc.mapper.DocImageFileMapper;
import org.springblade.doc.sign.DocUrlSigner;
import org.springblade.doc.store.IDocFileStore;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * WPS WebOffice 服务端回调（solution.wps.cn 官方 v3 协议）— 协议编排层。
 *
 * <p>路径体系 {@code /v3/3rd/files/{fileId}[/download|/permission|/upload...]}，
 * 由 WPS 中台按控制台登记的回调地址调用。响应遵循 WPS 协议：
 * 成功 {@code {"code":0,"data":{...},"message":""}}，失败 {@code {"code":xxx,"message":...}}，
 * 故不使用 BladeX 的 R 包装。</p>
 *
 * <p><b>WPS 统一改造 P1-1.5</b>：本类不再访问任何业务表，只做
 * ① {@link WpsFileId} 解析与按域路由 ② token 校验 ③ 协议响应包装。
 * 与 {@code DocImageFile}/{@code DocDetail} 耦合的解析、版本链、保存落库已迁入
 * {@link org.springblade.doc.online.DocWpsProvider}；收发文在 P2 提供
 * {@code domain()=odoc} 的实现后，同一入口自动复用整套 v3 协议。</p>
 *
 * <p><b>P3.3 安全加固</b>：{@link #authGuard} 对带 fileId 的端点做
 * {@code WpsCallbackSupport.tokenValid} 硬校验；{@link #auxGuard} 对
 * {@code /users}、{@code /object/**} 默认做形状 + 时效校验，开启
 * {@code blade.wps.callback-auth-bind-resource} 后按回调头 fileId 做完整 HMAC 验签。
 * 由 {@code blade.wps.callback-auth-strict} 控制（默认 true；联调排障可临时设 false，
 * 但此时<b>不得</b>同时打开网关 {@code /v3/3rd/**} 白名单，否则等于完全裸奔）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/v3/3rd")
@Tag(name = "内容引擎-WPS WebOffice回调", description = "/v3/3rd/files/* 预览/在线编辑回调，X-WebOffice-Token 鉴权")
public class WpsCallbackController {

	private final Map<String, WpsFileProvider> providers;
	private final DocUrlSigner docSigner;
	private final WpsUrlSigner signer;
	private final DocImageFileMapper imageFileMapper;
	private final IDocFileStore fileStore;
	private final JdbcTemplate jdbcTemplate;
	private final WpsProperties wpsProperties;

	public WpsCallbackController(List<WpsFileProvider> providerList,
								 DocUrlSigner docSigner, WpsUrlSigner signer,
								 DocImageFileMapper imageFileMapper,
								 IDocFileStore fileStore, JdbcTemplate jdbcTemplate,
								 WpsProperties wpsProperties) {
		this.providers = providerList.stream().collect(
			Collectors.toMap(WpsFileProvider::domain, Function.identity(), (a, b) -> a, HashMap::new));
		this.docSigner = docSigner;
		this.signer = signer;
		this.imageFileMapper = imageFileMapper;
		this.fileStore = fileStore;
		this.jdbcTemplate = jdbcTemplate;
		this.wpsProperties = wpsProperties;
	}

	// ---------------- 路由 / 鉴权 ----------------

	/** fileId → 业务域 Provider；非法 fileId 或尚未接入的域返回 null */
	private WpsFileProvider providerOf(String fileId) {
		WpsFileId fid = WpsFileId.parse(fileId);
		return fid == null ? null : providers.get(fid.domain());
	}

	private String bizOf(String fileId) {
		WpsFileId fid = WpsFileId.parse(fileId);
		return fid == null ? null : fid.bizId();
	}

	/**
	 * 带 fileId 端点的鉴权（P3.3）。返回拒绝响应，null 表示放行。
	 *
	 * <p>resource 固定 {@code wps:{wireFileId}}，与前端 init 时下发的 token 同源。
	 * 非严格模式（联调排障）仅告警放行；严格模式直接返回 WPS 协议错误码，
	 * 且拒绝发生在 Provider 路由之前，防止越权触达业务存储。</p>
	 */
	private ResponseEntity<Map<String, Object>> authGuard(HttpServletRequest req, String wireFileId) {
		if (WpsCallbackSupport.tokenValid(signer, wireFileId, req.getHeader(WpsCallbackSupport.HEADER_TOKEN))) {
			return null;
		}
		log.warn("WPS 回调 token 校验未通过 fileId={}, hasToken={}, strict={}", wireFileId,
			req.getHeader(WpsCallbackSupport.HEADER_TOKEN) != null, wpsProperties.isCallbackAuthStrict());
		return wpsProperties.isCallbackAuthStrict()
			? err(WpsCallbackSupport.CODE_AUTH_FAILED, "unauthorized callback")
			: null;
	}

	/**
	 * 辅助端点（{@code /users}、{@code /object/**}）的鉴权：路径无 fileId。
	 *
	 * <p>默认（{@code callback-auth-bind-resource=false}）走形状 + 时效校验，挡住无 token
	 * 裸奔与随手伪造；开启绑定后从回调头/query 取 fileId，按 {@code wps:{fileId}} 做完整
	 * HMAC 验签，取不到 fileId 一律拒绝（避免留旁路）。严格模式下失败即返回 40101。</p>
	 */
	private ResponseEntity<Map<String, Object>> auxGuard(HttpServletRequest req) {
		String token = req.getHeader(WpsCallbackSupport.HEADER_TOKEN);
		boolean bind = wpsProperties.isCallbackAuthBindResource();
		String auxFileId = bind ? auxFileId(req) : null;
		boolean ok = bind
			? auxFileId != null && WpsCallbackSupport.tokenValid(signer, auxFileId, token)
			: WpsCallbackSupport.tokenShapeValid(token);
		if (ok) {
			return null;
		}
		log.warn("WPS 辅助端点 token 校验未通过 hasToken={}, bindResource={}, hasFileId={}, strict={}",
			token != null, bind, auxFileId != null, wpsProperties.isCallbackAuthStrict());
		return wpsProperties.isCallbackAuthStrict()
			? err(WpsCallbackSupport.CODE_AUTH_FAILED, "unauthorized callback")
			: null;
	}

	/** 辅助端点的 fileId：回调头优先，query file_id 次之 */
	private String auxFileId(HttpServletRequest req) {
		String v = req.getHeader(WpsCallbackSupport.HEADER_FILE_ID);
		if (v == null || v.isBlank()) {
			v = req.getParameter("file_id");
		}
		return (v == null || v.isBlank()) ? null : v.trim();
	}

	/** 把 Provider 返回的（可能相对）URL 补成 WPS 中台可达的绝对地址 */
	private String absolute(HttpServletRequest req, WpsFileProvider provider, String url) {
		if (url == null) {
			return null;
		}
		if (url.startsWith("http://") || url.startsWith("https://")) {
			return url;
		}
		String base = WpsCallbackSupport.publicBase(
			provider.publicBase(), req.getScheme(), req.getServerName(), req.getServerPort());
		return base + (url.startsWith("/") ? "" : "/") + url;
	}

	private Map<String, Object> versionData(String wireFileId, WpsFileMeta meta) {
		if (meta == null) {
			return null;
		}
		meta.setId(wireFileId);
		return WpsCallbackSupport.versionItem(meta);
	}

	// ---------------- 预览 ----------------

	@PreAuth("permitAll()")
	@GetMapping("/files/{fileId}")
	@Operation(summary = "WPS-文件元信息")
	public ResponseEntity<Map<String, Object>> info(HttpServletRequest req, @PathVariable String fileId) {
		log.info("WPS info 到站 fileId={}, hasToken={}, ua={}", fileId,
			req.getHeader(WpsCallbackSupport.HEADER_TOKEN) != null, req.getHeader("User-Agent"));
		ResponseEntity<Map<String, Object>> denied = authGuard(req, fileId);
		if (denied != null) {
			return denied;
		}
		WpsFileProvider p = providerOf(fileId);
		if (p == null) {
			return err(WpsCallbackSupport.CODE_FILE_INVALID, "file_id invalid");
		}
		Map<String, Object> data = versionData(fileId, p.info(bizOf(fileId)));
		return data == null ? err(WpsCallbackSupport.CODE_FILE_INVALID, "file_id invalid") : json(data);
	}

	@PreAuth("permitAll()")
	@GetMapping("/files/{fileId}/download")
	@Operation(summary = "WPS-文件下载地址")
	public ResponseEntity<Map<String, Object>> download(HttpServletRequest req, @PathVariable String fileId) {
		ResponseEntity<Map<String, Object>> denied = authGuard(req, fileId);
		if (denied != null) {
			return denied;
		}
		WpsFileProvider p = providerOf(fileId);
		if (p == null) {
			return err(WpsCallbackSupport.CODE_FILE_INVALID, "file_id invalid");
		}
		String url = absolute(req, p, p.downloadUrl(bizOf(fileId)));
		return url == null ? err(WpsCallbackSupport.CODE_FILE_INVALID, "file_id invalid")
			: json(new LinkedHashMap<>(Map.of("url", url)));
	}

	@PreAuth("permitAll()")
	@GetMapping("/files/{fileId}/permission")
	@Operation(summary = "WPS-文档用户权限")
	public ResponseEntity<Map<String, Object>> permission(HttpServletRequest req, @PathVariable String fileId) {
		ResponseEntity<Map<String, Object>> denied = authGuard(req, fileId);
		if (denied != null) {
			return denied;
		}
		WpsFileProvider p = providerOf(fileId);
		if (p == null) {
			return err(WpsCallbackSupport.CODE_FILE_INVALID, "file_id invalid");
		}
		return json(p.permission(bizOf(fileId),
			WpsCallbackSupport.userQuery(req.getHeader(WpsCallbackSupport.HEADER_USER_QUERY))));
	}

	// ---------------- 编辑保存（三阶段） ----------------

	@PreAuth("permitAll()")
	@GetMapping("/files/{fileId}/upload/prepare")
	@Operation(summary = "WPS-上传准备")
	public ResponseEntity<Map<String, Object>> prepare(HttpServletRequest req, @PathVariable String fileId) {
		ResponseEntity<Map<String, Object>> denied = authGuard(req, fileId);
		if (denied != null) {
			return denied;
		}
		return json(new LinkedHashMap<>(Map.of("digest_types", List.of("sha1"))));
	}

	@PreAuth("permitAll()")
	@PostMapping("/files/{fileId}/upload/address")
	@Operation(summary = "WPS-获取上传地址")
	public ResponseEntity<Map<String, Object>> address(HttpServletRequest req, @PathVariable String fileId,
													   @RequestBody(required = false) Map<String, Object> body) {
		ResponseEntity<Map<String, Object>> denied = authGuard(req, fileId);
		if (denied != null) {
			return denied;
		}
		WpsFileProvider p = providerOf(fileId);
		if (p == null) {
			return err(WpsCallbackSupport.CODE_FILE_INVALID, "file_id invalid");
		}
		String name = (body != null && body.get("name") != null) ? String.valueOf(body.get("name")) : null;
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("method", "PUT");
		r.put("url", absolute(req, p, p.uploadUrl(fileId, name)));
		return json(r);
	}

	/** 三阶段保存第二步：WPS 以 PUT 将文件实体上传到 address 返回的 url（octet-stream body） */
	@PreAuth("permitAll()")
	@PutMapping(value = "/files/{fileId}/upload", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@Operation(summary = "WPS-上传文件实体(PUT)")
	public ResponseEntity<Map<String, Object>> uploadPut(HttpServletRequest req, @PathVariable String fileId,
														 @RequestParam(name = "name", required = false) String name,
														 @RequestBody byte[] body) {
		ResponseEntity<Map<String, Object>> denied = authGuard(req, fileId);
		if (denied != null) {
			return denied;
		}
		return saveNewVersion(fileId, body, name);
	}

	/** 单阶段保存（兼容保留，WPS 已推荐三阶段） */
	@PreAuth("permitAll()")
	@PostMapping(value = "/files/{fileId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "WPS-上传(单阶段保存)")
	public ResponseEntity<Map<String, Object>> upload(HttpServletRequest req, @PathVariable String fileId,
													  @RequestParam("file") MultipartFile file) {
		ResponseEntity<Map<String, Object>> denied = authGuard(req, fileId);
		if (denied != null) {
			return denied;
		}
		if (file == null || file.isEmpty()) {
			return err(WpsCallbackSupport.CODE_FILE_INVALID, "empty file");
		}
		try {
			return saveNewVersion(fileId, file.getBytes(), file.getOriginalFilename());
		} catch (IOException e) {
			log.error("WPS 上传读取失败 fileId={}", fileId, e);
			return err(WpsCallbackSupport.CODE_SERVER_ERROR, "read error");
		}
	}

	@PreAuth("permitAll()")
	@PostMapping("/files/{fileId}/upload/complete")
	@Operation(summary = "WPS-上传完成回调")
	public ResponseEntity<Map<String, Object>> complete(HttpServletRequest req, @PathVariable String fileId) {
		ResponseEntity<Map<String, Object>> denied = authGuard(req, fileId);
		if (denied != null) {
			return denied;
		}
		WpsFileProvider p = providerOf(fileId);
		if (p == null) {
			return err(WpsCallbackSupport.CODE_FILE_INVALID, "file_id invalid");
		}
		Map<String, Object> data = versionData(fileId, p.info(bizOf(fileId)));
		return data == null ? err(WpsCallbackSupport.CODE_FILE_INVALID, "file_id invalid") : json(data);
	}

	/** 保存为新版本，委托 Provider 落库（控制器不碰业务表） */
	private ResponseEntity<Map<String, Object>> saveNewVersion(String fileId, byte[] content, String name) {
		if (content == null || content.length == 0) {
			return err(WpsCallbackSupport.CODE_FILE_INVALID, "empty file");
		}
		WpsFileProvider p = providerOf(fileId);
		if (p == null) {
			return err(WpsCallbackSupport.CODE_FILE_INVALID, "file_id invalid");
		}
		try {
			Map<String, Object> data = versionData(fileId, p.saveNewVersion(bizOf(fileId), content, name));
			return json(data);
		} catch (Exception e) {
			log.error("WPS 保存失败 fileId={}", fileId, e);
			return err(WpsCallbackSupport.CODE_SERVER_ERROR, "save error");
		}
	}

	// ---------------- 重命名 / 历史版本 ----------------

	@PreAuth("permitAll()")
	@PutMapping("/files/{fileId}/name")
	@Operation(summary = "WPS-重命名")
	public ResponseEntity<Map<String, Object>> rename(HttpServletRequest req, @PathVariable String fileId,
													  @RequestBody(required = false) Map<String, Object> body) {
		ResponseEntity<Map<String, Object>> denied = authGuard(req, fileId);
		if (denied != null) {
			return denied;
		}
		WpsFileProvider p = providerOf(fileId);
		if (p == null) {
			return err(WpsCallbackSupport.CODE_FILE_INVALID, "file_id invalid");
		}
		String name = (body != null && body.get("name") != null) ? String.valueOf(body.get("name")) : null;
		if (name == null || name.isBlank()) {
			return err(WpsCallbackSupport.CODE_FILE_INVALID, "name required");
		}
		p.rename(bizOf(fileId), name);
		return json(new LinkedHashMap<>());
	}

	@PreAuth("permitAll()")
	@GetMapping("/files/{fileId}/versions")
	@Operation(summary = "WPS-历史版本列表")
	public ResponseEntity<Map<String, Object>> versions(HttpServletRequest req, @PathVariable String fileId,
														@RequestParam(required = false) Integer offset,
														@RequestParam(required = false) Integer limit) {
		ResponseEntity<Map<String, Object>> denied = authGuard(req, fileId);
		if (denied != null) {
			return denied;
		}
		WpsFileProvider p = providerOf(fileId);
		if (p == null) {
			return err(WpsCallbackSupport.CODE_FILE_INVALID, "file_id invalid");
		}
		List<WpsFileMeta> list = p.versions(bizOf(fileId));
		int from = (offset != null && offset > 0) ? offset : 0;
		int to = list.size();
		if (limit != null && limit > 0) {
			to = Math.min(to, from + limit);
		}
		List<Map<String, Object>> arr = new ArrayList<>();
		for (int i = from; i < to; i++) {
			WpsFileMeta m = list.get(i);
			m.setId(fileId);
			arr.add(WpsCallbackSupport.versionItem(m));
		}
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("code", 0);
		r.put("data", arr);
		r.put("message", "");
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(r);
	}

	@PreAuth("permitAll()")
	@GetMapping("/files/{fileId}/versions/{version}")
	@Operation(summary = "WPS-指定历史版本信息")
	public ResponseEntity<Map<String, Object>> versionInfo(HttpServletRequest req, @PathVariable String fileId,
														   @PathVariable int version) {
		ResponseEntity<Map<String, Object>> denied = authGuard(req, fileId);
		if (denied != null) {
			return denied;
		}
		WpsFileProvider p = providerOf(fileId);
		if (p == null) {
			return err(WpsCallbackSupport.CODE_FILE_INVALID, "file_id invalid");
		}
		List<WpsFileMeta> list = p.versions(bizOf(fileId));
		int idx = version - 1;
		if (idx < 0 || idx >= list.size()) {
			return err(WpsCallbackSupport.CODE_FILE_INVALID, "version not found");
		}
		return json(versionData(fileId, list.get(idx)));
	}

	@PreAuth("permitAll()")
	@GetMapping("/files/{fileId}/versions/{version}/download")
	@Operation(summary = "WPS-指定历史版本下载地址")
	public ResponseEntity<Map<String, Object>> versionDownload(HttpServletRequest req, @PathVariable String fileId,
															   @PathVariable int version) {
		ResponseEntity<Map<String, Object>> denied = authGuard(req, fileId);
		if (denied != null) {
			return denied;
		}
		WpsFileProvider p = providerOf(fileId);
		if (p == null) {
			return err(WpsCallbackSupport.CODE_FILE_INVALID, "file_id invalid");
		}
		String url = absolute(req, p, p.versionDownloadUrl(bizOf(fileId), version));
		return url == null ? err(WpsCallbackSupport.CODE_FILE_INVALID, "version not found")
			: json(new LinkedHashMap<>(Map.of("url", url)));
	}

	// ---------------- 附件对象（智能文档/多维表格 插图，doc 域特有） ----------------

	@PreAuth("permitAll()")
	@PutMapping(value = "/object/{key}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@Operation(summary = "WPS-上传附件对象")
	public ResponseEntity<Map<String, Object>> objectPut(HttpServletRequest req,
														 @PathVariable String key,
														 @RequestParam(name = "name", required = false) String name,
														 @RequestBody byte[] body) {
		ResponseEntity<Map<String, Object>> denied = auxGuard(req);
		if (denied != null) {
			return denied;
		}
		if (body == null || body.length == 0) {
			return err(WpsCallbackSupport.CODE_FILE_INVALID, "empty body");
		}
		try {
			Long fileId = fileStore.save(body);
			DocImageFile rec = new DocImageFile();
			rec.setDocId(0L);
			rec.setFileId(fileId);
			rec.setOssFileId(key);
			rec.setFileName(name != null && !name.isBlank() ? name : key);
			rec.setFileSize((long) body.length);
			rec.setDocFileType(4);
			imageFileMapper.insert(rec);
			return json(new LinkedHashMap<>());
		} catch (Exception e) {
			log.error("WPS 附件对象上传失败 key={}", key, e);
			return err(WpsCallbackSupport.CODE_SERVER_ERROR, "save error");
		}
	}

	@PreAuth("permitAll()")
	@GetMapping("/object/{key}/url")
	@Operation(summary = "WPS-附件对象下载地址")
	public ResponseEntity<Map<String, Object>> objectUrl(HttpServletRequest req, @PathVariable String key) {
		ResponseEntity<Map<String, Object>> denied = auxGuard(req);
		if (denied != null) {
			return denied;
		}
		DocImageFile rec = objectOf(key);
		if (rec == null || rec.getFileId() == null) {
			return err(WpsCallbackSupport.CODE_FILE_INVALID, "object not found");
		}
		WpsFileProvider p = providers.get(WpsFileId.DOMAIN_DOC);
		String base = WpsCallbackSupport.publicBase(p == null ? null : p.publicBase(),
			req.getScheme(), req.getServerName(), req.getServerPort());
		String url = base + "/doc/file/" + rec.getFileId() + "/content?token="
			+ docSigner.sign(String.valueOf(rec.getFileId()));
		return json(new LinkedHashMap<>(Map.of("url", url)));
	}

	@PreAuth("permitAll()")
	@PostMapping("/object/copy")
	@Operation(summary = "WPS-拷贝附件对象")
	public ResponseEntity<Map<String, Object>> objectCopy(HttpServletRequest req,
															@RequestBody(required = false) Map<String, Object> body) {
		ResponseEntity<Map<String, Object>> denied = auxGuard(req);
		if (denied != null) {
			return denied;
		}
		if (body == null || body.get("key_dict") == null) {
			return err(WpsCallbackSupport.CODE_FILE_INVALID, "key_dict required");
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> kd = (Map<String, Object>) body.get("key_dict");
			for (Map.Entry<String, Object> e : kd.entrySet()) {
				String src = String.valueOf(e.getKey());
				String dst = String.valueOf(e.getValue());
				DocImageFile s = objectOf(src);
				if (s == null || s.getFileId() == null) {
					continue;
				}
				byte[] content = fileStore.download(s.getFileId()).orElse(null);
				if (content == null) {
					continue;
				}
				Long nf = fileStore.save(content);
				DocImageFile rec = new DocImageFile();
				rec.setDocId(0L);
				rec.setFileId(nf);
				rec.setOssFileId(dst);
				rec.setFileName(s.getFileName());
				rec.setFileSize((long) content.length);
				rec.setDocFileType(4);
				imageFileMapper.insert(rec);
			}
			return json(new LinkedHashMap<>());
		} catch (Exception ex) {
			log.error("WPS 附件对象拷贝失败", ex);
			return err(WpsCallbackSupport.CODE_SERVER_ERROR, "copy error");
		}
	}

	private DocImageFile objectOf(String key) {
		if (key == null || key.isBlank()) {
			return null;
		}
		return imageFileMapper.selectOne(Wrappers.<DocImageFile>lambdaQuery()
			.eq(DocImageFile::getOssFileId, key).eq(DocImageFile::getDocFileType, 4)
			.orderByDesc(DocImageFile::getFileId).last("limit 1"));
	}

	// ---------------- 用户信息 ----------------

	@PreAuth("permitAll()")
	@GetMapping("/users")
	@Operation(summary = "WPS-用户信息(批量)")
	public ResponseEntity<Map<String, Object>> users(HttpServletRequest req,
													 @RequestParam(name = "user_ids", required = false) List<String> userIds) {
		ResponseEntity<Map<String, Object>> denied = auxGuard(req);
		if (denied != null) {
			return denied;
		}
		Map<String, String> uq = WpsCallbackSupport.userQuery(req.getHeader(WpsCallbackSupport.HEADER_USER_QUERY));
		String curId = uq.get("userId");
		String curName = uq.get("userName");
		List<Map<String, Object>> arr = new ArrayList<>();
		if (userIds != null) {
			for (String uid : userIds) {
				if (uid == null || uid.isBlank()) {
					continue;
				}
				Map<String, Object> u = new LinkedHashMap<>();
				u.put("id", uid);
				u.put("name", (uid.equals(curId) && curName != null && !curName.isBlank()) ? curName : nickNameOf(uid));
				arr.add(u);
			}
		}
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("code", 0);
		r.put("data", arr);
		r.put("message", "");
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(r);
	}

	private String nickNameOf(String uid) {
		try {
			return jdbcTemplate.queryForObject("SELECT account FROM sys_user WHERE id=?", String.class, Long.valueOf(uid));
		} catch (Exception e) {
			return "用户" + uid;
		}
	}

	// ---------------- 响应 ----------------

	private static ResponseEntity<Map<String, Object>> json(Map<String, Object> data) {
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(WpsCallbackSupport.ok(data));
	}

	private static ResponseEntity<Map<String, Object>> err(int code, String message) {
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(WpsCallbackSupport.err(code, message));
	}
}
