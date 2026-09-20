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
import org.springblade.formmode.dto.FormDataSaveDTO;
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
// 网关对 blade-formmode 做 StripPrefix，前端实际命中无前缀路径 /form/data、/form-data；
// Feign 直连（IFormmodeClient）使用带前缀路径 /api/blade-formmode/form/data。
// 两类路径均暴露，保证网关调用与 Feign 直连都能命中。
@RequestMapping({"/api/blade-formmode/form/data", "/api/blade-formmode/form-data", "/form/data", "/form-data"})
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
     * 按「表单ID」写入业务数据（新增/更新一行，返回业务数据ID）。
     *
     * <p>供 blade-workflow 发起流程时创建业务数据行：返回的 id 即 {@code wf_instance.data_id}。
     * 与 {@link #save} 不同，本接口按 {@code workflow_bill.table_name} 解析真实表名，不依赖
     * modeinfo 模块，故对迁移过来的表单（表名与表单ID不一致）同样可用。</p>
     *
     * <p>Feign 直连路径：{@code POST /form-data/save-by-form}（不带 /api/blade-formmode 前缀）。</p>
     */
    @PostMapping("/save-by-form")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Long> saveByForm(@Valid @RequestBody FormDataSaveDTO dto) {
        Long dataId = formDataService.saveBusinessData(dto);
        return R.data(dataId, "业务数据保存成功");
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

    /**
     * 获取字段定义（前端契约：GET /form-data/{formId}/fields）
     *
     * <p>与 {@link #getFields} 语义一致，仅路径顺序不同以对齐前端 {@code formDataApi.getFieldDefinitions}；
     * 网关 StripPrefix 后前端命中的正是无前缀的 /form-data/{formId}/fields。</p>
     */
    @GetMapping("/{formId}/fields")
    public R<List<FieldDefinitionDTO>> getFieldsByForm(@PathVariable Long formId) {
        List<FieldDefinitionDTO> fields = formModeService.getFieldDefinitions(formId);
        return R.data(fields);
    }

}
