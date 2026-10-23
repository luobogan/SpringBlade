package org.springblade.doc.service;

/**
 * 内容操作日志服务（C-12）：对齐 E9 DocDetailLog / DownloadLog。
 */
public interface IDocLogService {

    /** 记录操作日志（operateType：0阅读 1新增 2改 13发布 22下载 23共享…） */
    void logOperate(Long docId, Integer operateType, String desc);

    /** 记录下载日志 */
    void logDownload(Long docId, Long fileId);
}
