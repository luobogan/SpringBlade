package org.springblade.doc.service;

import org.springblade.doc.entity.DocImageFile;

import java.util.List;

/**
 * 文档附件服务（列表/上传/重命名/删除）。
 */
public interface IDocAttachmentService {

    /** 附件列表（canRead 校验） */
    List<DocImageFile> list(Long docId);

    /** 上传附件：落盘 → 建 DocImageFile（canManage 校验） */
    DocImageFile upload(Long docId, byte[] content, String fileName, Integer docFileType);

    /** 登记系统通用附件：前端直传 OSS 后回写元数据（isExtFile=1），canManage 校验 */
    DocImageFile registerExternal(Long docId, String ossFileId, String ossFileUrl, String fileName, Long fileSize, Integer docFileType);

    /** 重命名附件 */
    DocImageFile rename(Long id, String fileName);

    /**
     * 删除附件
     *
     * @param id       附件记录ID
     * @param physical 是否同时删除物理文件（true=连统一存储的内容与元数据一起删；false=只删业务记录、保留文件）
     */
    boolean delete(Long id, boolean physical);
}
