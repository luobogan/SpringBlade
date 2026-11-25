package org.springblade.formmode.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.formmode.dto.FormLayoutSaveDTO;
import org.springblade.formmode.dto.FormLayoutSyncDTO;
import org.springblade.formmode.dto.FormLayoutTypeNameDTO;
import org.springblade.formmode.entity.FormLayout;
import org.springblade.formmode.service.IFormLayoutService;
import org.springblade.formmode.vo.FormLayoutVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 表单布局控制器
 * 提供Excel设计器的后端API
 */
@RestController
@RequestMapping("/form-layout")
@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
@Tag(name = "表单布局管理", description = "Excel设计器表单布局管理")
@RequiredArgsConstructor
@Slf4j
public class FormLayoutController extends BladeController {

    private final IFormLayoutService formLayoutService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 根据表单ID获取布局（支持按布局类型 / 流程节点取布局，带回退）
     *
     * <p><b>鉴权</b>：这是 blade-workflow 渲染包 / 预览 / 服务端校验的布局来源
     * （{@code IFormmodeClient#getFormLayout}，Feign 透传当前用户 token）。
     * 普通员工打开发起页、办理页时都会走到这里，故只要求「已登录」
     * （{@link WorkflowConstant#HAS_AUTH}）—— 否则普通用户能进发起页却拿不到布局，
     * 表现就是「流程表单带不出来（空白）」。</p>
     */
    @GetMapping("/{formId}")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "获取表单布局", description = "根据表单ID获取表单布局；可按 layouttype（布局类型）与 nodeKey（流程节点）取，带回退（返回 FormLayoutVO，供跨服务 Feign 调用）")
    public R<FormLayoutVO> getByFormId(
            @Parameter(description = "表单ID") @PathVariable Long formId,
            @Parameter(description = "布局类型：0编辑(默认) 1显示 3监控 4打印") @RequestParam(value = "layouttype", required = false) Integer layoutType,
            @Parameter(description = "流程节点Key（空=表单级通用）") @RequestParam(value = "nodeKey", required = false) String nodeKey,
            @Parameter(description = "节点无自身布局时是否继承表单级通用布局；布局设计器传 false（打开空白布局），默认 true（运行时继承）")
            @RequestParam(value = "inherit", required = false, defaultValue = "true") boolean inherit) {
        FormLayout formLayout = formLayoutService.getByFormId(formId, layoutType, nodeKey, inherit);
        if (formLayout == null) {
            return R.data(null);
        }
        FormLayoutVO vo = new FormLayoutVO();
        vo.setId(formLayout.getId());
        vo.setFormId(formLayout.getFormId());
        vo.setLayoutName(formLayout.getLayoutName());
        vo.setLayoutJson(formLayout.getLayoutJson());
        vo.setLayoutConfig(formLayout.getLayoutConfig());
        vo.setLayoutType(formLayout.getLayoutType());
        vo.setNodeKey(formLayout.getNodeKey());
        return R.data(vo);
    }

    /**
     * 布局列表（按表单过滤，可按类型/节点过滤）
     */
    @GetMapping("/list")
    @Operation(summary = "布局列表", description = "按 formId 列出布局，可按 layouttype / nodeKey 过滤")
    public R<java.util.List<FormLayout>> list(
            @Parameter(description = "表单ID") @RequestParam(value = "formId", required = false) Long formId,
            @Parameter(description = "布局类型") @RequestParam(value = "layouttype", required = false) Integer layoutType,
            @Parameter(description = "流程节点Key") @RequestParam(value = "nodeKey", required = false) String nodeKey) {
        return R.data(formLayoutService.listByFormId(formId, layoutType, nodeKey));
    }

    /**
     * 保存表单布局
     * 使用 String 类型的 formId 避免 JavaScript 大整数精度丢失
     */
    @PostMapping("/save")
    @Operation(summary = "保存表单布局", description = "保存Excel设计器的表单布局")
    public R<Boolean> save(@RequestBody FormLayoutSaveDTO dto) {
        try {
            // 将 DTO 转换为实体
            FormLayout formLayout = new FormLayout();
            formLayout.setId(dto.getId());
            formLayout.setLayoutName(dto.getLayoutName());
            formLayout.setLayoutJson(dto.getLayoutJson());
            formLayout.setLayoutConfig(dto.getLayoutConfig());
            formLayout.setLayoutType(dto.getLayoutType() == null ? 0 : dto.getLayoutType());
            formLayout.setNodeKey((dto.getNodeKey() == null || dto.getNodeKey().isBlank()) ? null : dto.getNodeKey());
            formLayout.setStatus(dto.getStatus());

            // 关键：将 String 转换为 Long，避免精度丢失
            if (dto.getFormId() != null && !dto.getFormId().trim().isEmpty()) {
                Long formId = Long.parseLong(dto.getFormId().trim());
                formLayout.setFormId(formId);
                log.info("保存表单布局, formId={}", formId);
            }

            boolean result = formLayoutService.saveFormLayout(formLayout);
            return R.data(result);
        } catch (NumberFormatException e) {
            log.error("表单ID格式错误: {}", dto.getFormId(), e);
            return R.fail("表单ID格式错误");
        } catch (IllegalArgumentException e) {
            log.warn("保存表单布局失败: {}", e.getMessage());
            return R.fail(e.getMessage());
        }
    }

    /** 查询表单四类布局类型显示名（含默认值） */
    @GetMapping("/type-names/{formId}")
    @Operation(summary = "查询布局类型显示名", description = "未配置的布局类型返回默认名称")
    public R<List<Map<String, Object>>> typeNames(@PathVariable Long formId) {
        Map<Integer, String> defaults = new LinkedHashMap<>();
        defaults.put(0, "编辑（含新建）");
        defaults.put(1, "显示");
        defaults.put(3, "监控");
        defaults.put(4, "打印");
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT layout_type, layout_name FROM form_layout_type_name WHERE form_id=? AND tenant_id='000000'",
                formId);
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    Object t = row.get("layout_type");
                    if (t instanceof Number) defaults.put(((Number) t).intValue(), String.valueOf(row.get("layout_name")));
                }
            }
        } catch (Exception e) {
            log.warn("查询布局类型显示名失败，使用默认值. formId={}, err={}", formId, e.getMessage());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        defaults.forEach((type, name) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("layoutType", type);
            item.put("layoutName", name);
            result.add(item);
        });
        return R.data(result);
    }

    /** 保存表单布局类型显示名 */
    @PostMapping("/type-name/save")
    @Operation(summary = "保存布局类型显示名", description = "按 formId + layoutType upsert")
    public R<Boolean> saveTypeName(@RequestBody FormLayoutTypeNameDTO dto) {
        try {
            if (dto == null || dto.getFormId() == null || dto.getLayoutType() == null) return R.fail("参数不完整");
            long formId = Long.parseLong(dto.getFormId().trim());
            int type = dto.getLayoutType();
            if (type != 0 && type != 1 && type != 3 && type != 4) return R.fail("布局类型非法");
            String name = dto.getLayoutName() == null ? "" : dto.getLayoutName().trim();
            if (name.isEmpty()) return R.fail("显示名不能为空");
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM form_layout_type_name WHERE form_id=? AND layout_type=? AND tenant_id='000000'",
                Long.class, formId, type);
            if (count != null && count > 0) {
                jdbcTemplate.update("UPDATE form_layout_type_name SET layout_name=?, update_time=NOW() WHERE form_id=? AND layout_type=? AND tenant_id='000000'",
                    name, formId, type);
            } else {
                jdbcTemplate.update("INSERT INTO form_layout_type_name (tenant_id,form_id,layout_type,layout_name,create_time,update_time) VALUES ('000000',?,?,?,NOW(),NOW())",
                    formId, type, name);
            }
            return R.data(true);
        } catch (NumberFormatException e) {
            return R.fail("表单ID格式错误");
        } catch (Exception e) {
            log.error("保存布局类型显示名失败. dto={}, err={}", dto, e.getMessage());
            return R.fail("保存失败：" + e.getMessage());
        }
    }

    /** 同步当前布局到同表单其他组 / 同组其他布局类型 */
    @PostMapping("/sync")
    @Operation(summary = "同步表单布局", description = "复制布局JSON与配置到目标nodeKey或目标布局类型")
    public R<Integer> syncLayout(@RequestBody FormLayoutSyncDTO dto) {
        try {
            if (dto == null || dto.getFormId() == null || dto.getSourceLayoutType() == null) return R.fail("参数不完整");
            long formId = Long.parseLong(dto.getFormId().trim());
            String sourceNode = normalizeNodeKey(dto.getSourceNodeKey());
            FormLayout source = formLayoutService.getByFormId(formId, dto.getSourceLayoutType(), sourceNode, false);
            if (source == null) return R.fail("源布局不存在，请先保存当前布局");

            boolean overwrite = Boolean.TRUE.equals(dto.getOverwrite());
            Set<String> targetNodes = new LinkedHashSet<>();
            if (dto.getTargetNodeKeys() != null) {
                dto.getTargetNodeKeys().stream().map(FormLayoutController::normalizeNodeKey).filter(java.util.Objects::nonNull).forEach(targetNodes::add);
            }
            if (sourceNode != null) targetNodes.remove(sourceNode);
            Set<Integer> targetTypes = new LinkedHashSet<>();
            if (dto.getTargetLayoutTypes() != null) {
                dto.getTargetLayoutTypes().stream().filter(java.util.Objects::nonNull).filter(t -> t == 0 || t == 1 || t == 3 || t == 4).forEach(targetTypes::add);
            }
            targetTypes.remove(dto.getSourceLayoutType());

            List<FormLayout> targets = new ArrayList<>();
            for (String node : targetNodes) {
                Long existsId = findExactLayoutId(formId, dto.getSourceLayoutType(), node);
                if (existsId != null && !overwrite) continue;
                targets.add(copyLayout(source, formId, dto.getSourceLayoutType(), node, existsId));
            }
            for (Integer type : targetTypes) {
                Long existsId = findExactLayoutId(formId, type, sourceNode);
                if (existsId != null && !overwrite) continue;
                targets.add(copyLayout(source, formId, type, sourceNode, existsId));
            }
            if (targets.isEmpty()) return R.data(0);
            for (FormLayout target : targets) formLayoutService.saveFormLayout(target);
            return R.data(targets.size());
        } catch (NumberFormatException e) {
            return R.fail("表单ID格式错误");
        } catch (Exception e) {
            log.error("同步表单布局失败. dto={}, err={}", dto, e.getMessage());
            return R.fail("同步失败：" + e.getMessage());
        }
    }

    /** 精确查找目标布局，避免 getByFormId 的“默认布局类型回退”把编辑布局误当作显示布局覆盖。 */
    private Long findExactLayoutId(Long formId, Integer layoutType, String nodeKey) {
        String node = normalizeNodeKey(nodeKey);
        List<Long> ids = node == null
            ? jdbcTemplate.queryForList(
                "SELECT id FROM form_layout WHERE form_id=? AND layout_type=? AND (node_key IS NULL OR node_key='') AND status=1 LIMIT 1",
                Long.class, formId, layoutType)
            : jdbcTemplate.queryForList(
                "SELECT id FROM form_layout WHERE form_id=? AND layout_type=? AND node_key=? AND status=1 LIMIT 1",
                Long.class, formId, layoutType, node);
        return (ids == null || ids.isEmpty()) ? null : ids.get(0);
    }

    private static FormLayout copyLayout(FormLayout source, Long formId, Integer layoutType, String nodeKey, Long targetId) {
        FormLayout target = new FormLayout();
        target.setId(targetId);
        target.setFormId(formId);
        target.setLayoutType(layoutType);
        target.setNodeKey(nodeKey);
        target.setLayoutName(source.getLayoutName());
        target.setLayoutJson(source.getLayoutJson());
        target.setLayoutConfig(source.getLayoutConfig());
        target.setStatus(source.getStatus() == null ? 1 : source.getStatus());
        return target;
    }

    private static String normalizeNodeKey(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        return key.trim();
    }

    /**
     * 按流程节点删除布局（节点被移除时清理残留，避免孤儿数据）
     */
    @DeleteMapping("/node/{formId}")
    @Operation(summary = "删除节点布局", description = "删除该表单下绑定指定流程节点的全部布局（各布局类型）")
    public R<Boolean> deleteByNode(
            @Parameter(description = "表单ID") @PathVariable Long formId,
            @Parameter(description = "流程节点Key") @RequestParam("nodeKey") String nodeKey) {
        return R.data(formLayoutService.deleteByNode(formId, nodeKey));
    }

    /**
     * 解析布局JSON
     */
    @PostMapping("/parse-json")
    @Operation(summary = "解析布局JSON", description = "解析Excel设计器的布局JSON")
    public R<String> parseJson(
            @Parameter(description = "布局JSON") @RequestBody String layoutJson) {
        String result = formLayoutService.parseLayoutJson(layoutJson);
        return R.data(result);
    }

    /**
     * 删除表单布局
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除表单布局", description = "根据ID删除表单布局")
    public R<Boolean> delete(
            @Parameter(description = "布局ID") @PathVariable Long id) {
        boolean result = formLayoutService.removeById(id);
        return R.data(result);
    }
}
