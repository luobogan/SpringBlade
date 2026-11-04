package org.springblade.workflow.freeflow;

import lombok.Data;

import java.io.Serializable;

/**
 * 自由流程推进结果。
 */
@Data
public class FreeAdvanceResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否推进到下一步（false=已到末步，流程归档） */
    private boolean advanced;

    /** 下一步绑定的池内静态节点 key */
    private String nextNodeKey;

    /** 下一步办理人数量 */
    private int assigneeCount;

    /** 是否已结束 */
    private boolean finished;

    public static FreeAdvanceResult finished() {
        FreeAdvanceResult r = new FreeAdvanceResult();
        r.setFinished(true);
        return r;
    }

    public static FreeAdvanceResult advanced(String nodeKey, int count) {
        FreeAdvanceResult r = new FreeAdvanceResult();
        r.setAdvanced(true);
        r.setNextNodeKey(nodeKey);
        r.setAssigneeCount(count);
        return r;
    }
}
