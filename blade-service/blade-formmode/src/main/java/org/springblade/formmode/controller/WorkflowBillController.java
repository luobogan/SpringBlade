package org.springblade.formmode.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.formmode.entity.FieldDefinition;
import org.springblade.formmode.entity.WorkflowBill;
import org.springblade.formmode.service.IDynamicTableService;
import org.springblade.formmode.service.IWorkflowBillService;
import org.springblade.formmode.service.IFieldDefinitionService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 表单定义控制器
 * 提供前端表单建模模块需要的 API
 */
@RestController
@RequestMapping("/form-definition")
@Tag(name = "表单定义管理", description = "表单定义管理")
@Slf4j
public class WorkflowBillController extends BladeController {

    @Autowired
    private IWorkflowBillService workflowBillService;

    @Autowired
    private IDynamicTableService dynamicTableService;

    @Autowired
    private IFieldDefinitionService fieldDefinitionService;

    /**
     * 获取表单列表
     */
    @GetMapping
    @Operation(summary = "获取表单列表", description = "获取表单定义列表")
    public R<IPage<WorkflowBill>> list(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "表单名称") @RequestParam(required = false) String formName,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {

        Page<WorkflowBill> page = new Page<>(current, pageSize);
        QueryWrapper<WorkflowBill> queryWrapper = new QueryWrapper<>();

        if (StringUtil.isNotBlank(formName)) {
            queryWrapper.like("form_name", formName);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }

        queryWrapper.orderByDesc("create_time");
        IPage<WorkflowBill> result = workflowBillService.page(page, queryWrapper);
        return R.data(result);
    }

    /**
     * 获取所有表单（不分页）
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有表单", description = "获取所有表单定义不分页")
    public R<List<WorkflowBill>> getAll() {
        List<WorkflowBill> list = workflowBillService.list();
        return R.data(list);
    }

    /**
     * 获取表单详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取表单详情", description = "根据ID获取表单定义详情")
    public R<WorkflowBill> getById(
            @Parameter(description = "表单ID") @PathVariable String id) {
        WorkflowBill workflowBill = workflowBillService.getById(id);
        return R.data(workflowBill);
    }

    /**
     * 创建表单
     */
    @PostMapping
    @Operation(summary = "创建表单", description = "创建新的表单定义")
    public R<WorkflowBill> create(@RequestBody WorkflowBill workflowBill) {
        workflowBillService.save(workflowBill);
        return R.data(workflowBill);
    }

    /**
     * 更新表单
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新表单", description = "更新表单定义")
    public R<WorkflowBill> update(
            @Parameter(description = "表单ID") @PathVariable String id,
            @RequestBody WorkflowBill workflowBill) {
        workflowBill.setId(Long.parseLong(id));
        workflowBillService.updateById(workflowBill);
        return R.data(workflowBill);
    }

    /**
     * 删除表单
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除表单", description = "删除表单定义")
    public R<Boolean> delete(
            @Parameter(description = "表单ID") @PathVariable String id) {
        return R.data(workflowBillService.removeById(id));
    }

    /**
     * 批量删除表单
     */
    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除表单", description = "批量删除表单定义")
    public R<Boolean> batchDelete(@RequestBody String[] ids) {
        List<String> idList = Arrays.asList(ids);
        return R.data(workflowBillService.removeByIds(idList.stream().map(Long::parseLong).collect(Collectors.toList())));
    }

    /**
     * 更新表单状态
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新表单状态", description = "更新表单定义状态")
    public R<Boolean> updateStatus(
            @Parameter(description = "表单ID") @PathVariable String id,
            @Parameter(description = "状态") @RequestParam Integer status) {
        WorkflowBill workflowBill = new WorkflowBill();
        workflowBill.setId(Long.parseLong(id));
        workflowBill.setStatus(status);
        return R.data(workflowBillService.updateById(workflowBill));
    }

    /**
     * 获取下一个自增表名
     * 规则：formtable_main_{N}，查询现有最大N值并累加
     */
    @GetMapping("/next-table-name")
    @Operation(summary = "获取下一个表名", description = "自动生成下一个主表名 formtable_main_{N}")
    public R<String> getNextTableName() {
        // 查询所有匹配 formtable_main_{N} 的表名
        QueryWrapper<WorkflowBill> queryWrapper = new QueryWrapper<>();
        queryWrapper.likeRight("table_name", "formtable_main_");
        List<WorkflowBill> forms = workflowBillService.list(queryWrapper);

        int maxN = 0;
        for (WorkflowBill form : forms) {
            String tableName = form.getTableName();
            if (tableName != null && tableName.matches("formtable_main_\\d+")) {
                try {
                    int n = Integer.parseInt(tableName.substring("formtable_main_".length()));
                    if (n > maxN) {
                        maxN = n;
                    }
                } catch (NumberFormatException e) {
                    // 忽略格式不匹配的
                }
            }
        }

        int nextN = maxN + 1;
        String nextTableName = "formtable_main_" + nextN;
        return R.data(nextTableName);
    }

