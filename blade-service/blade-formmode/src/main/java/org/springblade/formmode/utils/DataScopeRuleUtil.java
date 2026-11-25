package org.springblade.formmode.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 数据范围规则的多权限/多主体解析工具。 */
public final class DataScopeRuleUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DataScopeRuleUtil() { }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScopeSubject {
        private Integer objtype;
        private Long objid;
        private Integer includeSub;
    }

    public static List<Integer> parseRightTypes(String csv) {
        List<Integer> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) return out;
        for (String p : csv.split(",")) {
            if (p == null || p.isBlank()) continue;
            try {
                int v = Integer.parseInt(p.trim());
                if (v >= 1 && v <= 4 && !out.contains(v)) out.add(v);
            } catch (Exception ignored) { }
        }
        return out;
    }

    public static String joinRightTypes(List<Integer> types) {
        if (types == null || types.isEmpty()) return null;
        Set<Integer> set = new LinkedHashSet<>(types);
        return String.join(",", set.stream().map(String::valueOf).toList());
    }

    public static List<ScopeSubject> parseSubjects(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            List<ScopeSubject> list = MAPPER.readValue(json, new TypeReference<List<ScopeSubject>>() { });
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static String toJson(List<?> subjects) {
        if (subjects == null || subjects.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(subjects);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean containsAny(List<Integer> types, int... candidates) {
        if (types == null || types.isEmpty()) return false;
        for (int c : candidates) if (types.contains(c)) return true;
        return false;
    }
}
