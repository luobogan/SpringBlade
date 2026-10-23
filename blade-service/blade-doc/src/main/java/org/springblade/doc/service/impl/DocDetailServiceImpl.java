package org.springblade.doc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.entity.DocDetailContent;
import org.springblade.doc.entity.DocSecCategory;
import org.springblade.doc.enums.DocStatus;
import org.springblade.doc.mapper.DocDetailContentMapper;
import org.springblade.doc.mapper.DocDetailMapper;
import org.springblade.doc.mapper.DocSecCategoryMapper;
import org.springblade.doc.service.DocPermissionService;
import org.springblade.doc.service.IDocDetailService;
import org.springblade.workflow.dto.StartProcessDTO;
import org.springblade.workflow.feign.IWorkflowClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 文档详情/保存/提交/审批回调实现。
 *
 * <p><b>DOC-P0-3 修复</b>：更新路径保留<b>显式</b> {@code canManage} 校验（标注禁止注释）。
 * <b>流程审批集成</b>：目录开启审批({@code isOpenApproveWf=1})且绑定工作流时，提交即经
 * {@link IWorkflowClient} 发起流程并置「审批中(3)」，回填实例ID；发起失败 fail-closed（绝不静默置生效）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocDetailServiceImpl implements IDocDetailService {

    private final DocDetailMapper detailMapper;
    private final DocSecCategoryMapper categoryMapper;
    private final DocDetailContentMapper contentMapper;
    private final DocPermissionService docPermissionService;
    private final IWorkflowClient workflowClient;

    @Override
    public DocDetail getById(Long id) {
        return detailMapper.selectById(id);
    }

    @Override
    public boolean canRead(Long id) {
        return docPermissionService.canRead(id);
    }

    @Override
    public DocDetail saveDoc(DocDetail doc) {
        if (doc == null) {
            throw new ServiceException("文档不能为空");
        }
        Long userId = SecureUtil.getUserId();
        if (doc.getId() == null) {
            doc.setDocStatus(DocStatus.DRAFT.getCode());
            doc.setCreateUser(userId);
            detailMapper.insert(doc);
        } else {
            // ★ DOC-P0-3：更新必须显式编辑权校验（E9 曾整段注释 canEdit 致越权写）。严禁注释/绕过本判断。
            if (!docPermissionService.canManage(doc.getId())) {
                throw new ServiceException("无权限编辑该文档");
            }
            detailMapper.updateById(doc);
        }
        return doc;
    }

    @Override
    public DocDetail submit(Long id) {
        // ★ DOC-P0-3：提交同样显式编辑权校验
        if (!docPermissionService.canManage(id)) {
            throw new ServiceException("无权限提交该文档");
        }
        DocDetail doc = detailMapper.selectById(id);
        if (doc == null) {
            throw new ServiceException("文档不存在");
        }
        // 审批流：目录开启审批且绑定工作流 → 发起流程，置「审批中(3)」，回填实例ID
        DocSecCategory cat = doc.getSecCategoryId() == null ? null : categoryMapper.selectById(doc.getSecCategoryId());
        if (cat != null && Integer.valueOf(1).equals(cat.getIsOpenApproveWf()) && cat.getApproveWorkflowId() != null) {
            StartProcessDTO dto = new StartProcessDTO();
            dto.setDefId(cat.getApproveWorkflowId());
            dto.setStarter(SecureUtil.getUserId());
            dto.setTitle("DOC:" + (doc.getDocSubject() == null ? "" : doc.getDocSubject()));
            dto.setDataId(id);
            Map<String, Object> vars = new HashMap<>();
            vars.put("docId", id);
            vars.put("docSubject", doc.getDocSubject());
            dto.setVariables(vars);
            R<String> r = workflowClient.startProcess(dto);
            if (r == null || !r.isSuccess() || r.getData() == null) {
                // fail-closed：发起失败不得静默置生效
                throw new ServiceException("发起审批流程失败：" + (r == null ? "无响应" : r.getMsg()));
            }
            doc.setApproveInstId(Long.valueOf(r.getData()));
            doc.setDocStatus(DocStatus.APPROVING.getCode());
            detailMapper.updateById(doc);
            log.info("文档提交审批成功 docId={} instId={}", id, r.getData());
            return doc;
        }
        // 无审批：草稿/退回 → 生效
        doc.setDocStatus(DocStatus.EFFECTIVE.getCode());
        detailMapper.updateById(doc);
        return doc;
    }

    @Override
    public void onApprovalCompleted(Long instId, boolean approved) {
        if (instId == null) {
            return;
        }
        // 回调无租户上下文：按唯一 approveInstId 查询/更新（忽略租户行）
        DocDetail doc = detailMapper.selectByApproveInstId(instId);
        if (doc == null) {
            log.warn("审批回调未找到文档 instId={}", instId);
            return;
        }
        int status = approved ? DocStatus.EFFECTIVE_APPROVED.getCode() : DocStatus.RETURNED.getCode();
        int rows = detailMapper.updateStatusByApproveInstId(instId, status);
        log.info("审批回调更新文档 docId={} instId={} approved={} status={} rows={}",
            doc.getId(), instId, approved, status, rows);
    }

    @Override
    public DocDetail publish(Long id) {
        return transition(id, DocStatus.EFFECTIVE_APPROVED,
            DocStatus.DRAFT, DocStatus.EFFECTIVE, DocStatus.PENDING_PUBLISH, DocStatus.RETURNED);
    }

    @Override
    public DocDetail invalidate(Long id) {
        return transition(id, DocStatus.INVALID,
            DocStatus.EFFECTIVE, DocStatus.EFFECTIVE_APPROVED, DocStatus.ARCHIVED);
    }

    @Override
    public DocDetail archive(Long id) {
        return transition(id, DocStatus.ARCHIVED,
            DocStatus.EFFECTIVE, DocStatus.EFFECTIVE_APPROVED, DocStatus.INVALID);
    }

    @Override
    public DocDetail reopen(Long id) {
        return transition(id, DocStatus.EFFECTIVE,
            DocStatus.INVALID, DocStatus.ARCHIVED, DocStatus.VOID);
    }

    @Override
    public DocDetail voidDoc(Long id) {
        return transition(id, DocStatus.VOID,
            DocStatus.DRAFT, DocStatus.EFFECTIVE, DocStatus.EFFECTIVE_APPROVED,
            DocStatus.RETURNED, DocStatus.PENDING_PUBLISH);
    }

    /**
     * 文档状态流转（发布/失效/归档/重新打开/作废）：显式 canManage + 允许的起始状态校验 + fail-closed。
     */
    private DocDetail transition(Long id, DocStatus target, DocStatus... allowedFrom) {
        if (!docPermissionService.canManage(id)) {
            throw new ServiceException("无权限操作该文档");
        }
        DocDetail doc = detailMapper.selectById(id);
        if (doc == null) {
            throw new ServiceException("文档不存在");
        }
        int cur = doc.getDocStatus() == null ? DocStatus.DRAFT.getCode() : doc.getDocStatus();
        boolean ok = allowedFrom.length == 0;
        for (DocStatus s : allowedFrom) {
            if (s.getCode() == cur) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            throw new ServiceException("当前状态不允许该操作");
        }
        doc.setDocStatus(target.getCode());
        detailMapper.updateById(doc);
        log.info("文档状态流转 docId={} {} -> {}", id, cur, target.getCode());
        return doc;
    }

    @Override
    public String getContent(Long docId) {
        if (!docPermissionService.canRead(docId)) {
            throw new ServiceException("无权限查看该文档正文");
        }
        DocDetailContent c = contentMapper.selectOne(Wrappers.<DocDetailContent>lambdaQuery()
            .eq(DocDetailContent::getDocId, docId).last("LIMIT 1"));
        return (c == null || c.getContent() == null) ? "" : c.getContent();
    }

    @Override
    public void saveContent(Long docId, String content) {
        if (!docPermissionService.canManage(docId)) {
            throw new ServiceException("无权限编辑该文档正文");
        }
        DocDetailContent c = contentMapper.selectOne(Wrappers.<DocDetailContent>lambdaQuery()
            .eq(DocDetailContent::getDocId, docId).last("LIMIT 1"));
        if (c == null) {
            c = new DocDetailContent();
            c.setDocId(docId);
            c.setContent(content);
            contentMapper.insert(c);
        } else {
            c.setContent(content);
            contentMapper.updateById(c);
        }
        log.info("保存文档正文 docId={} len={}", docId, content == null ? 0 : content.length());
    }

    @Override
    public DocDetail checkOut(Long id) {
        if (!docPermissionService.canManage(id)) {
            throw new ServiceException("无权限签出该文档");
        }
        DocDetail doc = detailMapper.selectById(id);
        if (doc == null) {
            throw new ServiceException("文档不存在");
        }
        if (doc.getCheckOutStatus() != null && doc.getCheckOutStatus() == 1) {
            throw new ServiceException("文档已被签出");
        }
        Long userId = SecureUtil.getUserId();
        doc.setCheckOutStatus(1);
        doc.setCheckOutUserId(userId);
        detailMapper.updateById(doc);
        log.info("文档签出 docId={} userId={}", id, userId);
        return doc;
    }

    @Override
    public DocDetail checkIn(Long id) {
        DocDetail doc = detailMapper.selectById(id);
        if (doc == null) {
            throw new ServiceException("文档不存在");
        }
        Long userId = SecureUtil.getUserId();
        boolean isChecker = doc.getCheckOutUserId() != null && doc.getCheckOutUserId().equals(userId);
        if (!isChecker && !docPermissionService.canManage(id)) {
            throw new ServiceException("仅签出本人或文档管理者可签入");
        }
        doc.setCheckOutStatus(0);
        doc.setCheckOutUserId(-1L);
        detailMapper.updateById(doc);
        log.info("文档签入 docId={} userId={}", id, userId);
        return doc;
    }

    @Override
    public DocDetail forceCheckIn(Long id) {
        BladeUser user = SecureUtil.getUser();
        if (user == null || !hasRole(user, RoleConstant.ADMINISTRATOR, DocConstant.ROLE_DOC)) {
            throw new ServiceException("强制签入需管理员或内容管理员权限");
        }
        DocDetail doc = detailMapper.selectById(id);
        if (doc == null) {
            throw new ServiceException("文档不存在");
        }
        doc.setCheckOutStatus(0);
        doc.setCheckOutUserId(-1L);
        detailMapper.updateById(doc);
        log.info("文档强制签入 docId={} operator={}", id, SecureUtil.getUserId());
        return doc;
    }

    private boolean hasRole(BladeUser user, String... roles) {
        if (user.getRoleName() == null || user.getRoleName().trim().isEmpty()) {
            return false;
        }
        for (String r : user.getRoleName().split(",")) {
            for (String want : roles) {
                if (want.equals(r.trim())) {
                    return true;
                }
            }
        }
        return false;
    }
}
