package org.springblade.workflow.feign;

import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.dto.StartProcessDTO;
import org.springblade.workflow.vo.DefinitionKeyTenantVO;
import org.springblade.workflow.vo.FormBindingVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springblade.workflow.vo.WfTaskVO;

import java.util.List;

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
 * <p><b>契约收敛</b>：仅保留跨服务必需的 {@code startProcess}，对齐内部端点
 * {@code POST /feign/client/instance/start}（无鉴权，服务间直连；对外带鉴权的仍是 {@code POST /instance/start}）；
 * 路径采用 {@code /feign/client} 前缀，以便网关 {@code InnerFilter} 拒绝一切经网关的外部访问；
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
    @PostMapping("/feign/client/instance/start")
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

    /**
     * 由流程定义ID（wf_process_definition.id）查询其 Flowable 引擎身份（procKey + tenantId）。
     *
     * <p>「以 Flowable 为唯一事实源」改造：用于把 mode_triggerworkflowset.workflowid（旧 defId）
     * 回填为 workflow_key（procKey）。仅需已登录（HAS_AUTH），由调用方所在租户上下文透传。</p>
     *
     * @param defId 流程定义ID（wf_process_definition.id）
     * @return procKey + tenantId；defId 不存在返回 null
     */
    @GetMapping("/definition/key-tenant/{defId}")
    R<DefinitionKeyTenantVO> getDefinitionKeyAndTenant(@PathVariable("defId") Long defId);

    /**
     * 查询流程实例是否已结束（内部端点，无鉴权）
     *
     * <p><b>用途</b>：发起侧竞态兜底。某些场景（无表单 / 节点被自动推进）流程会在
     * {@code startProcess} <b>内部就同步走完</b>，{@code PROCESS_COMPLETED} 回调随之在调用方
     * 回填业务关联（如把实例ID写回流转记录）之前发生，导致回调按实例ID查不到记录而空跑。
     * 调用方回填后再用本接口确认一次，若已结束则就地补办回调（服务端幂等）。</p>
     *
     * @param id 流程实例ID（wf_instance.id）
     * @return 是否已结束（endTime 非空）
     */
    @GetMapping("/feign/client/instance/ended/{id}")
    R<Boolean> isEnded(@PathVariable("id") Long id);

    /**
     * 查询指定办理人的待办列表（门户待办元素专供内部端点）
     *
     * <p><b>为什么待办/已办已由前端经网关直连，这里还要开内部端点</b>：
     * 配置驱动门户的待办卡片数据由 blade-portal 服务端聚合后随元素数据一并返回
     * （见 {@code POST /element-data/batch}）。若让前端自己取待办，
     * 则每张卡片一次请求、且鉴权/条数个性化逻辑会散落到前端，
     * 违背「服务端聚合、单元素失败不拖垮整页」的约定。</p>
     *
     * <p>本端点无 {@code @PreAuth}（内部 Feign 经服务发现直连、不带用户令牌），
     * 路径以 {@code /feign/client} 前缀隔离，网关 {@code InnerFilter} 拒绝外部访问；
     * 办理人由调用方以 {@code assignee} 显式传入（门户传当前登录用户ID）。</p>
     *
     * @param assignee 办理人用户ID
     * @param limit    最多返回条数（为空取服务默认值）
     * @return 待办列表
     */
    @GetMapping("/feign/client/task/todo")
    R<List<WfTaskVO>> todoList(@RequestParam("assignee") Long assignee,
                               @RequestParam(value = "limit", required = false) Integer limit);

}