    /**
     * 检查表名是否存在
     */
    @GetMapping("/check-table-name")
    @Operation(summary = "检查表名", description = "检查数据库表名是否已存在")
    public R<Boolean> checkTableName(
            @Parameter(description = "表名") @RequestParam String tableName,
            @Parameter(description = "排除的ID") @RequestParam(required = false) String excludeId) {
        QueryWrapper<WorkflowBill> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("table_name", tableName);
        if (StringUtil.isNotBlank(excludeId)) {
            queryWrapper.ne("id", excludeId);
        }
        long count = workflowBillService.count(queryWrapper);
        return R.data(count == 0);
    }

    /**
     * 创建数据库表
     * 根据表单ID创建对应的数据库表（主表 + 明细表）
     */
    @PostMapping("/{id}/create-table")
    @Operation(summary = "创建数据库表", description = "根据表单定义创建数据库表")
    public R<String> createTable(
            @Parameter(description = "表单ID") @PathVariable String id) {
        try {
            WorkflowBill workflowBill = workflowBillService.getById(id);
            if (workflowBill == null) {
                return R.fail("表单不存在");
            }

            // 1. 创建主表（使用配置的表名）
            String tableName = workflowBill.getTableName();
            log.info("=== 创建数据库表 - 表单ID: {}, 表名: {} ===", id, tableName);
            if (tableName == null || tableName.isEmpty()) {
                return R.fail("表单表名未配置");
            }
            boolean mainTableResult = dynamicTableService.createMainTable(tableName);
            if (!mainTableResult) {
                return R.fail("主表创建失败");
            }

            // 2. 创建明细表（如果有）
            // 明细表命名规则：{主表名}_dt{M}
            Integer detailTableCount = workflowBill.getDetailTableCount();
            if (detailTableCount != null && detailTableCount > 0) {
                for (int i = 1; i <= detailTableCount; i++) {
                    String detailTableName = tableName + "_dt" + i;
                    boolean detailTableResult = dynamicTableService.createDetailTable(detailTableName);
                    if (!detailTableResult) {
                        return R.fail("明细表 " + detailTableName + " 创建失败");
                    }
                }
            }

            // 3. 查询所有字段，添加到对应表中
            // 系统保留列名，不需要从字段定义中添加
            Set<String> systemColumns = new HashSet<>(Arrays.asList(
                    "id", "request_id", "modedatacreater", "modedatacreatedate",
                    "modedatacreatetime", "modedatamodifier", "modedatamodifydate",
                    "modedatamodifytime", "mode_uuid", "main_id", "is_deleted"
            ));

            QueryWrapper<FieldDefinition> fieldQuery = new QueryWrapper<>();
            fieldQuery.eq("billid", id);
            List<FieldDefinition> allFields = fieldDefinitionService.list(fieldQuery);

            for (FieldDefinition field : allFields) {
                String targetTableName;
                if (field.getIsMain() != null && field.getIsMain() == 0) {
                    // 明细表字段
                    Integer detailIndex = field.getDetailTable();
                    if (detailIndex == null || detailIndex <= 0) {
                        detailIndex = 1;
                    }
                    targetTableName = tableName + "_dt" + detailIndex;
                } else {
                    // 主表字段
                    targetTableName = tableName;
                }

                String sqlType = mapDbTypeToSql(field.getFieldDbType(), field.getFieldLen());
                // 使用 fieldDbName 作为数据库列名，如果为空则使用 fieldName
                String columnName = field.getFieldDbName();
                if (columnName == null || columnName.isEmpty()) {
                    columnName = field.getFieldName();
                }
                // 跳过系统保留列
                if (systemColumns.contains(columnName.toLowerCase())) {
                    continue;
                }

                try {
                    dynamicTableService.addColumnToTable(targetTableName, columnName, sqlType, null);
                } catch (Exception e) {
                    // 列已存在则忽略
                    log.warn("添加列失败，可能已存在: " + columnName + ", 表: " + targetTableName);
                }
            }

            return R.success("数据库表创建成功");
        } catch (Exception e) {
            return R.fail("创建数据库表失败: " + e.getMessage());
        }
    }

