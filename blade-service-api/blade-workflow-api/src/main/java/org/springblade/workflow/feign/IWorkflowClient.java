package org.springblade.workflow.feign;

import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.dto.StartProcessDTO;
import org.springblade.workflow.vo.FormBindingVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 审批流程 Feign 客户端接口
 *
 * <p><b>服务名</b>：统一为 {@code blade-workflow}（常量 {@link WorkflowConstant#APPLICATION_WORKFLOW_NAME}）；
 * 严禁硬编码旧的 {@code blade-flow}（会导致服务发现失败）。</p>
 *
 * <p><b>路径约定</b>：Cloud 版约定（CLAUDE.md §6.3）后端 Controller 直接使用<b>资源路径</b>，
 * 由网关按服务名路由（{@code Path=/blade-workflow/**} + {@code StripPrefix=1}），
 * 前端经网关以 {@code /api/blade-workflow/...} 访问（Umi dev proxy 会剥离 {@code /api}）。
 * Feign 经服务发现直连服务、<b>不经网关</b>，故此处同样使用资源路径，不带 {@code /api} 前缀。</p>
 *
 * <p><b>契约收敛</b>：仅保留跨服务必需的 {@code startProcess}，对齐 {@code POST /instance/start}；
 * 待办/已办/审批等接口由前端经网关直连，不再经 Feign。</p>
 */
@FeignClient(
    value = WorkflowConstant.APPLICATION_WORKFLOW_NAME,
    fallback = IWorkflowClientFallback.class
)
public interface IWorkflowClient {

    /**
     * 发起流程实例
     *
     * <p>⚠️ 返回值为<b>字符串形式的实例ID</b>，与 {@code POST /instance/start} 保持一致：
     * 19 位雪花 ID 以 JSON 数字传输会被 JS 丢弃精度。</p>
     *
     * @param dto 发起参数（formId + dataId + 流程定义 + 表单字段值）
     * @return 流程实例ID（wf_instance.id，字符串）
     */
    @PostMapping("/instance/start")
    R<String> startProcess(@RequestBody StartProcessDTO dto);

    /**
     * 查询表单被流程绑定的情况（流程定义 + 流程实例）。
     *
     * <p>供 blade-formmode 删除表单前校验：已被流程设计绑定的表单不允许删除。</p>
     *
     * @param formId 表单ID（workflow_bill.id）
     * @return 绑定数量与流程名称，bound=true 表示已被占用
     */
    @GetMapping("/definition/form-binding/{formId}")
    R<FormBindingVO> getFormBinding(@PathVariable("formId") Long formId);

}
