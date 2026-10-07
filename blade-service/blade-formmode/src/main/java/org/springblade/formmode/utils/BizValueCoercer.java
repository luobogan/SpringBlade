package org.springblade.formmode.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 业务表（formtable_main_N / 前端新建的自建表）字段值「按列类型」兜底。
 *
 * <p><b>解决的问题</b>：业务数据落库时，前端/明细子表/下拉等可能送来
 * {@code {value,label}} 对象、数组、空串等值；而目标列若是 decimal/int，
 * 直接塞进去会触发 {@code Incorrect decimal value: '' for column 'xxx'} /
 * {@code Data too long} / {@code Out of range}，导致<b>整行 INSERT/UPDATE 失败</b>
 * （workflow 侧随后回退成「占位 dataId」，业务上表现为"业务数据行未创建成功"）。
 * 丢一个字段远比整行建不出来轻。</p>
 *
 * <p><b>设计要点</b>：列类型在<b>每次写入时</b>从 {@code information_schema} 动态读取，
 * 因此对本项目<b>所有</b>业务表生效 —— 既是存量自建表，也包括前端新建表（建表后其列
 * 同样能被读到），无需任何按表名硬编码。</p>
 *
 * <p>用法：先 {@link #loadColumnMeta} 取列元信息，写值时对每个字段调 {@link #coerce}。
 * 兜底降级均记 warn（列名+原值），便于发现上游脏值，不是静默吞。</p>
 */
@Slf4j
public final class BizValueCoercer {

    private BizValueCoercer() {
    }

    /** 数值型列（int 家族 / decimal / float / double），需按数字解析 */
    private static final Set<String> NUMERIC_TYPES = new HashSet<>(Arrays.asList(
        "tinyint", "smallint", "mediumint", "int", "integer", "bigint",
        "decimal", "numeric", "float", "double", "real"));

    /** 整型列取值范围；越界落库会报 Out of range，故越界降级为 null */
    private static final Map<String, long[]> INT_RANGE = Map.of(
        "tinyint", new long[]{Byte.MIN_VALUE, Byte.MAX_VALUE},
        "smallint", new long[]{Short.MIN_VALUE, Short.MAX_VALUE},
        "mediumint", new long[]{-(1L << 23), (1L << 23) - 1},
        "int", new long[]{Integer.MIN_VALUE, Integer.MAX_VALUE},
        "integer", new long[]{Integer.MIN_VALUE, Integer.MAX_VALUE},
        "bigint", new long[]{Long.MIN_VALUE, Long.MAX_VALUE});

    /** 无需按长度截断的文本类型 */
    private static final Set<String> UNBOUNDED_TEXT = Set.of("text", "tinytext", "mediumtext", "longtext", "json");

    /** 从 JSON 串取「值」：兼容 Select 常见的 {"value":34.5,"label":"x"} / ["34.5"] */
    private static final Pattern VALUE_IN_JSON =
        Pattern.compile("\"(?:value|val|data|id)\"\\s*:\\s*\"?(-?\\d+(?:\\.\\d+)?)");
    /** 兜底：取串中第一个数字 */
    private static final Pattern FIRST_NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 列元信息（按小写列名索引，name为表里真实列名） */
    public static final class ColumnMeta {
        public final String name;
        public final String dataType;
        public final Integer charLen;
        public final Integer scale;

        public ColumnMeta(String name, String dataType, Integer charLen, Integer scale) {
            this.name = name;
            this.dataType = dataType;
            this.charLen = charLen;
            this.scale = scale;
        }
    }

    /**
     * 读取目标表全部列的类型/长度/小数位元信息（小写列名 → ColumnMeta）。
     * 供写值前按列类型兜底；表不存在时返回空 Map。
     */
    public static Map<String, ColumnMeta> loadColumnMeta(JdbcTemplate jdbcTemplate, String tableName) {
        Map<String, ColumnMeta> meta = new LinkedHashMap<>();
        for (Map<String, Object> col : jdbcTemplate.queryForList(
            "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, NUMERIC_SCALE "
                + "FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
            tableName)) {
            Object name = col.get("COLUMN_NAME");
            if (name == null) {
                continue;
            }
            String key = String.valueOf(name).toLowerCase();
            meta.put(key, new ColumnMeta(
                String.valueOf(name),
                asString(col.get("DATA_TYPE")),
                asInt(col.get("CHARACTER_MAXIMUM_LENGTH")),
                asInt(col.get("NUMERIC_SCALE"))
            ));
        }
        return meta;
    }

    /**
     * 按目标列的类型/长度做值兜底，保证「单个字段类型不匹配」不会让整行写入失败。
     *
     * <p>兜底策略（降级均记 warn）：
     * <ul>
     *   <li>数值列：布尔→1/0；按 scale 四舍五入；空串/取不出数字→<b>null</b>；整型越界→<b>null</b>；</li>
     *   <li>字符串列（varchar/char）：超长→截断，防 Data too long；</li>
     *   <li>文本/json 列：原样（对象已被 JSON 化）。</li>
     * </ul>
     */
    public static Object coerce(String col, String dataType, Integer charLen, Integer scale, Object raw) {
        Object v = normalizeValue(raw);
        if (v == null) {
            return null;
        }
        String dt = dataType == null ? "" : dataType;
        if (NUMERIC_TYPES.contains(dt)) {
            return toNumberOrNull(col, dt, scale, v);
        }
        if (UNBOUNDED_TEXT.contains(dt)) {
            return v;
        }
        // varchar / char 等有界字符串：超长截断（按字符数，与 MySQL 字符语义一致）
        if (v instanceof CharSequence && charLen != null && charLen > 0) {
            CharSequence cs = (CharSequence) v;
            if (cs.length() > charLen) {
                log.warn("[formmode] 字段值超长已截断. col={}, len={}->{}, dataType={}", col, cs.length(), charLen, dt);
                return cs.subSequence(0, charLen);
            }
        }
        return v;
    }

    /** 按列元信息兜底（元信息为 null 时原样返回） */
    public static Object coerce(ColumnMeta meta, Object raw) {
        if (meta == null) {
            return normalizeValue(raw);
        }
        return coerce(meta.name, meta.dataType, meta.charLen, meta.scale, raw);
    }

    /** 数值列兜底：取不到合法数字一律降级 null（记 warn），绝不抛异常 */
    private static Object toNumberOrNull(String col, String dataType, Integer scale, Object v) {
        try {
            BigDecimal num;
            if (v instanceof Boolean) {
                num = ((Boolean) v) ? BigDecimal.ONE : BigDecimal.ZERO;
            } else if (v instanceof BigDecimal) {
                num = (BigDecimal) v;
            } else if (v instanceof Number) {
                num = new BigDecimal(v.toString());
            } else {
                String s = String.valueOf(v).trim();
                if (s.isEmpty()) {
                    log.warn("[formmode] 数值列收到空串，按 null 落库. col={}, dataType={}", col, dataType);
                    return null;
                }
                num = parseNumeric(s);
                if (num == null) {
                    log.warn("[formmode] 数值列收到非数字值，按 null 落库. col={}, dataType={}, value={}",
                        col, dataType, abbreviate(s));
                    return null;
                }
            }
            // decimal 保留目标小数位，多余位四舍五入到列精度
            if (scale != null && scale >= 0 && !"float".equals(dataType) && !"double".equals(dataType)) {
                num = num.setScale(scale, RoundingMode.HALF_UP);
            }
            // 整型列：小数按 MySQL 语义四舍五入；越界降级 null（否则 Out of range 整行失败）
            long[] range = INT_RANGE.get(dataType);
            if (range != null) {
                if (num.scale() > 0) {
                    num = num.setScale(0, RoundingMode.HALF_UP);
                }
                if (num.compareTo(BigDecimal.valueOf(range[0])) < 0 || num.compareTo(BigDecimal.valueOf(range[1])) > 0) {
                    log.warn("[formmode] 整型列越界，按 null 落库. col={}, dataType={}, value={}",
                        col, dataType, num.toPlainString());
                    return null;
                }
            }
            return num;
        } catch (Exception e) {
            log.warn("[formmode] 数值列兜底异常，按 null 落库. col={}, dataType={}, err={}",
                col, dataType, e.getMessage());
            return null;
        }
    }

    /** 字符串→数字：纯数字 → JSON 里的 value → 串中第一个数字；取不到返回 null */
    private static BigDecimal parseNumeric(String s) {
        try {
            return new BigDecimal(s);
        } catch (Exception ignored) {
            // 非纯数字，继续尝试 JSON/提取
        }
        Matcher m = VALUE_IN_JSON.matcher(s);
        if (m.find()) {
            try {
                return new BigDecimal(m.group(1));
            } catch (Exception ignored) {
                // 继续兜底
            }
        }
        Matcher f = FIRST_NUMBER.matcher(s);
        if (f.find()) {
            try {
                return new BigDecimal(f.group());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    /** 统一值类型：基本类型原样，复杂对象（明细行/富文本/Select 的 {value,label}）落 JSON 串 */
    private static Object normalizeValue(Object v) {
        if (v == null || v instanceof CharSequence || v instanceof Number
            || v instanceof Boolean || v instanceof java.util.Date) {
            return v;
        }
        try {
            return JsonUtil.toJson(v);
        } catch (Exception e) {
            try {
                return MAPPER.writeValueAsString(v);
            } catch (Exception ex) {
                return String.valueOf(v);
            }
        }
    }

    /** 日志里长值截断，避免刷屏 */
    private static String abbreviate(String s) {
        return s.length() <= 120 ? s : s.substring(0, 120) + "…(len=" + s.length() + ")";
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o).toLowerCase();
    }

    private static Integer asInt(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(o).trim());
        } catch (Exception e) {
            return null;
        }
    }
}