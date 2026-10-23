package org.springblade.doc.service;

import org.springblade.doc.entity.DocDetail;

/**
 * 文档详情/保存/提交/审批回调服务。
 */
public interface IDocDetailService {

    DocDetail getById(Long id);

    boolean canRead(Long id);

    /** 保存草稿（更新时显式编辑权校验，修 DOC-P0-3） */
    DocDetail saveDoc(DocDetail doc);

    /** 提交：目录开启审批则发起流程(审批中)，否则生效 */
    DocDetail submit(Long id);

    /** 审批完成回调：通过→生效(2)，驳回→退回(4) */
    void onApprovalCompleted(Long instId, boolean approved);

    /** 发布：→ 生效(2) */
    DocDetail publish(Long id);

    /** 失效：生效 → 失效(7) */
    DocDetail invalidate(Long id);

    /** 归档：生效 → 归档(5) */
    DocDetail archive(Long id);

    /** 重新打开：失效/归档/作废 → 生效(1) */
    DocDetail reopen(Long id);

    /** 作废：→ 作废(8) */
    DocDetail voidDoc(Long id);

    /** 读取正文 HTML（canRead 校验） */
    String getContent(Long docId);

    /** 保存正文 HTML（canManage 校验） */
    void saveContent(Long docId, String content);

    /** 签出：置 checkOutStatus=1 并记录签出人（已被签出则拒绝） */
    DocDetail checkOut(Long id);

    /** 签入：仅签出本人/管理员可还原 */
    DocDetail checkIn(Long id);

    /** 强制签入：仅管理员/内容管理员，解除他人签出 */
    DocDetail forceCheckIn(Long id);
}
