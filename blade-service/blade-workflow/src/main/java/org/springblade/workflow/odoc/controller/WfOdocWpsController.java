package org.springblade.workflow.odoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.common.wps.WpsCallbackSupport;
import org.springblade.common.wps.WpsFileId;
import org.springblade.common.wps.WpsFileMeta;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.odoc.OdocUrlSigner;
import org.springblade.workflow.odoc.service.IWfOdocDocService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 公文 WPS WebOffice 直连端点（WPS 统一改造 P2）。
 *
 * <p>与知识管理共用 WPS 中台（同一 appId，回调经 blade-doc 单入口 {@code /v3/3rd/files/*}）；
 * 此处端点只服务「字节流与元数据」——WPS 中台按 v3 协议向 blade-doc 拉元信息，但下载/上传
 * 直接打到这里（避免大文件绕经 blade-doc 回调入口）：</p>
 * <ul>
 *   <li>{@code GET  /odoc/wps/content?docId=&version=} 版本内容下载</li>
 *   <li>{@code PUT  /odoc/wps/upload/{fileId}} 三阶段保存上传（fileId = odoc:{docId}）</li>
 *   <li>{@code GET  /odoc/wps/meta|versions|version-meta} v3 元数据（由 blade-doc 的 OdocWpsProvider 经 Feign 取）</li>
 *   <li>{@code POST /odoc/wps/save} 单阶段保存兜底（Feign，正常三阶段不走）</li>
 * </ul>
 *
 * <p>全部 permitAll：字节流端点靠 URL 签名（{@code WpsUrlSigner}），服务间元数据/兜底保存靠
 * {@link WpsCallbackSupport#HEADER_INTERNAL_TOKEN} 按资源签发的服务间 token。</p>
 */
@Slf4j
@RestController
@RequestMapping("/odoc/wps")
@RequiredArgsConstructor
@Tag(name = "公文-WPS WebOffice直连", description = "WPS 中台下载/上传/元数据端点（字节流不经 blade-doc 回调入口）")
public class WfOdocWpsController {

	private final IWfOdocDocService docService;
	private final OdocUrlSigner signer;

	/**
	 * 服务间调用签名校验：resource = {@code wps:odoc:{op}:{bizId}}，
	 * 与 blade-doc 的 {@code OdocWpsProvider} 用同一把共享密钥逐字约定。
	 */
	private void requireInternal(String op, Object bizId, String token) {
		if (!signer.verify(WpsCallbackSupport.internalResource(op, bizId), token)) {
			throw new ServiceException("无权限");
		}
	}

	@Operation(summary = "WPS 版本内容下载")
	@GetMapping("/content")
	public ResponseEntity<byte[]> content(@RequestParam Long docId, @RequestParam int version,
										  @RequestParam(required = false) String token) {
		if (!signer.verify(docId + ":" + version, token)) {
			throw new ServiceException("无权限");
		}
		byte[] bytes = docService.wpsVersionContent(docId, version);
		if (bytes == null) {
			throw new ServiceException("版本不存在");
		}
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_OCTET_STREAM)
			.header("Content-Disposition", "attachment; filename=\""
				+ URLEncoder.encode("doc-" + docId + "-v" + version + ".docx", StandardCharsets.UTF_8) + "\"")
			.body(bytes);
	}

	@Operation(summary = "WPS 三阶段保存-上传（PUT，octet-stream）")
	@PutMapping(value = "/upload/{fileId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<Void> upload(@PathVariable String fileId,
									   @RequestParam(required = false) String name,
									   @RequestParam(required = false) String token,
									   @RequestBody byte[] content) {
		if (!signer.verify("wps:" + fileId, token)) {
			throw new ServiceException("无权限");
		}
		// P3.6 域隔离：必须证明 fileId 属于 odoc 域，否则持有 doc 域合法 token 的请求
		// 也能把字节流写成收发文版本（parse 只判格式、不判域）
		WpsFileId fid = WpsFileId.parseFor(fileId, WpsFileId.DOMAIN_ODOC);
		if (fid == null) {
			throw new ServiceException("file_id 非法");
		}
		docService.saveWpsVersion(Long.valueOf(fid.bizId()), content, name);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "WPS v3 元信息（服务间）")
	@GetMapping("/meta")
	public R<WpsFileMeta> meta(@RequestParam Long docId,
							   @RequestHeader(value = WpsCallbackSupport.HEADER_INTERNAL_TOKEN, required = false) String token) {
		requireInternal("meta", docId, token);
		return R.data(docService.wpsMeta(docId));
	}

	@Operation(summary = "WPS v3 版本链（服务间）")
	@GetMapping("/versions")
	public R<List<WpsFileMeta>> versions(@RequestParam Long docId,
										@RequestHeader(value = WpsCallbackSupport.HEADER_INTERNAL_TOKEN, required = false) String token) {
		requireInternal("versions", docId, token);
		return R.data(docService.wpsVersions(docId));
	}

	@Operation(summary = "WPS v3 单版本元信息（服务间）")
	@GetMapping("/version-meta")
	public R<WpsFileMeta> versionMeta(@RequestParam Long versionId,
									  @RequestHeader(value = WpsCallbackSupport.HEADER_INTERNAL_TOKEN, required = false) String token) {
		requireInternal("version-meta", versionId, token);
		return R.data(docService.wpsVersionMeta(versionId));
	}

	/**
	 * 单阶段保存兜底：blade-doc 的 {@code OdocWpsProvider#saveNewVersion} 经 Feign 打到这里。
	 * 正常 WPS 三阶段保存走上面的直连 PUT，本端点仅在中台退回单阶段上传时兜底，避免 404。
	 */
	@Operation(summary = "WPS 保存新版本（Feign 兜底）")
	@PostMapping(value = "/save", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public R<WpsFileMeta> save(@RequestParam Long docId,
							   @RequestParam(required = false) String name,
							   @RequestHeader(value = WpsCallbackSupport.HEADER_INTERNAL_TOKEN, required = false) String token,
							   @RequestBody byte[] content) {
		requireInternal("save", docId, token);
		if (content == null || content.length == 0) {
			throw new ServiceException("文件内容为空");
		}
		return R.data(docService.saveWpsVersion(docId, content, name));
	}
}
