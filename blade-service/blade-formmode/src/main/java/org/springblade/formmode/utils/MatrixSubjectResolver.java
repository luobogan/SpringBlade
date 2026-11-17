package org.springblade.formmode.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 矩阵主体解析器（对标 E9 sharetype=7）。
 *
 * <p>按「当前数据行的字段值」从矩阵表（wf_matrix/wf_matrix_field/wf_matrix_data）
 * 解析出有权用户集合。矩阵三表与 formmode 同库，直接 JDBC 查询，不引入跨服务调用。</p>
 *
 * <p>配置约定（存于 modedatasharerule / modedatashare_single）：
 * <ul>
 *   <li>{@code objid} = matrixId</li>
 *   <li>{@code condition_field} = 业务表字段名（其行值用于匹配矩阵条件字段）</li>
 *   <li>{@code matrix_value_field} = 矩阵结果字段名（wf_matrix_field.field_type=1 的 field_key）</li>
 * </ul>
 * 匹配规则（对齐 E9 MatrixUtil）：所有条件字段 AND；单条件多值逗号分隔任一命中；
 * 结果字段值为逗号分隔用户 ID，取并集、去重、保序。</p>
 */
@Slf4j
@Component
public class MatrixSubjectResolver {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;

    public MatrixSubjectResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 判断当前用户是否命中矩阵授权。
     *
     * @param matrixId        矩阵ID（规则 objid）
     * @param bizField        业务表条件字段名（规则 condition_field）
     * @param valueField      矩阵结果字段名（规则 matrix_value_field）；为空时取第一个结果字段
     * @param row             当前数据行（字段名→值）
     * @param userId          当前用户ID
     * @return 命中 true；无配置/查询失败/未命中 false（fail-closed）
     */
    public boolean matches(Long matrixId, String bizField, String valueField,
                           Map<String, Object> row, Long userId) {
        if (matrixId == null || userId == null || row == null || bizField == null || bizField.isBlank()) {
            return false;
        }
        return resolveUsers(matrixId, bizField, valueField, row).contains(userId);
    }

    /**
     * 按数据行解析矩阵授权的用户 ID 集合。
     */
    public Set<Long> resolveUsers(Long matrixId, String bizField, String valueField, Map<String, Object> row) {
        Set<Long> result = new LinkedHashSet<>();
        if (matrixId == null || bizField == null || bizField.isBlank() || row == null) {
            return result;
        }
        try {
            // 1) 矩阵字段定义：条件字段(field_type=0) + 结果字段(field_type=1)
            List<Map<String, Object>> fieldRows = jdbcTemplate.queryForList(
                "SELECT field_key, field_type FROM wf_matrix_field WHERE matrix_id=? AND is_deleted=0 ORDER BY id",
                matrixId);
            List<String> condKeys = new ArrayList<>();
            List<String> resultKeys = new ArrayList<>();
            for (Map<String, Object> f : fieldRows) {
                Object key = f.get("field_key");
                Object type = f.get("field_type");
                if (key == null || type == null) continue;
                int t = ((Number) type).intValue();
                if (t == 0) condKeys.add(String.valueOf(key));
                else if (t == 1) resultKeys.add(String.valueOf(key));
            }
            if (condKeys.isEmpty() || resultKeys.isEmpty()) {
                return result;
            }
            // 结果字段：优先配置值，否则取第一个
            String targetField = (valueField != null && !valueField.isBlank())
                ? valueField.trim() : resultKeys.get(0);
            if (!resultKeys.contains(targetField)) {
                log.warn("[formmode] 矩阵结果字段不存在. matrixId={}, valueField={}", matrixId, targetField);
                return result;
            }
            // 2) 业务行值 → 条件值映射：约定矩阵条件字段与业务字段同名匹配，
            //    若不同名则用 bizField 的值填充所有条件字段（E9 常见单条件矩阵）
            Object rawBizValue = row.get(bizField);
            if (rawBizValue == null) {
                rawBizValue = row.get(bizField.toLowerCase());
            }
            String bizValue = rawBizValue == null ? "" : String.valueOf(rawBizValue);
            Map<String, String> condValues = new LinkedHashMap<>();
            for (String ck : condKeys) {
                Object cv = row.get(ck);
                condValues.put(ck, (cv == null ? "" : String.valueOf(cv)).isEmpty() ? bizValue : String.valueOf(cv));
            }
            // 3) 逐行匹配矩阵数据
            List<Map<String, Object>> dataRows = jdbcTemplate.queryForList(
                "SELECT data_json FROM wf_matrix_data WHERE matrix_id=? AND is_deleted=0 ORDER BY data_order",
                matrixId);
            for (Map<String, Object> d : dataRows) {
                Object json = d.get("data_json");
                if (json == null) continue;
                Map<String, Object> cells = parseJson(json);
                if (cells == null || !rowMatches(cells, condValues)) continue;
                Object users = cells.get(targetField);
                if (users != null) {
                    for (String id : String.valueOf(users).split(",")) {
                        String t = id.trim();
                        if (!t.isEmpty()) {
                            try { result.add(Long.parseLong(t)); }
                            catch (NumberFormatException ignored) { }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[formmode] 矩阵主体解析失败，按无权限处理. matrixId={}, err={}", matrixId, e.getMessage());
            return new LinkedHashSet<>();
        }
        return result;
    }

    /** 单行条件 AND 匹配；单条件多值（逗号分隔）任一命中 */
    private boolean rowMatches(Map<String, Object> cells, Map<String, String> condValues) {
        for (Map.Entry<String, String> e : condValues.entrySet()) {
            Object cellVal = cells.get(e.getKey());
            String cv = cellVal == null ? "" : String.valueOf(cellVal).trim();
            String expected = e.getValue() == null ? "" : e.getValue().trim();
            if (cv.isEmpty() || expected.isEmpty()) {
                continue; // 空值条件不参与匹配（对齐 E9 文档「空值不参与」护栏）
            }
            boolean hit = false;
            for (String part : cv.split(",")) {
                if (part.trim().equals(expected)) { hit = true; break; }
            }
            if (!hit) return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(Object json) {
        try {
            if (json instanceof Map) {
                return (Map<String, Object>) json;
            }
            return MAPPER.readValue(String.valueOf(json), Map.class);
        } catch (Exception e) {
            log.warn("[formmode] 矩阵 data_json 解析失败. err={}", e.getMessage());
            return null;
        }
    }
}
