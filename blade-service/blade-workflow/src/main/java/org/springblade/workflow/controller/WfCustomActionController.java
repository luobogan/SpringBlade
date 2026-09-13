package org.springblade.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.entity.WfCustomAction;
import org.springblade.workflow.service.IWfCustomActionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 自定义接口动作控制器（对齐 ecology「注册自定义接口」）。
 *
 * <p>节点前后附加操作的「外部接口 → 自定义接口动作」即选用这里注册的动作：
 * 接口动作名称 / 接口动作标识 / 接口动作类文件（类全名）+ 参数设置。</p>
 */
@RestController
@RequestMapping("/custom-action")
// 角色门禁：流程管理员（workflow），与 formmode 侧保持同一角色
@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
@RequiredArgsConstructor
@Tag(name = "自定义接口动作", description = "注册供节点前后附加操作调用的 Java 动作类")
public class WfCustomActionController {

    private final IWfCustomActionService customActionService;

    @GetMapping("/list")
    @Operation(summary = "已注册的自定义接口动作列表", description = "供节点附加操作的「自定义接口动作」下拉选择")
    public R<List<WfCustomAction>> list() {
        return R.data(customActionService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "自定义接口动作详情")
    public R<WfCustomAction> detail(@PathVariable("id") Long id) {
        return R.data(customActionService.getDetail(id));
    }

    @PostMapping("/save")
    @Operation(summary = "注册 / 更新自定义接口动作", description = "接口动作标识唯一；类文件须为类全名且实现 IWfCustomAction")
    public R<Boolean> save(@RequestBody WfCustomAction action) {
        return R.status(customActionService.saveAction(action));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除自定义接口动作")
    public R<Boolean> remove(@PathVariable("id") Long id) {
        return R.status(customActionService.removeAction(id));
    }
}
