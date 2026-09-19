package org.springblade.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.dto.ValidateDTO;
import org.springblade.workflow.service.IWfFormRenderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审批态渲染 / 表单校验控制器
 *
 * <p>对应文档 §6.4：</p>
 * <ul>
 *   <li>{@code GET /form/render}：一次返回「布局 + 业务数据 + 节点字段权限 + 明细权限」，
 *       把表单侧与流程侧的耦合收敛在此，避免前端多次往返（决策 5：渲染包）。</li>
 *   <li>{@code POST /form/validate}：按节点必填矩阵做服务端复核（前端校验不可信）。</li>
 * </ul>
 *
 * <p><b>角色鉴权</b>：渲染包会读取表单布局（跨服务调 formmode）与节点权限，属受控能力，
 * 统一要求 {@code administrator} 角色（formmode 的 {@code FormLayoutController} 亦为同一角色）。
 * 跨服务 Feign 调用由 {@code BladeFeignRequestHeaderInterceptor} 透传当前 token，
 * 故同一角色在 workflow 与 formmode 两侧均可通过鉴权（鉴权链路打通）。</p>
 */
@RestController
@RequestMapping("/form")
@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
@RequiredArgsConstructor
@Tag(name = "审批态渲染", description = "渲染包与服务端校验")
public class WfFormRenderController {

    private final IWfFormRenderService formRenderService;

    @GetMapping("/render")
    @Operation(summary = "审批态渲染包", description = "布局 + 业务数据 + 节点字段权限 + 明细权限；taskId 为空则只读")
    public R<org.springblade.workflow.vo.FormRenderVO> render(
        @Parameter(description = "流程实例ID") @RequestParam("instanceId") Long instanceId,
        @Parameter(description = "任务ID（待办渲染时传入，用于判定读写态）") @RequestParam(value = "taskId", required = false) Long taskId,
        @Parameter(description = "指定渲染节点Key（测试页直显某节点布局；为空取当前/任务节点）") @RequestParam(value = "nodeKey", required = false) String nodeKey) {
        return R.data(formRenderService.render(instanceId, taskId, nodeKey));
    }

    @GetMapping("/preview")
    @Operation(summary = "表单预览（无需实例）", description = "按流程定义/表单/节点返回布局+字段权限+操作菜单，用于测试页/设计页预览，不创建实例")
    public R<org.springblade.workflow.vo.FormRenderVO> preview(
        @Parameter(description = "流程定义ID") @RequestParam(value = "defId", required = false) Long defId,
        @Parameter(description = "表单ID") @RequestParam(value = "formId", required = false) Long formId,
        @Parameter(description = "节点Key") @RequestParam(value = "nodeKey", required = false) String nodeKey) {
        return R.data(formRenderService.preview(defId, formId, nodeKey));
    }

    @PostMapping("/validate")
    @Operation(summary = "服务端校验", description = "按节点必填矩阵复核，必填字段缺失时返回错误信息")
    public R<Boolean> validate(@RequestBody ValidateDTO dto) {
        return R.data(formRenderService.validate(dto), "校验通过");
    }

}
