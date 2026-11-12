package org.springblade.workflow.api;

import org.springblade.common.wps.WpsCallbackSupport;
import org.springblade.common.wps.WpsFileMeta;
import org.springblade.core.tool.api.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 收发文（odoc）WPS 文件操作 Feign 客户端（WPS 统一改造 P2-2.1）。
 *
 * <p>blade-doc 的 {@code OdocWpsProvider} 经由此客户端把 WPS v3 元数据请求代理到
 * blade-workflow（odoc 正文/版本链的真实归属服务），字节流不经过 blade-doc 单回调入口
 * （download/upload 直接指向 workflow 自身端点）。</p>
 *
 * <p><b>契约要点（本轮收口）</b>：
 * ① 返回类型必须是 {@link R} 包装——workflow 端点返回 {@code R<WpsFileMeta>}，
 * 若 Feign 声明裸 {@link WpsFileMeta}，Jackson 会把 {@code {code,success,data,msg}}
 * 映射成字段全默认值的空对象（不报错但数据全丢）；调用方需自行 {@code getData()}。
 * ② 每个方法都带 {@link WpsCallbackSupport#HEADER_INTERNAL_TOKEN} 服务间签名，
 * resource 由 {@link WpsCallbackSupport#internalResource(String, Object)} 逐字约定，
 * 防止 workflow 端口对外暴露时元数据被按 docId 枚举。</p>
 *
 * <p>方法契约与 workflow 侧 {@code WfOdocWpsController} 完全对应。</p>
 */
@FeignClient(value = "blade-workflow", contextId = "wpsOdocFile")
public interface IWpsOdocFileClient {

	@GetMapping("/odoc/wps/meta")
	R<WpsFileMeta> meta(@RequestParam("docId") Long docId,
						@RequestHeader(WpsCallbackSupport.HEADER_INTERNAL_TOKEN) String token);

	@GetMapping("/odoc/wps/versions")
	R<List<WpsFileMeta>> versions(@RequestParam("docId") Long docId,
								  @RequestHeader(WpsCallbackSupport.HEADER_INTERNAL_TOKEN) String token);

	@GetMapping("/odoc/wps/version-meta")
	R<WpsFileMeta> versionMeta(@RequestParam("versionId") Long versionId,
							   @RequestHeader(WpsCallbackSupport.HEADER_INTERNAL_TOKEN) String token);

	/** 保存新版本（防御性：正常 WPS 三阶段保存走 workflow 直连 PUT，不经 blade-doc） */
	@PostMapping(value = "/odoc/wps/save", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	R<WpsFileMeta> save(@RequestParam("docId") Long docId,
						@RequestParam("name") String name,
						@RequestHeader(WpsCallbackSupport.HEADER_INTERNAL_TOKEN) String token,
						@RequestBody byte[] content);
}
