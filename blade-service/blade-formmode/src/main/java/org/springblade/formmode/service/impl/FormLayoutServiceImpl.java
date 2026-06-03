package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springblade.formmode.entity.FormLayout;
import org.springblade.formmode.mapper.FormLayoutMapper;
import org.springblade.formmode.service.IFormLayoutService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 表单布局服务实现类
 * 实现JSON解析和生成逻辑，兼容泛微E9格式
 */
@Service
@Slf4j
public class FormLayoutServiceImpl extends ServiceImpl<FormLayoutMapper, FormLayout> implements IFormLayoutService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public FormLayout getByFormId(Long formId) {
        return lambdaQuery()
                .eq(FormLayout::getFormId, formId)
                .eq(FormLayout::getStatus, 1)
                .one();
    }

    @Override
    public boolean saveFormLayout(FormLayout formLayout) {
        if (formLayout.getId() == null) {
            return save(formLayout);
        } else {
            return updateById(formLayout);
        }
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
