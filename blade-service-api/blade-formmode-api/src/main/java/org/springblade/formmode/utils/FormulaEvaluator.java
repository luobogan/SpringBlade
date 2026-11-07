package org.springblade.formmode.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Excel 布局公式求值器（服务端）。
 *
 * <p>对齐泛微 E9 的公式体系：E9 前端计算（{@code formulaNew_wev8.js} / {@code formcalculate_wev8.js}
 * 用 eval）、后端只做配置持久化与结果入库；本项目服务端增加一次<b>权威重算</b>，
 * 保证「快照 / 业务表 / 引擎变量」三处一致，防止前端算错或被篡改后写入脏数据。</p>
 *
 * <p>安全策略与前端 {@code ant-design-pro/src/pages/FormMode/ExcelDesign/utils/formulaUtils.ts} 一致：
 * <b>不使用 eval / ScriptEngine</b>，改为受限的递归下降解析器：
 * 仅支持「单元格引用 / 区域引用 / 字段引用 {@code ${field}} / 数字 / 字符串 /
 * 四则与比较运算 / 白名单函数」，未知函数与非法表达式一律返回 Excel 风格错误值。</p>
 *
 * <p>支持函数：{@code SUM AVERAGE MIN MAX COUNT ROUND ABS IF INT LEN AND OR NOT CONCAT
 * CONCATENATE MOD SQRT POWER CEILING FLOOR TRIM ISBLANK RMB}。</p>
 */
public class FormulaEvaluator {

	/** Excel 风格错误值 */
	public static final String ERR_CYCLE = "#CYCLE!";
	public static final String ERR_DIV0 = "#DIV/0!";
	public static final String ERR_VALUE = "#VALUE!";
	public static final String ERR_NAME = "#NAME?";
	public static final String ERR_NUM = "#NUM!";
	public static final String ERR_ERROR = "#ERROR!";

	/** 白名单函数 */
	private static final Set<String> FUNCTIONS = new HashSet<>(java.util.Arrays.asList(
			"SUM", "AVERAGE", "MIN", "MAX", "COUNT", "ROUND", "ABS", "IF", "INT", "LEN", "AND", "OR",
			"NOT", "CONCAT", "CONCATENATE", "MOD", "SQRT", "POWER", "CEILING", "FLOOR", "TRIM", "ISBLANK",
			"RMB"));

	/** 单元格值解析器：按 0-based 行列取当前表单值 */
	@FunctionalInterface
	public interface CellResolver {
		Object get(int row, int col);
	}

	/** 字段值解析器：按字段名取值 */
	@FunctionalInterface
	public interface FieldResolver {
		Object get(String fieldName);
	}

	private final CellResolver cellResolver;
	private final FieldResolver fieldResolver;

	/** 词法单元（解析游标） */
	private final List<Token> toks;
	private int pos = 0;

	public FormulaEvaluator(CellResolver cellResolver, FieldResolver fieldResolver, List<Token> toks) {
		this.cellResolver = cellResolver;
		this.fieldResolver = fieldResolver;
		this.toks = toks;
	}

	/**
	 * 求值入口：出错返回 Excel 风格错误串，不抛异常（与前端一致，避免一条坏公式打断整表保存）。
	 */
	public static Object evaluate(String expression, CellResolver cellResolver, FieldResolver fieldResolver) {
		if (expression == null || expression.trim().isEmpty()) {
			return "";
		}
		try {
			String src = expression.trim();
			if (src.startsWith("=")) {
				src = src.substring(1);
			}
			FormulaEvaluator evaluator = new FormulaEvaluator(cellResolver, fieldResolver, tokenize(src));
			return evaluator.parseComparison();
		} catch (Exception e) {
			return ERR_ERROR;
		}
	}

	// ────────────────────────────── 词法 ──────────────────────────────

	private enum Tk {
		NUM, STR, ID, FIELD, OP, LP, RP, COMMA, COLON
	}

	private static final class Token {
		final Tk type;
		final String text;
		final double num;

		Token(Tk type, String text, double num) {
			this.type = type;
			this.text = text;
			this.num = num;
		}

		static Token of(Tk t, String text) {
			return new Token(t, text, 0);
		}

		static Token num(double v) {
			return new Token(Tk.NUM, null, v);
		}
	}

