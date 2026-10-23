package org.springblade.doc.store;

import java.util.Optional;

/**
 * 内容文件存储接口：fileId → 字节。
 *
 * <p>默认本地实现 {@link LocalDocFileStore}；后续可切换对象存储（接口不变）。</p>
 */
public interface IDocFileStore {

    /** 按 fileId 取文件字节；不存在返回 empty */
    Optional<byte[]> download(Long fileId);

    /** 保存文件字节，返回生成的 fileId（雪花） */
    Long save(byte[] content);
}
