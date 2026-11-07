package org.springblade.formmode.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 布局公式解析器（服务端权威重算）。
 *
 * <p>对齐泛微 E9 的公式落地方式：E9 把公式配置存在 {@code workflow_formula_htmllayout}
 * （含 {@code destcell} 目标字段 + {@code formula} 表达式），保存时由
 * {@code FieldInfo.getFormulaNew} 把 destcell 里的字段并入「可保存字段清单」，
 * 再由后端把公式结果写进目标字段。</p>
 *
 * <p>本项目等价物：公式挂在布局 JSON 的 {@code cellData[r][c]} 上——
 * {@code f} = 表达式，{@code formulaCfg.destField} = 目标字段名。
 * 保存前按布局重算一次，把结果写回 {@code fieldValues[destField]}，
 * 从而保证「业务表 / 快照 / 引擎变量」三处一致（前端算错或被篡改都不会写入脏数据）。</p>
 *
 * <p>求值口径与前端
 * {@code ant-design-pro/src/pages/FormMode/ExcelDesign/utils/formulaUtils.ts} 一致：
 * 单元格引用（A1）、区域引用（A1:B3）、字段引用（${field}）、四则与比较运算、白名单函数。</p>
 */
@Slf4j
public class LayoutFormulaResolver {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/** 公式格：坐标 + 表达式 + 目标字段 */
	private static final class FormulaCell {
		private final int row;
		private final int col;
		private final String expr;
		private final String destField;
		/** 目标字段键（主表为字段名；明细表为 dt{i}__字段名） */
		private final String destKey;

		FormulaCell(int row, int col, String expr, String destField, String destKey) {
			this.row = row;
			this.col = col;
			this.expr = expr;
			this.destField = destField;
			this.destKey = destKey;
		}
	}

	/** 单张 sheet 的解析结果 */
	private static final class SheetContext {
		/** 公式格列表 */
		private final List<FormulaCell> formulas = new ArrayList<>();
		/** 坐标(r_c) → 字段名 */
		private final Map<String, String> fieldNameByCoord = new HashMap<>();
		/** 字段名 → 坐标(r_c) */
		private final Map<String, String> coordByFieldName = new HashMap<>();
	}

	/**
	 * 按布局重算公式。
	 *
	 * @param layoutJson  布局 JSON（form_layout.layout_json）
	 * @param fieldValues 当前表单值（key = 字段名 / dt{i}__字段名）
	 * @return 目标字段 → 公式计算结果（无公式时返回空 Map）
	 */
	public static Map<String, Object> resolve(String layoutJson, Map<String, Object> fieldValues) {
		Map<String, Object> out = new LinkedHashMap<>();
		if (layoutJson == null || layoutJson.isEmpty()) {
			return out;
		}
		try {
			JsonNode root = MAPPER.readTree(layoutJson);
			Map<String, Object> values = fieldValues == null ? new HashMap<>() : fieldValues;

			// 主表
			JsonNode sheetsNode = root.get("sheets");
			if (sheetsNode != null && sheetsNode.isObject()) {
				Iterator<Map.Entry<String, JsonNode>> it = sheetsNode.fields();
				while (it.hasNext()) {
					JsonNode sheet = it.next().getValue();
					SheetContext ctx = parseSheet(sheet, "", "");
					evalSheet(ctx, values, out);
				}
			} else if (sheetsNode != null && sheetsNode.isArray()) {
				for (JsonNode sheet : sheetsNode) {
					SheetContext ctx = parseSheet(sheet, "", "");
					evalSheet(ctx, values, out);
				}
			}

			// 明细表：字段键带 dt{i}__ 前缀（与前端 collectFieldValues 口径一致）
			JsonNode dts = root.get("detailTables");
			if (dts != null && dts.isObject()) {
				Iterator<Map.Entry<String, JsonNode>> it = dts.fields();
				while (it.hasNext()) {
					Map.Entry<String, JsonNode> en = it.next();
					String prefix = "dt" + en.getKey() + "__";
					JsonNode sub = en.getValue();
					JsonNode subSheets = sub.get("sheets");
					if (subSheets != null && subSheets.isObject()) {
						Iterator<Map.Entry<String, JsonNode>> sit = subSheets.fields();
						while (sit.hasNext()) {
							SheetContext ctx = parseSheet(sit.next().getValue(), prefix, prefix);
							evalSheet(ctx, values, out);
						}
					}
				}
			}
		} catch (Exception e) {
			log.warn("[blade-workflow] 布局公式解析失败，已降级跳过. ", e);
		}
		return out;
	}

