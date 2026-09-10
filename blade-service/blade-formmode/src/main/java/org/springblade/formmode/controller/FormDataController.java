package org.springblade.formmode.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.formmode.dto.FieldDefinitionDTO;
import org.springblade.formmode.dto.FormDataDTO;
import org.springblade.formmode.service.IFormDataService;
import org.springblade.formmode.service.IFormModeService;
import org.springblade.formmode.vo.FormDataVO;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 表单数据控制器
 *
 * 对应 ecology 的 ModeDataManager (数据CRUD)
 */
@RestController
// 路径统一：后端原为 /form/data，前端 formDataApi 用 /form-data，
// 此处同时暴露两个路径，保证新旧调用方均可命中，避免联调阻塞（文档 3.4-7）
@RequestMapping({"/api/blade-formmode/form/data", "/api/blade-formmode/form-data"})
@Tag(name = "表单数据", description = "表单数据的增删改查")
@RequiredArgsConstructor
public class FormDataController extends BladeController {

    private final IFormDataService formDataService;
    private final IFormModeService formModeService;

    /**
     * 保存表单数据（新建/编辑）
     */
    @PostMapping("/save")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Long> save(@Valid @RequestBody FormDataDTO formDataDTO) {
        Long dataId = formDataService.saveFormData(formDataDTO);
        return R.data(dataId, "数据保存成功");
    }

    /**
     * 获取数据详情
     */
    @GetMapping("/{modeId}/{dataId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<FormDataVO> detail(@PathVariable Long modeId, @PathVariable Long dataId) {
        FormDataVO result = formDataService.getFormDataById(modeId, dataId);
        return result != null ? R.data(result) : R.fail("数据不存在");
    }

    /**
     * 分页查询数据
     */
    @PostMapping("/page/{modeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Page<Map<String, Object>>> page(@PathVariable Long modeId,
                                              @RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestBody(required = false) Map<String, Object> params) {
        Page<?> page = new Page<>(current, size);
        Page<Map<String, Object>> result = formDataService.pageFormData(modeId, page, params);
        return R.data(result);
    }

    /**
     * 删除数据
     */
    @DeleteMapping("/{modeId}/{dataId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Boolean> delete(@PathVariable Long modeId, @PathVariable Long dataId) {
        boolean result = formDataService.deleteFormData(modeId, dataId);
        return result ? R.success("数据删除成功") : R.fail("数据不存在");
    }

    /**
     * 批量删除数据
     */
    @DeleteMapping("/batch/{modeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Boolean> batchDelete(@PathVariable Long modeId, @RequestBody List<Long> dataIds) {
        boolean result = formDataService.batchDeleteFormData(modeId, dataIds);
        return result ? R.success("批量删除成功") : R.fail("删除失败");
    }

    /**
     * 获取字段定义
     */
    @GetMapping("/fields/{modeId}")
    public R<List<FieldDefinitionDTO>> getFields(@PathVariable Long modeId) {
        List<FieldDefinitionDTO> fields = formModeService.getFieldDefinitions(modeId);
        return R.data(fields);
    }

}
