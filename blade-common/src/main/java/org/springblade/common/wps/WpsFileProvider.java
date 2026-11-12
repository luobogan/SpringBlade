package org.springblade.common.wps;

import java.util.List;
import java.util.Map;

/**
 * WPS 回调协议与业务存储之间的 SPI。
 *
 * <p>「知识管理」与「收发文」各自实现一个 Provider，操作自己的表与文件存储：
 * <ul>
 *   <li>{@code doc} → blade-doc 的 {@code DocImageFile / DocDetail / IDocFileStore}</li>
 *   <li>{@code odoc} → blade-workflow 的 {@code WfOdocDocVersion / IOdocFileStore}</li>
 * </ul>
 * 公共 {@code WpsCallbackController} 只做协议编排，不依赖任何业务表。</p>
 *
 * <p><b>关键约束</b>：字节流不经过回调入口。{@link #downloadUrl} 与 {@link #uploadUrl}
 * 返回指向文件归属服务的绝对签名 URL，由 WPS 中台直连读写；入口只处理元数据。</p>
 */
public interface WpsFileProvider {

	/** 支持的业务域：doc / odoc */
	String domain();

	/** 该业务域对外可达的公网 base（拼 download/upload 绝对 URL 用）；为空时由入口按请求推断兜底 */
	String publicBase();

	/** 文件元信息；不存在返回 null（入口统一回 40005） */
	WpsFileMeta info(String bizId);

	/** 当前版本内容的签名下载 URL */
	String downloadUrl(String bizId);

	/**
	 * 三阶段保存的 PUT 上传 URL（address 端点返回）。
	 *
	 * <p><b>参数是 wireFileId（WPS 中台实际使用的 fileId），不是 bizId</b>：
	 * 知识管理存量会话为纯数字 legacy fileId，收发文接入后用 {@code odoc:xxx} 结构化形式；
	 * 上传地址必须原样回带 wireFileId，否则 WPS 中台后续 PUT 的 fileId 与会话不一致，
	 * 且回调入口无法按域路由。实现内部如需 bizId 可 {@link WpsFileId#parse} 解析。</p>
	 */
	String uploadUrl(String wireFileId, String name);

	/** 保存为新版本并返回新版本元信息 */
	WpsFileMeta saveNewVersion(String bizId, byte[] content, String name);

	/** 版本链（按时间正序，v1 最早） */
	List<WpsFileMeta> versions(String bizId);

	/** 指定历史版本的签名下载 URL；不存在返回 null */
	String versionDownloadUrl(String bizId, int version);

	void rename(String bizId, String name);

	/** 文档级权限；key 对齐 WPS 协议 read/update/download/rename/history/copy/print/saveas/comment/user_id */
	Map<String, Object> permission(String bizId, Map<String, String> userQuery);
}
