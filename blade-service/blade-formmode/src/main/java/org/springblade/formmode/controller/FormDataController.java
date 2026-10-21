package org.springblade.formmode.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.formmode.dto.FieldDefinitionDTO;
import org.springblade.formmode.dto.FormDataDTO;
import org.springblade.formmode.dto.FormDataSaveDTO;
import org.springblade.formmode.service.IFormDataService;
import org.springblade.formmode.enums.RightType;
import org.springblade.formmode.service.IFormModeService;
import org.springblade.formmode.service.IFormRightService;
import org.springblade.formmode.vo.FormDataVO;
import org.springframework.beans.factory.annotation.Value;
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
@Slf4j
public class FormDataController extends BladeController {

    private final IFormDataService formDataService;
    private final IFormModeService formModeService;
    private final IFormRightService formRightService;

    // ── 模块权限校验开关（非流程场景，对标 E9 moderightinfo 的 7 种 righttype）──
    // 写操作默认强制：checkRight 在「该模块未配置任何权限」时放行，故默认开启对无配置模块零影响；
    /** 写操作（新建/编辑/删除）是否强制模块权限校验 */
    @Value("${blade.formmode.right-check.enforce:true}")
    private boolean writeRightEnforce;
    /** 读操作（详情/分页）是否强制模块权限校验；默认 false（先 warn 观察，灰度后再开） */
    @Value("${blade.formmode.right-check.enforce-read:false}")
    private boolean readRightEnforce;