	/** 解析单张 sheet：公式格 + 字段名 ↔ 坐标索引 */
	private static SheetContext parseSheet(JsonNode sheet, String fieldPrefix, String destKeyPrefix) {
		SheetContext ctx = new SheetContext();
		if (sheet == null) {
			return ctx;
		}
		JsonNode cellData = sheet.get("cellData");
		if (cellData == null || !cellData.isObject()) {
			return ctx;
		}
		Iterator<Map.Entry<String, JsonNode>> rows = cellData.fields();
		while (rows.hasNext()) {
			Map.Entry<String, JsonNode> rowEntry = rows.next();
			JsonNode rowObj = rowEntry.getValue();
			if (rowObj == null || !rowObj.isObject()) {
				continue;
			}
			Iterator<Map.Entry<String, JsonNode>> cells = rowObj.fields();
			while (cells.hasNext()) {
				Map.Entry<String, JsonNode> cellEntry = cells.next();
				JsonNode cell = cellEntry.getValue();
				if (cell == null || !cell.isObject()) {
					continue;
				}
				int row = parseInt(rowEntry.getKey());
				int col = parseInt(cellEntry.getKey());
				// 字段名索引（单元格引用 → 字段值）
				JsonNode meta = cell.get("fieldMeta");
				if (meta != null && meta.isObject()) {
					JsonNode fn = meta.get("fieldName");
					if (fn != null && fn.isTextual() && !fn.asText().isEmpty()) {
						ctx.fieldNameByCoord.put(row + "_" + col, fieldPrefix + fn.asText());
						ctx.coordByFieldName.putIfAbsent(fieldPrefix + fn.asText(), row + "_" + col);
					}
				}
				// 公式格
				String expr = cell.hasNonNull("f") ? String.valueOf(cell.get("f").asText()) : null;
				if ((expr == null || expr.trim().isEmpty()) && cell.hasNonNull("v")) {
					String v = cell.get("v").asText();
					if (v != null && v.trim().startsWith("=")) {
						expr = v.trim();
					}
				}
				if (expr == null || expr.trim().isEmpty()) {
					continue;
				}
				JsonNode cfg = cell.get("formulaCfg");
				String destField = null;
				if (cfg != null && cfg.isObject()) {
					JsonNode df = cfg.get("destField");
					if (df != null && df.isTextual() && !df.asText().isEmpty()) {
						destField = df.asText();
					}
				}
				if (destField == null) {
					continue;
				}
				ctx.formulas.add(new FormulaCell(row, col, expr, destField, destKeyPrefix + destField));
			}
		}
		return ctx;
	}

	/** 对单张 sheet 的公式求值（公式引用公式 → DFS；循环 → #CYCLE!） */
	private static void evalSheet(SheetContext ctx, Map<String, Object> values, Map<String, Object> out) {
		if (ctx.formulas.isEmpty()) {
			return;
		}
		SheetEvaluator evaluator = new SheetEvaluator(ctx, values);
		for (FormulaCell fc : ctx.formulas) {
			Object result = evaluator.value(fc.row, fc.col);
			if (result == null) {
				continue;
			}
			if (FormulaEvaluator.isError(result)) {
				// 公式本身有问题：保留用户提交的值，只记日志，不阻断整表保存
				log.warn("[blade-workflow] 公式计算失败，保留原值. dest={}, expr={}, result={}",
						fc.destKey, fc.expr, result);
				continue;
			}
			out.put(fc.destKey, result);
		}
	}

	/**
	 * 单张 sheet 的求值器：公式引用公式走 DFS（cache 去重 + visiting 检环）。
	 */
	private static final class SheetEvaluator {
		private final SheetContext ctx;
		private final Map<String, Object> values;
		private final Map<String, String> exprByCoord = new HashMap<>();
		private final Map<String, Object> cache = new HashMap<>();
		private final Set<String> visiting = new HashSet<>();

		SheetEvaluator(SheetContext ctx, Map<String, Object> values) {
			this.ctx = ctx;
			this.values = values;
			for (FormulaCell fc : ctx.formulas) {
				exprByCoord.put(fc.row + "_" + fc.col, fc.expr);
			}
		}

		Object value(int row, int col) {
			String key = row + "_" + col;
			String expr = exprByCoord.get(key);
			if (expr == null) {
				// 普通格：按 fieldMeta 映射到字段值；没有字段元数据时取 0
				String fieldName = ctx.fieldNameByCoord.get(key);
				if (fieldName == null) {
					return 0d;
				}
				Object v = values.get(fieldName);
				return v == null ? 0d : v;
			}
			if (cache.containsKey(key)) {
				return cache.get(key);
			}
			if (visiting.contains(key)) {
				return FormulaEvaluator.ERR_CYCLE;
			}
			visiting.add(key);
			Object result = FormulaEvaluator.evaluate(expr,
					(this::value),
					this::fieldValue);
			visiting.remove(key);
			cache.put(key, result);
			return result;
		}

		/** 字段引用取值：优先字段名键；否则按 fieldMeta 定位到所在格再求值 */
		private Object fieldValue(String name) {
			Object direct = values.get(name);
			if (direct != null) {
				return direct;
			}
			String coord = ctx.coordByFieldName.get(name);
			if (coord == null) {
				return 0d;
			}
			int idx = coord.indexOf('_');
			if (idx <= 0) {
				return 0d;
			}
			int row = parseInt(coord.substring(0, idx));
			int col = parseInt(coord.substring(idx + 1));
			return value(row, col);
		}
	}

	private static int parseInt(String s) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return 0;
		}
	}
}
