package org.springblade.doc.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.doc.entity.DocDetailLog;
import org.springblade.doc.entity.DownloadLog;
import org.springblade.doc.mapper.DocDetailLogMapper;
import org.springblade.doc.mapper.DownloadLogMapper;
import org.springblade.doc.service.IDocLogService;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocLogServiceImpl implements IDocLogService {

    private final DocDetailLogMapper detailLogMapper;
    private final DownloadLogMapper downloadLogMapper;

    @Override
    public void logOperate(Long docId, Integer operateType, String desc) {
        if (docId == null || operateType == null) {
            return;
        }
        try {
            DocDetailLog log = new DocDetailLog();
            log.setDocId(docId);
            log.setOperateType(operateType);
            log.setOperateUser(SecureUtil.getUserId());
            log.setOperateDesc(desc);
            detailLogMapper.insert(log);
        } catch (Exception e) {
            // 日志失败不得阻断主流程
            log.warn("写操作日志失败 docId={} operateType={}", docId, operateType, e);
        }
    }

    @Override
    public void logDownload(Long docId, Long fileId) {
        try {
            DownloadLog log = new DownloadLog();
            log.setDocId(docId);
            log.setFileId(fileId);
            log.setOperateUser(SecureUtil.getUserId());
            downloadLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("写下载日志失败 docId={} fileId={}", docId, fileId, e);
        }
    }
}
