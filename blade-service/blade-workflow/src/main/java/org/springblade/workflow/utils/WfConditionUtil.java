package org.springblade.workflow.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 出口条件表达式求值工具（模拟运行 / 流程测试共用）。
 *
 * <p>表达式格式：{@code ${field op value && ...}}，例如 {@code ${main.amount > 1000 && main.type == 'A'}}。
 * 字段 key 形如 {@code main.xxx} / {@code dt1.xxx}，查找时优先精确匹配，其次按字段名后缀匹配。</p>
 *
 * <p>求值失败一律按「假」处理，绝不抛异常，避免阻塞走查主链路。</p>
 */
public final class WfConditionUtil {

    private static final Pattern CONTAINS = Pattern.compile("^([\\w.]+)\\.contains\\(['\"](.*)['\"]\\)$");
    private static final Pattern COMPARE = Pattern.compile("^([\\w.]+)\\s*(>=|<=|==|!=|>|<)\\s*(.*)$");

    private WfConditionUtil() {
    }

    /**
     * 求条件表达式在给定表单数据下是否为真。
     *
     * @param expr     条件表达式（可为空 → 视为真）
     * @param formData 表单数据
     * @return true=条件成立
     */
    public static boolean eval(String expr, Map<String, Object> formData) {
        if (expr == null || expr.isBlank()) {
            return true;
        }
        String body = expr.trim();
        if (body.startsWith("${") && body.endsWith("}")) {
            body = body.substring(2, body.length() - 1);
        }
        String[] segs = body.split("&&");
        for (String seg : segs) {
            seg = seg.trim();
            if (seg.isEmpty()) {
                continue;
            }
            if (!evalSingle(seg, formData)) {
                return false;
            }
        }
        return true;
    }

    /** 求单个比较片段 */
    private static boolean evalSingle(String seg, Map<String, Object> formData) {
        // field.contains('val')
        Matcher cm = CONTAINS.matcher(seg);
        if (cm.find()) {
            Object v = lookupField(cm.group(1), formData);
            return v != null && v.toString().contains(cm.group(2));
        }
        Matcher mm = COMPARE.matcher(seg);
        if (!mm.find()) {
            return false;
        }
        String field = mm.group(1);
        String op = mm.group(2);
        String raw = mm.group(3).trim().replaceAll("^['\"]|['\"]$", "");
        Object fv = lookupField(field, formData);
        if (fv == null) {
            // 仅「不等于」在空值下为真，其余为假
            return "!=".equals(op);
        }
        String fs = fv.toString();
        if (isNumberLike(fs) && isNumberLike(raw)) {
            return compareNum(Double.parseDouble(fs), Double.parseDouble(raw), op);
        }
        int cmp = fs.compareTo(raw);
        switch (op) {
            case "==": return cmp == 0;
            case "!=": return cmp != 0;
            case ">": return cmp > 0;
            case "<": return cmp < 0;
            case ">=": return cmp >= 0;
            case "<=": return cmp <= 0;
            default: return false;
        }
    }

    /** 取字段值：优先精确匹配，其次按字段名后缀匹配（main.xxx / dt1.xxx → xxx） */
    public static Object lookupField(String key, Map<String, Object> formData) {
        if (formData == null) {
            return null;
        }
        if (formData.containsKey(key)) {
            return formData.get(key);
        }
        int idx = key == null ? -1 : key.lastIndexOf('.');
        if (idx >= 0) {
            String suffix = key.substring(idx + 1);
            if (formData.containsKey(suffix)) {
                return formData.get(suffix);
            }
        }
        return null;
    }

    private static boolean isNumberLike(String v) {
        if (v == null || v.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(v);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean compareNum(double a, double b, String op) {
        switch (op) {
            case "==": return a == b;
            case "!=": return a != b;
            case ">": return a > b;
            case "<": return a < b;
            case ">=": return a >= b;
            case "<=": return a <= b;
            default: return false;
        }
    }
}
