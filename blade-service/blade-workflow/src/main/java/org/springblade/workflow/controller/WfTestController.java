package org.springblade.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.dto.WfTestRunDTO;
import org.springblade.workflow.entity.WfTestLog;
import org.springblade.workflow.service.IWfTestService;
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

/**
 * 流程测试控制器（设计期校验）
 *
 * <p>对齐 ecology「流程测试」：指定发起人走查路径 → 逐节点解析操作者 → 产出测试日志。
 * 前端入口为流程设计页的「流程测试」按钮。</p>
 *
 * <p>路径约定同 {@code WfDefinitionController}：网关以
 * {@code /api/blade-workflow/test/...} 暴露。</p>
 */
@RestController
@RequestMapping("/test")
@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
@RequiredArgsConstructor
@Tag(name = "流程测试", description = "设计期流程测试：走查路径、解析操作者、产出测试日志")
public class WfTestController {

    private final IWfTestService testService;

    @PostMapping("/run")
    @Operation(summary = "运行流程测试",
        description = "指定测试发起人从创建节点走查路径，逐节点解析操作者并记录日志；结果落库 wf_test_log")
    public R<WfTestResultVO> run(@RequestBody WfTestRunDTO dto) {
        return R.data(testService.run(dto), "测试完成");
    }

    @GetMapping("/list")
    @Operation(summary = "测试历史列表", description = "按流程定义查最近的测试记录（最多 100 条）")
    public R<List<WfTestLog>> list(@RequestParam(value = "defId", required = false) Long defId) {
        return R.data(testService.list(defId));
    }

    @PostMapping("/cleanup")
    @Operation(summary = "清理测试数据",
        description = "删除所有测试态（is_test=1）的实例/待办/日志/快照，并级联卸载测试部署（清掉 ACT_* 数据）；"
            + "defId 为空时清理全部测试数据")
    public R<Long> cleanup(@RequestParam(value = "defId", required = false) Long defId) {
        return R.data(testService.cleanupTestData(defId), "已清理测试数据");
    }

    @GetMapping("/{id}")
    @Operation(summary = "测试详情", description = "含日志正文（logContent）与结构化结果（resultJson）")
    public R<WfTestLog> detail(@PathVariable("id") Long id) {
        return R.data(testService.detail(id));
    }

    @DeleteMapping
    @Operation(summary = "删除测试记录")
    public R<Boolean> remove(@RequestParam("ids") List<Long> ids) {
        return R.data(testService.remove(ids));
    }
}
