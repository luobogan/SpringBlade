package org.springblade.workflow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.dto.FormSaveDTO;
import org.springblade.workflow.dto.StartProcessDTO;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.vo.ApprovalLogVO;
import org.springblade.workflow.vo.InstanceVO;
import org.springblade.workflow.vo.WfNodeOperatorVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 流程实例控制器
 *
 * <p>路径约定：Controller 使用<b>资源路径</b>，前端经网关以
 * {@code /api/blade-workflow/instance/...} 访问。</p>
 *
 * <p><b>鉴权</b>：发起 / 我的请求 / 详情 / 流转记录 / 节点操作者 / 快照 / 撤回 / 终止
 * 都是<b>全体员工</b>走流程的必经能力，因此类上只要求「已登录」
 * （{@link WorkflowConstant#HAS_AUTH}），不再要求 {@code workflow} 角色。
 * 运行期控制中偏运维的两个动作（恢复 {@code /resume}、作废 {@code /cancel}）
 * 仍保留流程管理员角色门（方法级注解覆盖类级）。</p>
 *
 * <p>「能不能动<b>这一条</b>」由服务层记录级校验兜底（{@code WfInstanceServiceImpl}
 * 的 {@code requireVisible} / {@code canVisible}）：读取限发起人、参与人、管理员；
 * 撤回与终止限发起人或管理员。</p>
 */
@RestController
@RequestMapping("/instance")
@PreAuth(WorkflowConstant.HAS_AUTH)
@RequiredArgsConstructor
@Tag(name = "流程实例", description = "流程发起、实例查询、流转记录与运行期控制")
public class WfInstanceController {

    private final IWfInstanceService instanceService;

    @PostMapping("/start")
    @Operation(summary = "发起流程", description = "formId + dataId + 流程定义；写表单数据快照并生成首个待办")
    public R<String> start(@RequestBody StartProcessDTO dto) {
        // ⚠️ 实例ID以<b>字符串</b>返回：19 位雪花 ID 超出 JS 安全整数（2^53），
        //    以 JSON 数字返回会被前端解析时丢精度（如 ...538 → ...500），
        //    后续拿它查实例就会「流程实例不存在」。跨服务 Feign（IWorkflowClient）同样按 String 收敛。
        return R.data(String.valueOf(instanceService.start(dto)), "发起成功");
    }

    @PostMapping("/save-draft")
    @Operation(summary = "保存草稿", description = "只存不流转：创建/更新草稿实例与发起人待办，返回草稿实例ID（可再次保存复用）")
    public R<String> saveDraft(@RequestBody FormSaveDTO dto) {
        // ⚠️ 同 start：实例ID以字符串返回，避免 19 位雪花 ID 前端丢精度。
        return R.data(String.valueOf(instanceService.saveDraft(dto)), "已保存草稿");
    }

    @GetMapping("/{id}")
    @Operation(summary = "实例详情", description = "含当前节点与状态")
    public R<InstanceVO> detail(@PathVariable("id") Long id) {
        return R.data(instanceService.detail(id));
    }

    @GetMapping("/mine")
    @Operation(summary = "我的请求",
        description = "我发起的流程实例分页（发起人=当前登录人，可按标题模糊）；「我的请求」页签数据源")
    public R<IPage<InstanceVO>> mine(
        @Parameter(description = "当前页，从 1 开始") @RequestParam(value = "current", required = false) Long current,
        @Parameter(description = "每页条数，默认 20") @RequestParam(value = "pageSize", required = false) Long pageSize,
        @Parameter(description = "流程标题，模糊匹配") @RequestParam(value = "title", required = false) String title) {
        return R.data(instanceService.mine(current, pageSize, title));
    }

    @GetMapping("/by-biz")
    @Operation(summary = "按业务数据反查实例")
    public R<InstanceVO> byBiz(
        @Parameter(description = "表单ID") @RequestParam("formId") Long formId,
        @Parameter(description = "业务数据ID") @RequestParam("dataId") Long dataId) {
        return R.data(instanceService.getByBiz(formId, dataId));
    }

    @GetMapping("/{id}/logs")
    @Operation(summary = "流转/审批记录")
    public R<List<ApprovalLogVO>> logs(@PathVariable("id") Long id) {
        return R.data(instanceService.logs(id));
    }

    @GetMapping("/{id}/node-operators")
    @Operation(summary = "节点操作者情况",
        description = "流程图节点悬浮「操作者」面板数据：按节点返回已操作 / 已查看 / 未操作人员ID（姓名由前端人员字典解析）")
    public R<java.util.Map<String, WfNodeOperatorVO>> nodeOperators(@PathVariable("id") Long id) {
        return R.data(instanceService.nodeOperators(id));
    }

    @GetMapping("/{id}/snapshot/{nodeKey}")
    @Operation(summary = "表单数据快照")
    public R<String> snapshot(@PathVariable("id") Long id,
                              @PathVariable("nodeKey") String nodeKey) {
        return R.data(instanceService.snapshot(id, nodeKey));
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "撤回")
    public R<Boolean> withdraw(@PathVariable("id") Long id,
                               @RequestParam(value = "opinion", required = false) String opinion) {
        return R.data(instanceService.withdraw(id, opinion), "已撤回");
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "终止/暂停",
        description = "「我的请求」的终止按钮：发起人本人或流程管理员可操作，暂停后不再推进")
    public R<Boolean> stop(@PathVariable("id") Long id) {
        return R.data(instanceService.stop(id), "已暂停");
    }

    @PostMapping("/{id}/resume")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    @Operation(summary = "恢复", description = "运行期运维动作：仅流程管理员")
    public R<Boolean> resume(@PathVariable("id") Long id) {
        return R.data(instanceService.resume(id), "已恢复");
    }

    @PostMapping("/{id}/cancel")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    @Operation(summary = "撤销（作废）", description = "运行期运维动作：仅流程管理员")
    public R<Boolean> cancel(@PathVariable("id") Long id,
                             @RequestParam(value = "opinion", required = false) String opinion) {
        return R.data(instanceService.cancel(id, opinion), "已撤销");
    }

}
