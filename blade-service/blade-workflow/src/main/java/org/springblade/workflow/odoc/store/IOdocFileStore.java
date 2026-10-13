package org.springblade.workflow.odoc.store;

/**
 * 公文文件存储抽象。
 *
 * <p>odoc 版本表只存平台文件服务的 {@code fileId}（Long，外键语义），本接口负责把
 * OnlyOffice 回传的临时文件落库、以及把 fileId 还原成可下载地址。默认实现走平台
 * blade-resource，若文件服务契约不同，替换实现即可，业务层零改动。</p>
 */
public interface IOdocFileStore {

	/**
	 * 从 OnlyOffice 给的临时下载地址拉取编辑后文件，落库并返回平台文件ID。
	 *
	 * @param sourceUrl OnlyOffice 回调里 {@code url} 字段给出的临时下载地址
	 * @param fileName  文件名（含扩展名）
	 * @return 平台文件服务的主键 fileId
	 */
	Long storeFromUrl(String sourceUrl, String fileName);

	/** 直接上传字节流并返回平台文件ID（套红产物等本服务生成的文档用） */
	Long store(byte[] bytes, String fileName);

	/**
	 * 取某 fileId 的可下载内容 URL（供 OnlyOffice document.url 与前端预览）。
	 */
	String getDownloadUrl(Long fileId);

	/**
	 * 直接下载文件字节（内容代理端点用）。
	 */
	byte[] download(Long fileId);

	/**
	 * 还原某 fileId 落库时的真实文件名（含扩展名），用于下载端点的
	 * Content-Disposition。默认实现按 fileId 在存储目录里匹配真实文件；
	 * 若匹配不到则回退为空串，由调用方决定兜底名。
	 */
	String resolveFileName(Long fileId);

}