    /**
     * 模块操作权限校验（非流程场景）。
     *
     * <p>语义（{@link IFormRightService#checkRight}）：该模块<b>未配置任何权限时放行</b>；
     * 配置了但当前用户（角色/部门/人员，objid=0 表示全部）未命中则拒绝。</p>
     *
     * <p>兜底策略：解析不到 modeId（表单未建模块 / 数据模型 int 溢出）时放行；
     * 校验过程异常也降级为放行 + 告警——权限链路故障不应阻断业务主流程。</p>
     *
     * @param modeId  模块ID（modeinfo.id）
     * @param type    权限类型
     * @param enforce true=未通过则抛异常拒绝；false=仅告警（灰度观察模式）
     */
    private void assertModeRight(Long modeId, RightType type, boolean enforce) {
        if (modeId == null) {
            return;
        }
        boolean ok;
        try {
            ok = formRightService.checkRight(modeId, type.getCode(), SecureUtil.getUserId());
        } catch (Exception e) {
            log.warn("[formmode] 模块权限校验异常，按放行处理. modeId={}, type={}, err={}", modeId, type, e.getMessage());
            return;
        }
        if (ok) {
            return;
        }
        if (enforce) {
            throw new ServiceException("无权执行该操作（" + type.getName() + "不足）");
        }
        log.warn("[formmode] 模块权限校验未通过（观察模式，未阻断）. modeId={}, type={}, user={}",
            modeId, type, SecureUtil.getUserId());
    }

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
     *
     * <p><b>鉴权</b>：本接口是「发起流程时创建业务数据行 + 回填 request_id」的落点，
     * 由 blade-workflow 以<b>当前用户身份</b>（Feign 透传 token）调用。普通员工发起自己的申请时
     * 也会走到这里，故只要求「已登录」（{@link WorkflowConstant#HAS_AUTH}）—— 否则会出现
     * 「流程能发起，但业务表没有对应行 / request_id 没回填」（L3 自检 business_row_ready=0）。</p>
     */
    @PostMapping("/save-by-form")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    public R<Long> saveByForm(@Valid @RequestBody FormDataSaveDTO dto) {
        Long dataId = formDataService.saveBusinessData(dto);
        return R.data(dataId, "业务数据保存成功");
    }

    /**
     * 按「表单ID」新增业务数据行（前端契约：POST /form-data/{formId}）。
     *
     * <p>对齐前端 {@code dataApi.create(formId, fieldMap)}：路径携带 formId，请求体为
     * 字段名→值的 Map。内部复用 {@link #saveByForm} 同口径的 {@code saveBusinessData}，
     * 按 {@code workflow_bill.table_name} 定位真实业务表，仅写入存在的列。</p>
     *
     * <p>前端独立「新增数据」页面（/formmode/formview/aae?mode=add）即命中本接口。</p>
     */
    @PostMapping("/{formId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Long> createByForm(@PathVariable Long formId,
                                @RequestBody Map<String, Object> fieldValues) {
        assertModeRight(formRightService.resolveModeIdByFormId(formId), RightType.CREATE, writeRightEnforce);
        FormDataSaveDTO dto = new FormDataSaveDTO();
        dto.setFormId(formId);
        dto.setFieldValues(fieldValues);
        Long dataId = formDataService.saveBusinessData(dto);
        return R.data(dataId, "业务数据新增成功");
    }

    /**
     * 按「表单ID」更新业务数据行（前端契约：PUT /form-data/{formId}/{dataId}）。
     *
     * <p>对齐前端 {@code dataApi.update(formId, dataId, fieldMap)}。dataId 非空即走
     * {@code saveBusinessData} 的更新分支（按 id 更新该行存在的列）。</p>
     */
    @PutMapping("/{formId}/{dataId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Long> updateByForm(@PathVariable Long formId,
                                @PathVariable Long dataId,
                                @RequestBody Map<String, Object> fieldValues) {
        assertModeRight(formRightService.resolveModeIdByFormId(formId), RightType.EDIT, writeRightEnforce);
        FormDataSaveDTO dto = new FormDataSaveDTO();
        dto.setFormId(formId);
        dto.setDataId(dataId);
        dto.setFieldValues(fieldValues);
        Long id = formDataService.saveBusinessData(dto);
        return R.data(id, "业务数据更新成功");
    }

    /**
     * 按「表单ID」批量删除业务数据行（前端契约：POST /form-data/{formId}/batch-delete）。
     *
     * <p>对齐前端 {@code dataApi.batchDelete(formId, ids)}（body=ids 数组）。
     * 复用 {@code deleteBusinessData}（按 {@code workflow_bill.table_name} 定位真实表）。</p>
     */
    @PostMapping("/{formId}/batch-delete")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Boolean> batchDeleteByForm(@PathVariable Long formId,
                                        @RequestBody List<Long> dataIds) {
        assertModeRight(formRightService.resolveModeIdByFormId(formId), RightType.DELETE, writeRightEnforce);
        if (dataIds == null || dataIds.isEmpty()) {
            return R.success("批量删除成功");
        }
        for (Long dataId : dataIds) {
            formDataService.deleteBusinessData(formId, dataId);
        }
        return R.success("批量删除成功");
    }

    /**
     * 获取数据详情
     */
    @GetMapping("/{modeId}/{dataId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<FormDataVO> detail(@PathVariable Long modeId, @PathVariable Long dataId) {
        assertModeRight(modeId, RightType.DETAIL, readRightEnforce);
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
        assertModeRight(modeId, RightType.VIEW, readRightEnforce);
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
     * 按「表单ID」删除业务数据行（供 blade-workflow 删除草稿清理业务行）。
     *
     * <p>与 {@code /save-by-form} 对称：按 {@code workflow_bill.table_name} 解析真实表名，
     * 不依赖 modeinfo 模块。鉴权同 {@code /save-by-form}——仅要求「已登录」
     * （{@link WorkflowConstant#HAS_AUTH}），由 blade-workflow 以当前用户身份（Feign 透传 token）调用。</p>
     */
    @DeleteMapping("/by-form")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    public R<Boolean> deleteByForm(@RequestParam("formId") Long formId,
                                   @RequestParam("dataId") Long dataId) {
        boolean result = formDataService.deleteBusinessData(formId, dataId);
        return result ? R.success("业务数据删除成功") : R.fail("数据不存在");
    }

    /**
     * 按「表单ID + 数据ID」读取业务数据行（含自定义字段值）。
     *
     * <p>与 {@code /by-form} 删除对称：按 {@code workflow_bill.table_name} 定位真实业务表，
     * 返回 {@link FormDataVO#getFieldValues()}（字段名→值，已过滤系统字段）。
     * 供 blade-workflow 公文套红时按表单字段填充书签（Feign 透传 token，仅要求已登录）。</p>
     */
    @GetMapping("/by-form")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    public R<FormDataVO> getByForm(@RequestParam("formId") Long formId,
                                   @RequestParam("dataId") Long dataId) {
        FormDataVO result = formDataService.getFormDataById(formId, dataId);
        return result != null ? R.data(result) : R.fail("数据不存在");
    }

    /**
     * 按「表单ID + 数据ID」读取<b>明细表数据</b>（补齐明细持久化缺口）。
     *
     * <p>明细物理表（{@code {主表名}_dtN}）此前无读取入口，导致明细只存在于流程快照 JSON，
     * 无法查询 / 统计 / 关联。本端点按 {@code workflow_bill.table_name} 定位主表，再从
     * information_schema 动态发现其明细表并逐表取行。</p>
     *
     * <p>返回 {@code 明细表名 → 行列表}；该表单无明细时返回空对象（不报错）。
     * 鉴权同 {@code /by-form}——仅要求已登录（Feign 透传 token）。</p>
     */
    @GetMapping("/by-form/detail")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    public R<Map<String, List<Map<String, Object>>>> getDetailByForm(@RequestParam("formId") Long formId,
                                                                     @RequestParam("dataId") Long dataId) {
        return R.data(formDataService.getDetailData(formId, dataId));
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
