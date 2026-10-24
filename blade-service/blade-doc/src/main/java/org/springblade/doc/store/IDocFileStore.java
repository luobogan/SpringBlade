package org.springblade.doc.store;

import java.util.Optional;

/**
 * 内容文件存储接口：fileId → 字节。
 *
 * <p>默认实现 {@link LocalDocFileStore}（已统一到通用上传组件 blade-file）。</p>
 */
public interface IDocFileStore {

	/** 按 fileId 取文件字节；不存在返回 empty */
	Optional<byte[]> download(Long fileId);

	/** 保存文件字节，返回生成的 fileId（雪花） */
	Long save(byte[] content);

	/** 保存文件字节并保留原始文件名，返回生成的 fileId */
	default Long save(byte[] content, String fileName) {
		return save(content);
	}

	/** 判断 fileId 对应内容文件是否存在 */
	boolean exists(Long fileId);
}