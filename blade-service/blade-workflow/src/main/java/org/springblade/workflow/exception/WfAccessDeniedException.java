package org.springblade.workflow.exception;

import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.api.ResultCode;

/**
 * 流程越权异常：HTTP 403 + 明确的中文原因。
 *
 * <p>服务层记录级校验失败时抛出（例如「你不是该任务的办理人」「你不是该流程的发起人」）。
 * 用 {@link ResultCode#REQ_REJECT}（403 请求被拒绝）而不是默认的 400，
 * 便于前端区分「参数/业务规则不满足」与「无权操作这条数据」。</p>
 *
 * <p>{@code BladeRestExceptionTranslator} 对 {@code ServiceException} 取
 * {@code R.fail(e.getResultCode(), e.getMessage())}，因此 {@code resultCode} 提供 403、
 * {@link #getMessage()} 提供可读原因，两者都会透给前端。</p>
 */
public class WfAccessDeniedException extends ServiceException {

    private static final long serialVersionUID = 1L;

    /**
     * 越权原因（透给前端展示，因此要写成人话，而不是「权限不足」这种泛化文案）
     */
    private final String reason;

    public WfAccessDeniedException(String reason) {
        super(ResultCode.REQ_REJECT);
        this.reason = reason;
    }

    @Override
    public String getMessage() {
        return reason;
    }

}
