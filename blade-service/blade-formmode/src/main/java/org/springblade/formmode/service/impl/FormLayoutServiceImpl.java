package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.formmode.entity.FormLayout;
import org.springblade.formmode.entity.WorkflowBill;
import org.springblade.formmode.mapper.FormLayoutMapper;
import org.springblade.formmode.mapper.WorkflowBillMapper;
import org.springblade.formmode.service.IFormLayoutService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 表单布局服务实现类
 * 实现JSON解析和生成逻辑，兼容泛微E9格式
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FormLayoutServiceImpl extends ServiceImpl<FormLayoutMapper, FormLayout> implements IFormLayoutService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WorkflowBillMapper workflowBillMapper;

    /** 默认布局类型：0 编辑布局 */
    private static final int DEFAULT_LAYOUT_TYPE = 0;

    @Override
    public FormLayout getByFormId(Long formId) {
        // 兼容旧调用：返回该表单最新一条布局
        return latestByFormId(formId);
    }

    @Override
    public FormLayout getByFormId(Long formId, Integer layoutType, String nodeKey) {
        if (formId == null) {
            return null;
        }
        int type = (layoutType == null) ? DEFAULT_LAYOUT_TYPE : layoutType;
        String node = (nodeKey == null || nodeKey.isBlank()) ? null : nodeKey;

        // ① 节点级 + 指定类型
        if (node != null) {
            FormLayout l = findOne(formId, type, node);
            if (l != null) {
                return l;
            }
            // ② 节点级 + 默认类型
            if (type != DEFAULT_LAYOUT_TYPE) {
                l = findOne(formId, DEFAULT_LAYOUT_TYPE, node);
                if (l != null) {
                    return l;
                }
            }
        }
        // ③ 表单级 + 指定类型
        FormLayout l = findOne(formId, type, null);
        if (l != null) {
            return l;
        }
        // ④ 表单级 + 默认类型
        if (type != DEFAULT_LAYOUT_TYPE) {
            l = findOne(formId, DEFAULT_LAYOUT_TYPE, null);
            if (l != null) {
                return l;
            }
        }
        // ⑤ 兼容旧数据：任意类型/节点的最新一条
        return latestByFormId(formId);
    }

    @Override
    public List<FormLayout> listByFormId(Long formId, Integer layoutType, String nodeKey) {
        return lambdaQuery()
                .eq(FormLayout::getFormId, formId)
                .eq(FormLayout::getStatus, 1)
                .eq(layoutType != null, FormLayout::getLayoutType, layoutType)
                .eq(nodeKey != null && !nodeKey.isBlank(), FormLayout::getNodeKey, nodeKey)
                .orderByAsc(FormLayout::getLayoutType)
                .orderByDesc(FormLayout::getCreateTime)
                .list();
    }

    /**
     * 精确匹配：formId + 类型 + 节点（node 为空时匹配 node_key 为空/空串）
     */
    private FormLayout findOne(Long formId, int layoutType, String nodeKey) {
        LambdaQueryWrapper<FormLayout> q = Wrappers.<FormLayout>lambdaQuery()
                .eq(FormLayout::getFormId, formId)
                .eq(FormLayout::getStatus, 1)
                .eq(FormLayout::getLayoutType, layoutType);
        if (nodeKey == null || nodeKey.isBlank()) {
            q.and(w -> w.isNull(FormLayout::getNodeKey).or().eq(FormLayout::getNodeKey, ""));
        } else {
            q.eq(FormLayout::getNodeKey, nodeKey);
        }
        q.orderByDesc(FormLayout::getCreateTime);
        List<FormLayout> list = list(q);
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    /**
     * 该表单最新一条布局（旧行为兜底）
     */
    private FormLayout latestByFormId(Long formId) {
        // 使用 list() 替代 one()，避免重复数据导致异常；多条时按创建时间取最新
        List<FormLayout> list = lambdaQuery()
                .eq(FormLayout::getFormId, formId)
                .eq(FormLayout::getStatus, 1)
                .orderByDesc(FormLayout::getCreateTime)
                .list();

        if (list != null && !list.isEmpty()) {
            if (list.size() > 1) {
                log.warn("发现多条相同 formId 的布局记录, formId={}, 记录数={}, 返回最新一条", formId, list.size());
            }
            return list.get(0);
        }
        return null;
    }

    /**
     * 按「表单 + 布局类型 + 流程节点」查找已有布局（不区分 status，用于 upsert 判定）。
     */
    private FormLayout findAny(Long formId, int layoutType, String nodeKey) {
        LambdaQueryWrapper<FormLayout> q = Wrappers.<FormLayout>lambdaQuery()
                .eq(FormLayout::getFormId, formId)
                .eq(FormLayout::getLayoutType, layoutType);
        if (nodeKey == null || nodeKey.isBlank()) {
            q.and(w -> w.isNull(FormLayout::getNodeKey).or().eq(FormLayout::getNodeKey, ""));
        } else {
            q.eq(FormLayout::getNodeKey, nodeKey);
        }
        q.orderByDesc(FormLayout::getCreateTime);
        List<FormLayout> list = list(q);
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    @Override
    public boolean saveFormLayout(FormLayout formLayout) {
        // 检查 formId 是否存在于 workflow_bill 表
        if (formLayout.getFormId() != null) {
            WorkflowBill bill = workflowBillMapper.selectById(formLayout.getFormId());
            if (bill == null) {
                log.error("保存表单布局失败: formId={} 不存在于 workflow_bill 表", formLayout.getFormId());
                throw new IllegalArgumentException("表单ID不存在: " + formLayout.getFormId());
            }
        }

        // Upsert：保存请求不带 id（前端不传），若只按 id 判空，每次保存都会 INSERT 一条新记录，
        // 同一「表单+类型+节点」堆积多行；读取时按 create_time desc 取第一条，
        // 同秒写入/时间精度不足时会取到旧行 → 表现为「点了保存但数据没存上」。
        // 故先按业务键定位已有记录，命中则更新，未命中才新增。
        if (formLayout.getId() == null) {
            int type = formLayout.getLayoutType() == null ? DEFAULT_LAYOUT_TYPE : formLayout.getLayoutType();
            FormLayout existing = findAny(formLayout.getFormId(), type, formLayout.getNodeKey());
            if (existing != null) {
                formLayout.setId(existing.getId());
                log.info("保存表单布局: 命中已有记录 id={}, formId={}, layoutType={}, nodeKey={}",
                        existing.getId(), formLayout.getFormId(), type, formLayout.getNodeKey());
            }
        }

        if (formLayout.getId() == null) {
            return save(formLayout);
        } else {
            return updateById(formLayout);
        }
    }

    @Override
    public boolean deleteByNode(Long formId, String nodeKey) {
        if (formId == null || nodeKey == null || nodeKey.isBlank()) {
            return false;
        }
        List<FormLayout> list = lambdaQuery()
                .eq(FormLayout::getFormId, formId)
                .eq(FormLayout::getNodeKey, nodeKey)
                .list();
        if (list == null || list.isEmpty()) {
            return true;
        }
        List<Long> ids = list.stream().map(FormLayout::getId).filter(Objects::nonNull).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return true;
        }
        log.info("删除节点布局: formId={}, nodeKey={}, 条数={}", formId, nodeKey, ids.size());
        return removeByIds(ids);
    }

    @Override
    public String parseLayoutJson(String layoutJson) {
        try {
            // 解析泛微E9格式的布局JSON
            JsonNode rootNode = objectMapper.readTree(layoutJson);

            // 兼容E9格式：可能包含sheets、cells、layout等字段
            if (rootNode.has("sheets")) {
                // 多Sheet格式
                JsonNode sheets = rootNode.get("sheets");
                log.info("解析多Sheet布局，Sheet数量: {}", sheets.size());
                return parseSheetsLayout(sheets);
            } else if (rootNode.has("cells")) {
                // 单Sheet格式
                JsonNode cells = rootNode.get("cells");
                log.info("解析单Sheet布局，单元格数量: {}", cells.size());
                return parseCellsLayout(cells);
            } else if (rootNode.has("layout")) {
                // 布局配置格式
                JsonNode layout = rootNode.get("layout");
                log.info("解析布局配置");
                return parseLayoutConfig(layout);
            } else {
                log.warn("未知的布局JSON格式");
                return "未知的布局格式";
            }
        } catch (Exception e) {
            log.error("解析布局JSON失败: {}", e.getMessage(), e);
            return "解析失败: " + e.getMessage();
        }
    }

    /**
     * 解析多Sheet布局
     */
    private String parseSheetsLayout(JsonNode sheets) {
        try {
            List<Map<String, Object>> result = new ArrayList<>();

            sheets.fields().forEachRemaining(sheetEntry -> {
                String sheetName = sheetEntry.getKey();
                JsonNode sheetData = sheetEntry.getValue();

                Map<String, Object> sheetInfo = new HashMap<>();
                sheetInfo.put("sheetName", sheetName);

                // 解析单元格数据
                if (sheetData.has("cellData")) {
                    JsonNode cellData = sheetData.get("cellData");
                    List<Map<String, Object>> cells = parseCellData(cellData);
                    sheetInfo.put("cells", cells);
                }

                result.add(sheetInfo);
            });

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("解析多Sheet布局失败: {}", e.getMessage(), e);
            return "解析失败";
        }
    }

    /**
     * 解析单Sheet布局
     */
    private String parseCellsLayout(JsonNode cells) {
        try {
            List<Map<String, Object>> result = parseCellData(cells);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("解析单Sheet布局失败: {}", e.getMessage(), e);
            return "解析失败";
        }
    }

    /**
     * 解析布局配置
     */
    private String parseLayoutConfig(JsonNode layout) {
        try {
            // 解析布局配置，提取字段信息
            List<Map<String, Object>> fields = new ArrayList<>();

            if (layout.isArray()) {
                for (JsonNode fieldNode : layout) {
                    Map<String, Object> fieldInfo = new HashMap<>();
                    fieldInfo.put("fieldId", fieldNode.has("fieldId") ? fieldNode.get("fieldId").asText() : "");
                    fieldInfo.put("fieldName", fieldNode.has("fieldName") ? fieldNode.get("fieldName").asText() : "");
                    fieldInfo.put("fieldLabel", fieldNode.has("fieldLabel") ? fieldNode.get("fieldLabel").asText() : "");
                    fieldInfo.put("fieldType", fieldNode.has("fieldType") ? fieldNode.get("fieldType").asText() : "");
                    fieldInfo.put("row", fieldNode.has("row") ? fieldNode.get("row").asInt() : 0);
                    fieldInfo.put("col", fieldNode.has("col") ? fieldNode.get("col").asInt() : 0);
                    fields.add(fieldInfo);
                }
            }

            return objectMapper.writeValueAsString(fields);
        } catch (Exception e) {
            log.error("解析布局配置失败: {}", e.getMessage(), e);
            return "解析失败";
        }
    }

    /**
     * 解析单元格数据
     */
    private List<Map<String, Object>> parseCellData(JsonNode cellData) {
        List<Map<String, Object>> cells = new ArrayList<>();

        if (cellData.isObject()) {
            Iterator<String> rowKeys = cellData.fieldNames();
            while (rowKeys.hasNext()) {
                String rowKey = rowKeys.next();
                JsonNode rowData = cellData.get(rowKey);

                if (rowData.isObject()) {
                    Iterator<String> colKeys = rowData.fieldNames();
                    while (colKeys.hasNext()) {
                        String colKey = colKeys.next();
                        JsonNode cellNode = rowData.get(colKey);

                        Map<String, Object> cellInfo = new HashMap<>();
                        cellInfo.put("row", Integer.parseInt(rowKey));
                        cellInfo.put("col", Integer.parseInt(colKey));
                        cellInfo.put("value", cellNode.has("v") ? cellNode.get("v").asText() : "");

                        // 提取字段信息
                        if (cellNode.has("fieldInfo")) {
                            JsonNode fieldInfo = cellNode.get("fieldInfo");
                            cellInfo.put("fieldId", fieldInfo.has("fieldId") ? fieldInfo.get("fieldId").asText() : "");
                            cellInfo.put("fieldName", fieldInfo.has("fieldName") ? fieldInfo.get("fieldName").asText() : "");
                            cellInfo.put("fieldLabel", fieldInfo.has("fieldLabel") ? fieldInfo.get("fieldLabel").asText() : "");
                            cellInfo.put("fieldType", fieldInfo.has("fieldType") ? fieldInfo.get("fieldType").asText() : "");
                        }

                        cells.add(cellInfo);
                    }
                }
            }
        }

        return cells;
    }

    /**
     * 生成泛微E9格式的布局JSON
     */
    public String generateLayoutJson(List<Map<String, Object>> layoutData) {
        try {
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> sheets = new ArrayList<>();

            // 按Sheet分组
            Map<String, List<Map<String, Object>>> sheetGroups = new HashMap<>();
            for (Map<String, Object> cell : layoutData) {
                String sheetName = (String) cell.getOrDefault("sheetName", "Sheet1");
                sheetGroups.computeIfAbsent(sheetName, k -> new ArrayList<>()).add(cell);
            }

            // 生成每个Sheet的数据
            for (Map.Entry<String, List<Map<String, Object>>> entry : sheetGroups.entrySet()) {
                String sheetName = entry.getKey();
                List<Map<String, Object>> cells = entry.getValue();

                Map<String, Object> sheetData = new HashMap<>();
                sheetData.put("sheetName", sheetName);

                // 生成单元格数据
                Map<String, Object> cellData = new HashMap<>();
                for (Map<String, Object> cell : cells) {
                    int row = (int) cell.getOrDefault("row", 0);
                    int col = (int) cell.getOrDefault("col", 0);

                    Map<String, Object> cellValue = new HashMap<>();
                    cellValue.put("v", cell.getOrDefault("value", ""));

                    // 添加字段信息
                    if (cell.containsKey("fieldInfo")) {
                        cellValue.put("fieldInfo", cell.get("fieldInfo"));
                    }

                    cellData.put(String.valueOf(row), cellValue);
                }

                sheetData.put("cellData", cellData);
                sheets.add(sheetData);
            }

            result.put("sheets", sheets);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("生成布局JSON失败: {}", e.getMessage(), e);
            return "{}";
        }
    }
}
