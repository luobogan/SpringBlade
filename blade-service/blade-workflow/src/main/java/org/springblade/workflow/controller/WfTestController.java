package org.springblade.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.dto.WfTestRunDTO;
import org.springblade.workflow.dto.WfTestStepDTO;
import org.springblade.workflow.entity.WfTestLog;
import org.springblade.workflow.service.IWfTestService;
import org.springblade.workflow.vo.WfTaskVO;
import org.springblade.workflow.vo.WfTestResultVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 流程测试控制器（设计期校验）
 *
 * <p>对齐 ecology「流程测试」：指定发起人走查路径 → 逐节点解析操作者 → 产出测试日志。
 * 前端入口为流程设计页的「流程测试」按钮。</p>
 *
 * <p>路径约定同 {@code WfDefinitionController}：网关以
 * {@code /api/blade-workflow/test/...} 暴露。</p>
 *
 * <p><b>角色门按方法声明</b>（方案 §6.4 C12）——类级 {@code HAS_ROLE_WORKFLOW} 已下沉到方法级：</p>
 * <ul>
 *   <li><b>管理面</b>（{@code run}/{@code start}/{@code step}/{@code shadow}/{@code list}/
 *       {@code detail}/{@code remove}/{@code cleanup}）：保留 {@code HAS_ROLE_WORKFLOW}；</li>
 *   <li><b>办理面</b>（{@code state}/{@code todo}/{@code my-todo}/{@code approve}）：放开为
 *       {@code HAS_AUTH}（登录即可），由服务层记录级鉴权兜底 —— 断言目标是测试实例
 *       （{@code is_test=1}）且（真人模式下）本人是该待办的执行人。
 *       这是「用相应用户审批」的通道：节点操作者本人不带 {@code workflow} 角色也能办理测试单。</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Tag(name = "流程测试", description = "设计期流程测试：走查路径、解析操作者、产出测试日志")
public class WfTestController {

    private final IWfTestService testService;

    @PostMapping("/run")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    @Operation(summary = "运行流程测试",
        description = "指定测试发起人从创建节点走查路径，逐节点解析操作者并记录日志；结果落库 wf_test_log")
    public R<WfTestResultVO> run(@RequestBody WfTestRunDTO dto) {
        return R.data(testService.run(dto), "测试完成");
    }

    @PostMapping("/shadow")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    @Operation(summary = "影子比对（新旧版本）",
        description = "把同一套测试分别在「对照版本（旧）」与「目标版本（新）」上各跑一遍，"
            + "比对流转路径 / 节点经过次数 / 覆盖率，作为新版本与旧版本行为是否等价的上线依据；"
            + "跑完只清理本次比对产生的测试实例，不动既有测试历史")
    public R<Map<String, Object>> shadow(@RequestParam("defId") Long defId,
                                         @RequestParam(value = "baseDefId", required = false) Long baseDefId,
                                         @RequestParam(value = "testUserId", required = false) Long testUserId) {
        Map<String, Object> result = testService.shadowCompare(defId, baseDefId, testUserId);
        // 管理面审计：影子比对会在引擎上真实跑两个版本并自动清理，需留痕以便复盘
        log.info("[blade-workflow][审计] 影子比对. targetDefId={}, baseDefId={}, 结论等价={}, operator={}",
            defId, baseDefId, result.get("identical"), SecureUtil.getUserId());
        return R.data(result, "比对完成");
    }

    @PostMapping("/start")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    @Operation(summary = "发起交互式测试",
        description = "临时部署草稿流程并真实发起测试态实例，但不自动推进；随后可「开始自动测试」逐节点推进或手动提交")
    public R<WfTestResultVO> start(@RequestBody WfTestRunDTO dto) {
        return R.data(testService.start(dto), "测试已发起");
    }

    @PostMapping("/step")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    @Operation(summary = "交互式测试-单步推进",
        description = "提交当前节点待办推进一步；自动测试由前端循环调用（可暂停），手动测试由用户点「提交」调用一次")
    public R<WfTestResultVO> step(@RequestBody WfTestStepDTO dto) {
        return R.data(testService.step(dto), "已提交");
    }

    @GetMapping("/state")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "交互式测试-查询状态",
        description = "返回当前节点/待办/节点经过次数/出口覆盖/逐行日志；测试入口（含真人模式）可用，服务层断言 is_test=1")
    public R<WfTestResultVO> state(@RequestParam("instId") Long instId) {
        return R.data(testService.state(instId));
    }

    @GetMapping("/todo")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "交互式测试-待办列表", description = "测试态实例的待办（供手动办理定位任务ID）；服务层断言 is_test=1")
    public R<List<WfTaskVO>> todo(@RequestParam("instId") Long instId) {
        return R.data(testService.todo(instId));
    }

    @GetMapping("/my-todo")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "我的测试待办（真人模式）",
        description = "当前登录人在测试实例上的待办（is_test=1 且 status=待办）；"
            + "供「用相应用户审批」的节点操作者本人从测试入口办理（方案 §6.4 C12）")
    public R<List<WfTaskVO>> myTodo() {
        return R.data(testService.myTodo());
    }

    @PostMapping("/approve")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "真人办理测试待办",
        description = "断言目标为测试实例、且（非管理员时）当前用户是该待办的执行人；提交语义与 /step 一致")
    public R<WfTestResultVO> approve(@RequestBody WfTestStepDTO dto) {
        return R.data(testService.approve(dto), "已提交");
    }

    @GetMapping("/list")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    @Operation(summary = "测试历史列表", description = "按流程定义查最近的测试记录（最多 100 条）")
    public R<List<WfTestLog>> list(@RequestParam(value = "defId", required = false) Long defId) {
        return R.data(testService.list(defId));
    }

    @PostMapping("/cleanup")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    @Operation(summary = "清理测试数据",
        description = "删除所有测试态（is_test=1）的实例/待办/日志/快照，并级联卸载测试部署（清掉 ACT_* 数据）；"
            + "defId 为空时清理全部测试数据")
    public R<Long> cleanup(@RequestParam(value = "defId", required = false) Long defId) {
        Long cleaned = testService.cleanupTestData(defId);
        // 管理面审计（方案 §8 P3）：清理是破坏性动作（含业务行/测试历史），必须留「谁在何时清了哪个流程」
        log.info("[blade-workflow][审计] 清理测试数据. defId={}, 清理实例数={}, operator={}",
            defId, cleaned, SecureUtil.getUserId());
        return R.data(cleaned, "已清理测试数据");
    }

    @GetMapping("/{id}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    @Operation(summary = "测试详情", description = "含日志正文（logContent）与结构化结果（resultJson）")
    public R<WfTestLog> detail(@PathVariable("id") Long id) {
        return R.data(testService.detail(id));
    }

    @DeleteMapping
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    @Operation(summary = "删除测试记录")
    public R<Boolean> remove(@RequestParam("ids") List<Long> ids) {
        return R.data(testService.remove(ids));
    }
}