	private static List<Token> tokenize(String src) {
		List<Token> out = new ArrayList<>();
		int i = 0;
		int len = src.length();
		while (i < len) {
			char ch = src.charAt(i);
			if (Character.isWhitespace(ch)) {
				i++;
				continue;
			}
			// 字符串：'...' 或 "..."
			if (ch == '\'' || ch == '"') {
				char quote = ch;
				StringBuilder sb = new StringBuilder();
				i++;
				while (i < len && src.charAt(i) != quote) {
					if (src.charAt(i) == '\\' && i + 1 < len) {
						sb.append(src.charAt(i + 1));
						i += 2;
					} else {
						sb.append(src.charAt(i++));
					}
				}
				i++;
				out.add(Token.of(Tk.STR, sb.toString()));
				continue;
			}
			// 字段引用 ${field}
			if (ch == '$' && i + 1 < len && src.charAt(i + 1) == '{') {
				int end = src.indexOf('}', i + 2);
				if (end > 0) {
					out.add(Token.of(Tk.FIELD, src.substring(i + 2, end).trim()));
					i = end + 1;
					continue;
				}
			}
			// 数字
			if (Character.isDigit(ch) || (ch == '.' && i + 1 < len && Character.isDigit(src.charAt(i + 1)))) {
				int start = i;
				boolean dot = false;
				while (i < len) {
					char c = src.charAt(i);
					if (Character.isDigit(c)) {
						i++;
					} else if (c == '.' && !dot) {
						dot = true;
						i++;
					} else {
						break;
					}
				}
				out.add(Token.num(Double.parseDouble(src.substring(start, i))));
				continue;
			}
			// 标识符（函数名 / 单元格引用 / TRUE / FALSE）
			if (Character.isLetter(ch) || ch == '_') {
				int start = i;
				while (i < len) {
					char c = src.charAt(i);
					if (Character.isLetterOrDigit(c) || c == '_') {
						i++;
					} else {
						break;
					}
				}
				out.add(Token.of(Tk.ID, src.substring(start, i)));
				continue;
			}
			// 双字符运算符
			if (i + 1 < len) {
				String two = src.substring(i, i + 2);
				if (">=".equals(two) || "<=".equals(two) || "<>".equals(two) || "!=".equals(two)) {
					out.add(Token.of(Tk.OP, two));
					i += 2;
					continue;
				}
			}
			switch (ch) {
				case '(':
					out.add(Token.of(Tk.LP, "("));
					break;
				case ')':
					out.add(Token.of(Tk.RP, ")"));
					break;
				case ',':
					out.add(Token.of(Tk.COMMA, ","));
					break;
				case ':':
					out.add(Token.of(Tk.COLON, ":"));
					break;
				case '+':
				case '-':
				case '*':
				case '/':
				case '^':
				case '=':
				case '>':
				case '<':
					out.add(Token.of(Tk.OP, String.valueOf(ch)));
					break;
				default:
					throw new IllegalArgumentException("非法字符 " + ch);
			}
			i++;
		}
		return out;
	}

	// ────────────────────────────── 语法 ──────────────────────────────

	private Token peek() {
		return pos < toks.size() ? toks.get(pos) : null;
	}

	private boolean eat(Tk type) {
		Token t = peek();
		if (t == null || t.type != type) {
			return false;
		}
		pos++;
		return true;
	}

	private Object parseComparison() {
		Object left = parseAdditive();
		Token p = peek();
		if (p != null && p.type == Tk.OP && isCompareOp(p.text)) {
			pos++;
			Object right = parseAdditive();
			if (isError(left)) {
				return left;
			}
			if (isError(right)) {
				return right;
			}
			double a = toNum(left);
			double b = toNum(right);
			switch (p.text) {
				case ">":
					return a > b;
				case "<":
					return a < b;
				case ">=":
					return a >= b;
				case "<=":
					return a <= b;
				case "<>":
				case "!=":
					return a != b;
				default:
					return a == b;
			}
		}
		return left;
	}

	private static boolean isCompareOp(String op) {
		return ">".equals(op) || "<".equals(op) || ">=".equals(op) || "<=".equals(op)
				|| "<>".equals(op) || "!=".equals(op);
	}

	private Object parseAdditive() {
		Object left = parseMultiplicative();
		for (;;) {
			Token p = peek();
			if (p == null || p.type != Tk.OP || (!"+".equals(p.text) && !"-".equals(p.text))) {
				break;
			}
			pos++;
			Object right = parseMultiplicative();
			if (isError(left)) {
				continue;
			}
			if (isError(right)) {
				left = right;
				continue;
			}
			left = "+".equals(p.text) ? toNum(left) + toNum(right) : toNum(left) - toNum(right);
		}
		return left;
	}

