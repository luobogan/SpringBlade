package org.springblade.formmode.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.dto.FormModeDTO;
import org.springblade.formmode.entity.ModeInfo;
import org.springblade.formmode.entity.ModeTreeField;
import org.springblade.formmode.mapper.ModeTreeFieldMapper;
import org.springblade.formmode.service.IFormModeService;
import org.springblade.formmode.vo.FormModeVO;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 表单模块管理控制器
 *
 * 对应 ecology 的 FormmodeFormAction + FormmodeFormController
 */
@RestController
@RequestMapping("/api/blade-formmode/mode")
@Tag(name = "表单模块管理", description = "表单建模模块的CRUD管理")
@RequiredArgsConstructor
public class FormModeController extends BladeController {

    private final IFormModeService formModeService;
    private final ModeTreeFieldMapper modeTreeFieldMapper;

    /**
     * 创建表单模块
     */
    @PostMapping
    @PreAuth("hasPermission('formmode:mode:add')")
    public R<FormModeVO> create(@Valid @RequestBody FormModeDTO formModeDTO) {
        FormModeVO result = formModeService.createFormMode(formModeDTO);
        return R.data(result, "模块创建成功");
    }

    /**
     * 更新表单模块
     */
    @PutMapping("/{id}")
    @PreAuth("hasPermission('formmode:mode:edit')")
    public R<FormModeVO> update(@PathVariable Long id, @Valid @RequestBody FormModeDTO formModeDTO) {
        FormModeVO result = formModeService.updateFormMode(id, formModeDTO);
        return R.data(result, "模块更新成功");
    }

    /**
     * 删除表单模块
     */
    @DeleteMapping("/{id}")
    @PreAuth("hasPermission('formmode:mode:delete')")
    public R<Boolean> delete(@PathVariable Long id) {
        boolean result = formModeService.deleteFormMode(id);
        return result ? R.success("模块删除成功") : R.fail("模块不存在");
    }

    /**
     * 获取模块详情
     */
    @GetMapping("/{id}")
    @PreAuth("hasPermission('formmode:mode:view')")
    public R<FormModeVO> detail(@PathVariable Long id) {
        FormModeVO result = formModeService.getFormModeDetail(id);
        return result != null ? R.data(result) : R.fail("模块不存在");
    }

    /**
     * 获取所有模块列表
     */
    @GetMapping("/list")
    @PreAuth("hasPermission('formmode:mode:view')")
    public R<List<ModeInfo>> list() {
        List<ModeInfo> list = formModeService.list();
        return R.data(list);
    }

    /**
     * 获取应用分类列表
     */
    @GetMapping("/tree-fields")
    public R<List<ModeTreeField>> getTreeFields() {
        List<ModeTreeField> list = modeTreeFieldMapper.selectList(null);
        return R.data(list);
    }

}
