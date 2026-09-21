package org.springblade.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.dto.WfCustomOperationFull;
import org.springblade.workflow.entity.WfCustomOperation;
import org.springblade.workflow.entity.WfNodeDefaultSign;
import org.springblade.workflow.service.IWfCustomOperationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 节点「自定义操作」控制器（对齐泛微节点信息「自定义操作」）。
 *
 * <p>按钮 + 动作明细（URL / 流程操作 / 接口）+ 权限矩阵；以及「按操作类型默认签字意见」。</p>
 */
@RestController
@RequestMapping("/custom-operation")
@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
@RequiredArgsConstructor
@Tag(name = "自定义操作", description = "自定义操作按钮/动作/权限与默认签字意见")
public class WfCustomOperationController {

    private final IWfCustomOperationService customOperationService;

    @GetMapping("/list")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "运行时可见按钮（启用）")
    public R<List<WfCustomOperation>> list(@RequestParam("defId") Long defId,
                                           @RequestParam("nodeKey") String nodeKey) {
        return R.data(customOperationService.listEnabled(defId, nodeKey));
    }

    @GetMapping("/full")
    @Operation(summary = "配置用：节点全部按钮（含动作明细与权限矩阵）")
    public R<List<WfCustomOperationFull>> full(@RequestParam("defId") Long defId,
                                               @RequestParam("nodeKey") String nodeKey) {
        return R.data(customOperationService.listFull(defId, nodeKey));
    }

    @PostMapping("/save")
    @Operation(summary = "保存节点自定义操作（覆盖式）")
    public R<Boolean> save(@RequestParam("defId") Long defId,
                           @RequestParam("nodeKey") String nodeKey,
                           @RequestBody List<WfCustomOperationFull> payload) {
        customOperationService.saveBatch(defId, nodeKey, payload);
        return R.status(true);
    }

    @PostMapping("/execute")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "运行时执行自定义操作")
    public R<Boolean> execute(@RequestParam("opId") Long opId,
                              @RequestParam("instId") Long instId,
                              @RequestParam(value = "operator", required = false) Long operator) {
        return customOperationService.execute(opId, instId, operator);
    }

    @GetMapping("/default-sign")
    @Operation(summary = "按操作类型默认签字意见")
    public R<String> defaultSign(@RequestParam("defId") Long defId,
                                 @RequestParam("nodeKey") String nodeKey,
                                 @RequestParam("menuType") String menuType) {
        return R.data(customOperationService.defaultSign(defId, nodeKey, menuType));
    }

    @PostMapping("/default-sign/save")
    @Operation(summary = "保存节点默认签字意见（覆盖式）")
    public R<Boolean> saveDefaultSigns(@RequestParam("defId") Long defId,
                                       @RequestParam("nodeKey") String nodeKey,
                                       @RequestBody List<WfNodeDefaultSign> signs) {
        customOperationService.saveDefaultSigns(defId, nodeKey, signs);
        return R.status(true);
    }
}