	private Object parseMultiplicative() {
		Object left = parsePower();
		for (;;) {
			Token p = peek();
			if (p == null || p.type != Tk.OP || (!"*".equals(p.text) && !"/".equals(p.text))) {
				break;
			}
			pos++;
			Object right = parsePower();
			if (isError(left)) {
				continue;
			}
			if (isError(right)) {
				left = right;
				continue;
			}
			double b = toNum(right);
			if ("/".equals(p.text) && b == 0) {
				left = ERR_DIV0;
				continue;
			}
			left = "*".equals(p.text) ? toNum(left) * b : toNum(left) / b;
		}
		return left;
	}

	private Object parsePower() {
		Object left = parseUnary();
		Token p = peek();
		if (p != null && p.type == Tk.OP && "^".equals(p.text)) {
			pos++;
			Object right = parsePower();
			if (isError(left)) {
				return left;
			}
			if (isError(right)) {
				return right;
			}
			return Math.pow(toNum(left), toNum(right));
		}
		return left;
	}

	private Object parseUnary() {
		Token p = peek();
		if (p != null && p.type == Tk.OP && ("-".equals(p.text) || "+".equals(p.text))) {
			pos++;
			Object v = parseUnary();
			if (isError(v)) {
				return v;
			}
			return "-".equals(p.text) ? -toNum(v) : toNum(v);
		}
		return parsePrimary();
	}

	private Object parsePrimary() {
		Token p = peek();
		if (p == null) {
			throw new IllegalStateException("表达式不完整");
		}
		if (p.type == Tk.NUM) {
			pos++;
			return p.num;
		}
		if (p.type == Tk.STR) {
			pos++;
			return p.text;
		}
		if (p.type == Tk.FIELD) {
			pos++;
			Object v = fieldResolver.get(p.text);
			return v == null ? 0 : v;
		}
		if (p.type == Tk.LP) {
			pos++;
			Object v = parseComparison();
			if (!eat(Tk.RP)) {
				throw new IllegalStateException("缺少右括号");
			}
			return v;
		}
		if (p.type == Tk.ID) {
			pos++;
			String upper = p.text.toUpperCase();
			Token next = peek();
			if (next != null && next.type == Tk.LP) {
				boolean known = FUNCTIONS.contains(upper);
				pos++;
				List<Object> args = new ArrayList<>();
				if (!eat(Tk.RP)) {
					do {
						// 区域引用 A1:B3：必须在消耗参数前判定
						Token start = peek();
						Token maybeColon = pos + 1 < toks.size() ? toks.get(pos + 1) : null;
						Token end = pos + 2 < toks.size() ? toks.get(pos + 2) : null;
						if (start != null && start.type == Tk.ID && maybeColon != null
								&& maybeColon.type == Tk.COLON && end != null && end.type == Tk.ID) {
							pos += 3;
							int[] from = parseCellRef(start.text);
							int[] to = parseCellRef(end.text);
							if (from == null || to == null) {
								args.add(ERR_NAME);
							} else {
								List<Object> cells = new ArrayList<>();
								for (int r = Math.min(from[0], to[0]); r <= Math.max(from[0], to[0]); r++) {
									for (int c = Math.min(from[1], to[1]); c <= Math.max(from[1], to[1]); c++) {
										cells.add(cellResolver.get(r, c));
									}
								}
								args.add(cells);
							}
						} else {
							args.add(parseComparison());
						}
					} while (eat(Tk.COMMA));
					if (!eat(Tk.RP)) {
						throw new IllegalStateException("缺少右括号");
					}
				}
				if (!known) {
					return ERR_NAME;
				}
				return applyFunction(upper, args);
			}
			int[] ref = parseCellRef(p.text);
			if (ref != null) {
				return cellResolver.get(ref[0], ref[1]);
			}
			if ("TRUE".equals(upper)) {
				return 1d;
			}
			if ("FALSE".equals(upper)) {
				return 0d;
			}
			return ERR_NAME;
		}
		throw new IllegalStateException("无法解析的表达式");
	}

	// ────────────────────────────── 工具 ──────────────────────────────

