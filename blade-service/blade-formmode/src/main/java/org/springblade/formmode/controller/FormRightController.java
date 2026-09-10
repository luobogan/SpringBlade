package org.springblade.formmode.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.formmode.entity.ModeFieldAuthorize;
import org.springblade.formmode.entity.ModeRightInfo;
import org.springblade.formmode.service.IFormRightService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 表单权限控制器
 *
 * 对应 ecology 的 FormmodeRightController
 */
@RestController
@RequestMapping("/api/blade-formmode/right")
@Tag(name = "表单权限", description = "表单建模权限管理")
@RequiredArgsConstructor
public class FormRightController extends BladeController {

    private final IFormRightService formRightService;

    /**
     * 获取模块权限
     */
    @GetMapping("/mode/{modeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<List<ModeRightInfo>> getModeRights(@PathVariable Long modeId) {
        return R.data(formRightService.getModeRights(modeId));
    }

    /**
     * 保存模块权限
     */
    @PostMapping("/mode/{modeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Boolean> saveModeRights(@PathVariable Long modeId, @RequestBody List<ModeRightInfo> rights) {
        return R.data(formRightService.saveModeRights(modeId, rights), "权限保存成功");
    }

    /**
     * 获取字段权限
     */
    @GetMapping("/field/{modeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<List<ModeFieldAuthorize>> getFieldAuthorizes(@PathVariable Long modeId) {
        return R.data(formRightService.getFieldAuthorizes(modeId));
    }

    /**
     * 保存字段权限
     */
    @PostMapping("/field/{modeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Boolean> saveFieldAuthorizes(@PathVariable Long modeId, @RequestBody List<ModeFieldAuthorize> authorizes) {
        return R.data(formRightService.saveFieldAuthorizes(modeId, authorizes), "字段权限保存成功");
    }

    /**
     * 校验权限
     *
     * <p>修正：原先硬编码 0L 用户（任何人都按匿名用户鉴权，审批态权限形同虚设），
     * 改为从 {@link org.springblade.core.secure.utils.SecureUtil} 取真实当前用户。</p>
     */
    @GetMapping("/check/{modeId}/{rightType}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Boolean> checkRight(@PathVariable Long modeId, @PathVariable Integer rightType) {
        return R.data(formRightService.checkRight(modeId, rightType, SecureUtil.getUserId()));
    }

}
