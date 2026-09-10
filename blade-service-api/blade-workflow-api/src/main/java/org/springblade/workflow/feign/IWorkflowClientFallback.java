package org.springblade.workflow.feign;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.dto.StartProcessDTO;
import org.springframework.stereotype.Component;

/**
 * 审批流程 Feign 客户端降级实现
 */
@Slf4j
@Component
public class IWorkflowClientFallback implements IWorkflowClient {

    @Override
    public R<Long> startProcess(StartProcessDTO dto) {
        log.error("[blade-workflow] 发起流程失败，服务不可用. formId={}, dataId={}, procKey={}",
            dto == null ? null : dto.getFormId(),
            dto == null ? null : dto.getDataId(),
            dto == null ? null : dto.getProcKey());
        return R.fail("审批流程服务不可用");
    }

}
