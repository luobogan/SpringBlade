package org.springblade.formmode.feign;

import org.springblade.core.tool.api.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 工作流服务 Feign 客户端
 *
 * 用于表单建模审批触发时调用工作流服务创建审批实例
 * 对应 ecology 的 ModeDataApproval.createWorkflowRequest()
 */
@FeignClient(
    value = "blade-flow",  // 假设工作流服务名
    fallback = IWorkflowClientFallback.class
)
public interface IWorkflowClient {

    /**
     * 创建审批请求
     */
    @PostMapping("/api/blade-flow/workflow/request/create")
    R<Long> createWorkflowRequest(@RequestParam("workflowId") Integer workflowId,
                                  @RequestParam("dataId") Long dataId,
                                  @RequestBody Map<String, Object> fieldValues);

}

/**
 * 工作流客户端降级实现
 */
class IWorkflowClientFallback implements IWorkflowClient {

    @Override
    public R<Long> createWorkflowRequest(Integer workflowId, Long dataId, Map<String, Object> fieldValues) {
        return R.fail("工作流服务不可用");
    }

}