	/** 单元格引用 A1 → {row, col}（0-based）；非法返回 null */
	public static int[] parseCellRef(String ref) {
		if (ref == null) {
			return null;
		}
		int i = 0;
		StringBuilder letters = new StringBuilder();
		while (i < ref.length() && Character.isLetter(ref.charAt(i))) {
			letters.append(Character.toUpperCase(ref.charAt(i)));
			i++;
		}
		if (letters.length() == 0 || i >= ref.length()) {
			return null;
		}
		StringBuilder digits = new StringBuilder();
		while (i < ref.length() && Character.isDigit(ref.charAt(i))) {
			digits.append(ref.charAt(i));
			i++;
		}
		if (digits.length() == 0) {
			return null;
		}
		int col = 0;
		for (int k = 0; k < letters.length(); k++) {
			col = col * 26 + (letters.charAt(k) - 'A' + 1);
		}
		int row = Integer.parseInt(digits.toString()) - 1;
		if (col <= 0 || row < 0) {
			return null;
		}
		return new int[] {row, col - 1};
	}

	/** 是否错误值 */
	public static boolean isError(Object v) {
		return v instanceof String && ((String) v).matches("#[A-Z0-9/!?]+!?");
	}

	/** 转数值：空值按 0，非法按 0 */
	public static double toNum(Object v) {
		if (v == null) {
			return 0d;
		}
		if (v instanceof Number) {
			return ((Number) v).doubleValue();
		}
		if (isError(v)) {
			return Double.NaN;
		}
		String s = String.valueOf(v).replace(",", "").trim();
		if (s.isEmpty()) {
			return 0d;
		}
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return 0d;
		}
	}

	/** 数值 → 人民币大写（对齐 E9 numberChangeToChinese） */
	public static String numberToChineseCapital(Object value) {
		double n = toNum(value);
		if (Double.isNaN(n)) {
			return "";
		}
		boolean negative = n < 0;
		double abs = Math.abs(n);
		String fixed = String.format(java.util.Locale.ROOT, "%.2f", abs);
		String[] parts = fixed.split("\\.");
		long intVal = (long) Double.parseDouble(parts[0]);
		int jiao = parts.length > 1 && parts[1].length() > 0 ? Character.digit(parts[1].charAt(0), 10) : 0;
		int fen = parts.length > 1 && parts[1].length() > 1 ? Character.digit(parts[1].charAt(1), 10) : 0;
		StringBuilder sb = new StringBuilder("人民币");
		sb.append(integerToChinese(intVal)).append("元");
		if (jiao == 0 && fen == 0) {
			sb.append("整");
		} else {
			if (jiao > 0) {
				sb.append(CN_DIGITS[jiao]).append("角");
			}
			if (fen > 0) {
				if (jiao == 0) {
					sb.append("零");
				}
				sb.append(CN_DIGITS[fen]).append("分");
			} else {
				sb.append("整");
			}
		}
		return (negative ? "负" : "") + sb;
	}

	private static final String[] CN_DIGITS = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
	private static final String[] CN_UNITS = {"", "拾", "佰", "仟"};
	private static final String[] CN_BIG_UNITS = {"", "万", "亿", "兆"};

	/** 整数 → 中文 */
	public static String integerToChinese(long num) {
		if (num <= 0) {
			return "零";
		}
		List<Integer> groups = new ArrayList<>();
		long n = num;
		while (n > 0) {
			groups.add((int) (n % 10000));
			n /= 10000;
		}
		StringBuilder out = new StringBuilder();
		for (int g = groups.size() - 1; g >= 0; g--) {
			int val = groups.get(g);
			if (val == 0) {
				if (out.length() > 0 && !out.toString().endsWith("零")) {
					out.append('零');
				}
				continue;
			}
			String s = String.format("%04d", val);
			StringBuilder part = new StringBuilder();
			boolean pendingZero = false;
			for (int i = 0; i < 4; i++) {
				int d = Character.digit(s.charAt(i), 10);
				if (d == 0) {
					pendingZero = true;
					continue;
				}
				if (pendingZero && part.length() > 0) {
					part.append('零');
				}
				pendingZero = false;
				part.append(CN_DIGITS[d]).append(CN_UNITS[3 - i]);
			}
			out.append(part).append(CN_BIG_UNITS[g]);
		}
		String res = out.toString().replaceAll("零+", "零");
		res = res.replaceAll("零$", "");
		return res.isEmpty() ? "零" : res;
	}

	/** 函数求值 */
	private Object applyFunction(String name, List<Object> args) {
		List<Object> flat = new ArrayList<>();
		flatten(args, flat);
		List<Double> nums = new ArrayList<>();
		for (Object o : flat) {
			if (isError(o) || o == null || "".equals(o)) {
				continue;
			}
			nums.add(toNum(o));
		}
		switch (name) {
			case "SUM": {
				double s = 0;
				for (Double d : nums) {
					s += d;
				}
				return s;
			}
			case "AVERAGE":
				if (nums.isEmpty()) {
					return 0d;
				}
				{
					double s = 0;
					for (Double d : nums) {
						s += d;
					}
					return s / nums.size();
				}
			case "MIN":
				if (nums.isEmpty()) {
					return 0d;
				}
				{
					double m = Double.MAX_VALUE;
					for (Double d : nums) {
						m = Math.min(m, d);
					}
					return m;
				}
			case "MAX":
				if (nums.isEmpty()) {
					return 0d;
				}
				{
					double m = -Double.MAX_VALUE;
					for (Double d : nums) {
						m = Math.max(m, d);
					}
					return m;
				}
			case "COUNT":
				return (double) nums.size();
			case "ROUND": {
				double x = flat.isEmpty() ? 0 : toNum(flat.get(0));
				int d = flat.size() > 1 ? (int) toNum(flat.get(1)) : 0;
				double f = Math.pow(10, d);
				return Math.round(x * f) / f;
			}
			case "ABS":
				return Math.abs(flat.isEmpty() ? 0 : toNum(flat.get(0)));
			case "INT":
				return (double) (long) (flat.isEmpty() ? 0 : toNum(flat.get(0)));
			case "LEN":
				return (double) String.valueOf(flat.isEmpty() ? "" : flat.get(0)).length();
			case "IF":
				return (flat.isEmpty() ? 0 : toNum(flat.get(0))) != 0
						? (flat.size() > 1 ? flat.get(1) : "")
						: (flat.size() > 2 ? flat.get(2) : "");
			case "AND":
				for (Object o : flat) {
					if (toNum(o) == 0) {
						return 0d;
					}
				}
				return 1d;
			case "OR":
				for (Object o : flat) {
					if (toNum(o) != 0) {
						return 1d;
					}
				}
				return 0d;
			case "NOT":
				return toNum(flat.isEmpty() ? 0 : flat.get(0)) == 0 ? 1d : 0d;
			case "CONCAT":
			case "CONCATENATE": {
				StringBuilder sb = new StringBuilder();
				for (Object o : flat) {
					sb.append(o == null ? "" : String.valueOf(o));
				}
				return sb.toString();
			}
			case "MOD": {
				double b = flat.size() > 1 ? toNum(flat.get(1)) : 0;
				if (b == 0) {
					return ERR_DIV0;
				}
				return toNum(flat.isEmpty() ? 0 : flat.get(0)) % b;
			}
			case "SQRT": {
				double x = flat.isEmpty() ? 0 : toNum(flat.get(0));
				if (x < 0) {
					return ERR_NUM;
				}
				return Math.sqrt(x);
			}
			case "POWER":
				return Math.pow(flat.isEmpty() ? 0 : toNum(flat.get(0)), flat.size() > 1 ? toNum(flat.get(1)) : 0);
			case "CEILING":
				return Math.ceil(toNum(flat.isEmpty() ? 0 : flat.get(0)) * 1e8) / 1e8;
			case "FLOOR":
				return Math.floor(toNum(flat.isEmpty() ? 0 : toNum(flat.get(0)) * 1e8)) / 1e8;
			case "TRIM":
				return String.valueOf(flat.isEmpty() ? "" : flat.get(0)).trim();
			case "ISBLANK": {
				Object o = flat.isEmpty() ? null : flat.get(0);
				return (o == null || "".equals(o)) ? 1d : 0d;
			}
			case "RMB":
				return numberToChineseCapital(flat.isEmpty() ? 0 : flat.get(0));
			default:
				return ERR_NAME;
		}
	}

	private void flatten(List<Object> src, List<Object> out) {
		for (Object o : src) {
			if (o instanceof List) {
				@SuppressWarnings("unchecked")
				List<Object> sub = (List<Object>) o;
				flatten(sub, out);
			} else {
				out.add(o);
			}
		}
	}
}
