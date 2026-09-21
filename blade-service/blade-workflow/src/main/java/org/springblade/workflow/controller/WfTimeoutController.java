package org.springblade.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.entity.WfNodeTimeout;
import org.springblade.workflow.service.IWfTimeoutService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 节点超时规则控制器（对齐泛微节点信息「超时设置」）。
 *
 * <p>多条规则按节点配置：起算方式 / 截止方式（相对时长·固定时刻·表单时间字段）/
 * 超时动作（自动通过·流转·指定操作者·提醒）+ 提醒通道与对象。</p>
 */
@RestController
@RequestMapping("/node-timeout")
@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
@RequiredArgsConstructor
@Tag(name = "节点超时规则", description = "多条超时规则配置与保存")
public class WfTimeoutController {

    private final IWfTimeoutService timeoutService;

    @GetMapping("/list")
    @Operation(summary = "节点超时规则列表", description = "按 defId + nodeKey 取已启用规则（按 seq 升序）")
    public R<List<WfNodeTimeout>> list(@RequestParam("defId") Long defId,
                                       @RequestParam("nodeKey") String nodeKey) {
        return R.data(timeoutService.listEnabled(defId, nodeKey));
    }

    @PostMapping("/save")
    @Operation(summary = "保存节点超时规则", description = "覆盖保存该节点的全部规则（空数组即清空）")
    public R<Boolean> save(@RequestParam("defId") Long defId,
                           @RequestParam("nodeKey") String nodeKey,
                           @RequestBody List<WfNodeTimeout> rules) {
        timeoutService.saveRules(defId, nodeKey, rules);
        return R.status(true);
    }
}
