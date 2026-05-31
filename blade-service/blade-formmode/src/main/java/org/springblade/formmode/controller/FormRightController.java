package org.springblade.formmode.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
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
    @PreAuth("hasPermission('formmode:right:view')")
    public R<List<ModeRightInfo>> getModeRights(@PathVariable Long modeId) {
        return R.data(formRightService.getModeRights(modeId));
    }

    /**
     * 保存模块权限
     */
    @PostMapping("/mode/{modeId}")
    @PreAuth("hasPermission('formmode:right:edit')")
    public R<Boolean> saveModeRights(@PathVariable Long modeId, @RequestBody List<ModeRightInfo> rights) {
        return R.data(formRightService.saveModeRights(modeId, rights), "权限保存成功");
    }

    /**
     * 获取字段权限
     */
    @GetMapping("/field/{modeId}")
    @PreAuth("hasPermission('formmode:right:view')")
    public R<List<ModeFieldAuthorize>> getFieldAuthorizes(@PathVariable Long modeId) {
        return R.data(formRightService.getFieldAuthorizes(modeId));
    }

    /**
     * 保存字段权限
     */
    @PostMapping("/field/{modeId}")
    @PreAuth("hasPermission('formmode:right:edit')")
    public R<Boolean> saveFieldAuthorizes(@PathVariable Long modeId, @RequestBody List<ModeFieldAuthorize> authorizes) {
        return R.data(formRightService.saveFieldAuthorizes(modeId, authorizes), "字段权限保存成功");
    }

    /**
     * 校验权限
     */
    @GetMapping("/check/{modeId}/{rightType}")
    public R<Boolean> checkRight(@PathVariable Long modeId, @PathVariable Integer rightType) {
        // 实际项目中从 SecureUtil 获取当前用户
        return R.data(formRightService.checkRight(modeId, rightType, 0L));
    }

}