    /**
     * 同步数据库表结构（根据数据库中已有的字段定义）
     * 保存表单时自动调用，包含数据兼容性检查
     */
    @PostMapping("/{id}/sync-table-structure")
    @Operation(summary = "同步表结构", description = "根据数据库中已有的字段定义同步数据库表结构，包含数据兼容性检查")
    public R<Map<String, Object>> syncTableStructure(
            @Parameter(description = "表单ID") @PathVariable String id) {
        try {
            WorkflowBill workflowBill = workflowBillService.getById(id);
            if (workflowBill == null) {
                return R.fail("表单不存在");
            }

            String tableName = workflowBill.getTableName();
            if (tableName == null || tableName.isEmpty()) {
                return R.fail("表单表名未配置");
            }

            // 调用服务同步表结构（从数据库获取字段定义）
            Map<String, Object> result = dynamicTableService.syncTableStructure(id, tableName);
            return R.data(result);
        } catch (Exception e) {
            log.error("同步表结构失败: " + id, e);
            return R.fail("同步表结构失败: " + e.getMessage());
        }
    }

    /**
     * 同步数据库表结构（使用前端传入的字段定义）
     * 保存表单时自动调用，包含数据兼容性检查
     */
    @PostMapping("/{id}/sync-table-structure-with-fields")
    @Operation(summary = "同步表结构（带字段定义）", description = "使用前端传入的字段定义同步数据库表结构，包含数据兼容性检查")
    public R<Map<String, Object>> syncTableStructureWithFields(
            @Parameter(description = "表单ID") @PathVariable String id,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            WorkflowBill workflowBill = workflowBillService.getById(id);
            if (workflowBill == null) {
                return R.fail("表单不存在");
            }

            String tableName = workflowBill.getTableName();
            if (tableName == null || tableName.isEmpty()) {
                return R.fail("表单表名未配置");
            }

            // 直接读取请求体
            String requestBody = new java.io.BufferedReader(
                new java.io.InputStreamReader(request.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))
                .lines().collect(java.util.stream.Collectors.joining("\n"));

            log.info("========== 接收到同步表结构请求，请求体: {}", requestBody);

            // 使用Jackson解析请求体
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> fields = mapper.readValue(requestBody,
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});

            log.info("========== 接收到同步表结构请求，前端传入 {} 个字段定义 ==========", fields.size());

            // 将 Map 列表转换为 FieldDefinition 列表
            List<FieldDefinition> fieldList = new ArrayList<>();
            for (Map<String, Object> fieldMap : fields) {
                FieldDefinition field = new FieldDefinition();

                // 主键ID
                if (fieldMap.containsKey("id")) {
                    field.setId(getLongValue(fieldMap.get("id")));
                }

                // 字段名称
                field.setFieldName(getStringValue(fieldMap.get("fieldName")));

                // 字段标签
                field.setFieldLabel(getStringValue(fieldMap.get("fieldLabel")));

                // 数据库字段名
                field.setFieldDbName(getStringValue(fieldMap.get("fieldDbName")));

                // HTML类型
                field.setFieldHtmlType(getIntValue(fieldMap.get("fieldHtmlType")));

                // 字段类型
                field.setFieldType(getStringValue(fieldMap.get("fieldType")));

                // 数据库类型
                field.setFieldDbType(getStringValue(fieldMap.get("fieldDbType")));

                // 字段长度
                field.setFieldLen(getIntValue(fieldMap.get("fieldLength")));

                // 小数位数
                field.setDecimalDigit(getIntValue(fieldMap.get("fieldDecimals")));

                // 是否必填
                field.setIsNull(getIntValue(fieldMap.get("isRequired")));

                // 是否只读
                field.setIsReadOnly(getIntValue(fieldMap.get("isReadOnly")));

                // 默认值
                field.setDefaultValue(getStringValue(fieldMap.get("defaultValue")));

                // 排序
                field.setDsOrder(getIntValue(fieldMap.get("sort")));

                // 状态
                field.setStatus(getIntValue(fieldMap.get("status")));

                // 是否系统字段
                field.setIsSystemField(getIntValue(fieldMap.get("isSystemField")));

                // 是否列表显示
                field.setListDisplay(getIntValue(fieldMap.get("listDisplay")));

                // 是否主表字段
                field.setIsMain(getIntValue(fieldMap.get("isMain")));

                // 明细表索引
                field.setDetailTable(getIntValue(fieldMap.get("detailTable")));

                // 设置表单ID
                field.setBillId(id);

                fieldList.add(field);
            }

