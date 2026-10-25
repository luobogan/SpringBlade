package org.springblade.doc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.file.service.IFileStorageService;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.doc.config.DocProperties;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.entity.DocDetailContent;
import org.springblade.doc.entity.DocDetailLog;
import org.springblade.doc.entity.DocImageFile;
import org.springblade.doc.entity.DocPraise;
import org.springblade.doc.entity.DocRecycle;
import org.springblade.doc.entity.DocReply;
import org.springblade.doc.entity.DocScore;
import org.springblade.doc.entity.DocShare;
import org.springblade.doc.entity.DownloadLog;
import org.springblade.doc.enums.DocStatus;
import org.springblade.doc.mapper.DocDetailContentMapper;
import org.springblade.doc.mapper.DocDetailLogMapper;
import org.springblade.doc.mapper.DocDetailMapper;
import org.springblade.doc.mapper.DocImageFileMapper;
import org.springblade.doc.mapper.DocPraiseMapper;
import org.springblade.doc.mapper.DocRecycleMapper;
import org.springblade.doc.mapper.DocReplyMapper;
import org.springblade.doc.mapper.DocScoreMapper;
import org.springblade.doc.mapper.DocShareMapper;
import org.springblade.doc.mapper.DownloadLogMapper;
import org.springblade.doc.service.DocPermissionService;
import org.springblade.doc.service.IDocRecycleService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 回收站服务实现。
 *
 * <p><b>DET-P0-1 修复</b>：E9 回收站列表权限判断被整段注释，任意登录用户可拉取。
 * 本实现列表/恢复/彻底删除均由控制器 {@code @PreAuth(HAS_ROLE_DOC)} 门禁（内容管理员），
 * 服务层不再依赖「被注释」的判断。</p>
 *
 * <p><b>补齐回收站入口</b>：此前全模块无任何「删除到回收站」的写入路径（doc_recycle 只被
 * 读/删，DocDetailController 无删除接口，检索页无删除按钮），导致回收站永远为空、
 * 恢复/彻底删除无从触发。现由 {@link #removeToRecycle(Long)} 提供唯一入口：
 * 主表 is_deleted 置 1（逻辑删除，检索即不可见）+ 写 doc_recycle 记录。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocRecycleServiceImpl implements IDocRecycleService {

    private final DocRecycleMapper recycleMapper;
    private final DocDetailMapper detailMapper;
    private final DocDetailContentMapper contentMapper;
    private final DocImageFileMapper imageFileMapper;
    private final DocReplyMapper replyMapper;
    private final DocPraiseMapper praiseMapper;
    private final DocScoreMapper scoreMapper;
    private final DocShareMapper shareMapper;
    private final DocDetailLogMapper detailLogMapper;
    private final DownloadLogMapper downloadLogMapper;
    private final DocPermissionService permissionService;
    private final IFileStorageService fileStorageService;
    private final DocProperties properties;

    @Override
    public List<DocRecycle> list(Long secCategoryId) {
        List<DocRecycle> items;
        if (secCategoryId == null) {
            items = recycleMapper.selectList(
                Wrappers.<DocRecycle>lambdaQuery().orderByDesc(DocRecycle::getDeleteTime));
        } else {
            // 按目录过滤：取该目录下所有文档的 docId，再过滤回收站。
            // 注意：回收站里的文档已逻辑删除，必须用不含 is_deleted 条件的查询，否则按目录过滤恒为空。
            List<DocDetail> docs = detailMapper.selectBriefByCategory(secCategoryId);
            if (docs.isEmpty()) {
                return Collections.emptyList();
            }
            List<Long> docIds = docs.stream().map(DocDetail::getId).collect(Collectors.toList());
            items = recycleMapper.selectList(
                Wrappers.<DocRecycle>lambdaQuery()
                    .in(DocRecycle::getDocId, docIds)
                    .orderByDesc(DocRecycle::getDeleteTime));
        }
        fillSubject(items);
        return items;
    }

    /**
     * 回填文档标题：回收站里的文档已逻辑删除，检索接口查不到，只能由回收站列表关联回填，
     * 否则列表只剩 docId，管理员无从判断删的是哪篇文档。
     */
    private void fillSubject(List<DocRecycle> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Long> ids = items.stream()
            .map(DocRecycle::getDocId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> subjects = detailMapper.selectBriefByIds(ids).stream()
            .filter(d -> d.getId() != null)
            .collect(Collectors.toMap(DocDetail::getId, d -> d.getDocSubject() == null ? "" : d.getDocSubject(), (a, b) -> a));
        items.forEach(it -> {
            String subject = subjects.get(it.getDocId());
            if (subject != null) {
                it.setDocSubject(subject);
            }
        });
    }

    @Override
    public boolean removeToRecycle(Long docId) {
        if (docId == null) {
            throw new ServiceException("docId 不能为空");
        }
        // 1) 取存活文档（MP 的 selectById 自动追加 is_deleted=0）
        DocDetail doc = detailMapper.selectById(docId);
        if (doc == null) {
            throw new ServiceException("文档不存在或已删除");
        }
        // 2) 权限：只有能管理该文档的人才能删除（fail-closed）
        if (!permissionService.canManage(docId)) {
            throw new ServiceException("无权限删除该文档");
        }
        // 3) 状态守卫：审批中 / 已签出的文档不允许直接丢弃
        if (doc.getDocStatus() != null && doc.getDocStatus() == DocStatus.APPROVING.getCode()) {
            throw new ServiceException("审批中的文档不能删除，请先撤回审批");
        }
        if (doc.getCheckOutStatus() != null && doc.getCheckOutStatus() == 1) {
            throw new ServiceException("文档已签出，请先签入再删除");
        }
        // 4) 逻辑删除主表（检索即不可见）
        Long userId = SecureUtil.getUserId();
        int n = detailMapper.markRecycled(docId, DateUtil.now(), userId);
        if (n <= 0) {
            throw new ServiceException("文档不存在或已删除");
        }
        // 5) 登记回收站记录（同 docId 只保留一条，重复删除以最后一次为准）
        recycleMapper.deletePhysicallyByDocId(docId);
        DocRecycle recycle = new DocRecycle();
        recycle.setDocId(docId);
        recycle.setDeleteUser(userId);
        recycle.setDeleteTime(LocalDateTime.now());
        recycle.setCreateUser(userId);
        recycle.setUpdateUser(userId);
        recycle.setCreateTime(DateUtil.now());
        recycle.setUpdateTime(DateUtil.now());
        String tenantId = SecureUtil.getTenantId();
        if (tenantId != null && !tenantId.isBlank()) {
            recycle.setTenantId(tenantId);
        }
        recycleMapper.insert(recycle);
        log.info("文档移入回收站 docId={} deleteUser={}", docId, userId);
        return true;
    }

    @Override
    public boolean restore(Long docId) {
        if (docId == null) {
            throw new ServiceException("docId 不能为空");
        }
        DocRecycle recycle = recycleMapper.selectOne(
            Wrappers.<DocRecycle>lambdaQuery().eq(DocRecycle::getDocId, docId).last("LIMIT 1"));
        if (recycle == null) {
            return false;
        }
        // 恢复 = 清除逻辑删除标记，doc_status 保持删除前原值（恢复 ≠ 发布）
        int n = detailMapper.restoreFromRecycle(docId, DateUtil.now(), SecureUtil.getUserId());
        recycleMapper.deletePhysicallyByDocId(docId);
        log.info("回收站恢复文档 docId={} restored={}", docId, n);
        return n > 0;
    }

    @Override
    public boolean purge(Long docId) {
        if (docId == null) {
            throw new ServiceException("docId 不能为空");
        }
        DocRecycle recycle = recycleMapper.selectOne(
            Wrappers.<DocRecycle>lambdaQuery().eq(DocRecycle::getDocId, docId).last("LIMIT 1"));
        if (recycle == null) {
            throw new ServiceException("该文档不在回收站中，无法彻底删除");
        }
        // 1) 附件：先删物理文件（含统一文件服务与改造前本地落盘），再删业务记录
        List<DocImageFile> files = imageFileMapper.selectList(
            Wrappers.<DocImageFile>lambdaQuery().eq(DocImageFile::getDocId, docId));
        for (DocImageFile f : files) {
            if (f.getFileId() != null) {
                try {
                    fileStorageService.delete(f.getFileId());
                } catch (Exception e) {
                    log.warn("彻底删除：附件物理文件删除失败 fileId={}", f.getFileId(), e);
                }
            }
            if (f.getFilePath() != null && !f.getFilePath().isBlank()) {
                try {
                    Files.deleteIfExists(Paths.get(properties.getAttachmentPath(), f.getFilePath()));
                } catch (IOException e) {
                    log.warn("彻底删除：历史本地附件删除失败 path={}", f.getFilePath(), e);
                }
            }
        }
        imageFileMapper.delete(Wrappers.<DocImageFile>lambdaQuery().eq(DocImageFile::getDocId, docId));
        // 2) 正文/互动/共享/日志等从属数据
        contentMapper.delete(Wrappers.<DocDetailContent>lambdaQuery()
            .eq(DocDetailContent::getDocId, docId));
        replyMapper.delete(Wrappers.<DocReply>lambdaQuery()
            .eq(DocReply::getDocId, docId));
        praiseMapper.delete(Wrappers.<DocPraise>lambdaQuery()
            .eq(DocPraise::getDocId, docId));
        scoreMapper.delete(Wrappers.<DocScore>lambdaQuery()
            .eq(DocScore::getDocId, docId));
        shareMapper.delete(Wrappers.<DocShare>lambdaQuery()
            .eq(DocShare::getDocId, docId));
        detailLogMapper.delete(Wrappers.<DocDetailLog>lambdaQuery()
            .eq(DocDetailLog::getDocId, docId));
        downloadLogMapper.delete(Wrappers.<DownloadLog>lambdaQuery()
            .eq(DownloadLog::getDocId, docId));
        // 3) 主表：物理删除（彻底删除的语义）
        int n = detailMapper.purgePhysically(docId);
        // 4) 回收站记录（物理删除，回收站登记簿不保留已彻底删除的痕迹）
        recycleMapper.deletePhysicallyByDocId(docId);
        log.info("彻底删除文档 docId={} purged={}", docId, n);
        return n > 0;
    }
}
