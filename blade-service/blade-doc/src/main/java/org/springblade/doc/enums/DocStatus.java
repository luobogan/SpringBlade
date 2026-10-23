package org.springblade.doc.enums;

/**
 * 文档状态机（对齐 E9 DocManager.java:176 注释口径）。
 *
 * <p>0草稿 1生效(不需审批/归档重开) 2生效(审批或发布后) 3审批中 4退回 5归档 6待发布 7失效 8作废 9流程草稿。</p>
 */
public enum DocStatus {

    DRAFT(0),
    EFFECTIVE(1),
    EFFECTIVE_APPROVED(2),
    APPROVING(3),
    RETURNED(4),
    ARCHIVED(5),
    PENDING_PUBLISH(6),
    INVALID(7),
    VOID(8),
    FLOW_DRAFT(9);

    private final int code;

    DocStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