            // 调用服务同步表结构（使用前端传入的字段定义）
            Map<String, Object> result = dynamicTableService.syncTableStructure(id, tableName, fieldList);
            return R.data(result);
        } catch (Exception e) {
            log.error("同步表结构失败: " + id, e);
            return R.fail("同步表结构失败: " + e.getMessage());
        }
    }

    private Long getLongValue(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Integer getIntValue(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String getStringValue(Object value) {
        if (value == null) return null;
        return String.valueOf(value);
    }

    /**
     * 将字段数据库类型映射为SQL类型
     */
    /**
     * 删除明细表（包括数据库表和字段定义）
     * 只删除指定索引的明细表，不影响其他明细表的索引
     */
    @DeleteMapping("/{id}/detail-table/{detailIndex}")
    @Operation(summary = "删除明细表", description = "删除指定索引的明细表及其字段定义")
    public R<Boolean> deleteDetailTable(
            @Parameter(description = "表单ID") @PathVariable String id,
            @Parameter(description = "明细表索引（从1开始）") @PathVariable Integer detailIndex) {
        try {
            WorkflowBill workflowBill = workflowBillService.getById(id);
            if (workflowBill == null) {
                return R.fail("表单不存在");
            }

            String tableName = workflowBill.getTableName();
            String detailTableName = org.springblade.formmode.utils.TableNameUtil.getDetailTableName(tableName, detailIndex);

            log.info("========== 删除明细表 {} (索引: {}) ==========", detailTableName, detailIndex);

            // 1. 删除数据库表
            dynamicTableService.dropTable(detailTableName);
            log.info("已删除数据库表: {}", detailTableName);

            // 2. 删除对应的字段定义
            fieldDefinitionService.remove(new QueryWrapper<FieldDefinition>()
                    .eq("billid", id)
                    .eq("ismain", 0)
                    .eq("detailtable", detailIndex));
            log.info("已删除明细表 {} 的字段定义", detailIndex);

            return R.data(true);
        } catch (Exception e) {
            log.error("删除明细表失败: " + id + ", detailIndex: " + detailIndex, e);
            return R.fail("删除明细表失败: " + e.getMessage());
        }
    }

    private String mapDbTypeToSql(String dbType, Integer length) {
        if (dbType == null) {
            return "VARCHAR(255)";
        }
        // 支持整数类型码（ecology 标准）和字符串类型名
        String type = dbType.trim().toLowerCase();
        Map<String, String> typeMap = new HashMap<>();
        // 整数类型码
        typeMap.put("1", "VARCHAR");
        typeMap.put("2", "INT");
        typeMap.put("3", "DECIMAL");
        typeMap.put("4", "DATE");
        typeMap.put("5", "DATETIME");
        typeMap.put("6", "TEXT");
        // 字符串类型名
        typeMap.put("varchar", "VARCHAR");
        typeMap.put("int", "INT");
        typeMap.put("decimal", "DECIMAL");
        typeMap.put("date", "DATE");
        typeMap.put("datetime", "DATETIME");
        typeMap.put("text", "TEXT");
        typeMap.put("longtext", "LONGTEXT");

        String sqlType = typeMap.getOrDefault(type, "VARCHAR");
        if (sqlType.equals("VARCHAR")) {
            if (length != null && length > 0) {
                return sqlType + "(" + Math.min(length, 2000) + ")";
            }
            return sqlType + "(255)";
        }
        if (sqlType.equals("DECIMAL")) {
            return "DECIMAL(18,2)";
        }
        return sqlType;
    }
}
