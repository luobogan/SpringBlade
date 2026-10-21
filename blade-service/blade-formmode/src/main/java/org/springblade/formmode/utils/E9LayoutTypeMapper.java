package org.springblade.formmode.utils;

/**
 * 布局类型：本项目自有 {@code form_layout.layout_type} ↔ 泛微 E9 {@code layouttype} 双向映射
 *
 * <p>两套编码不一致，**严禁按数字直接等同**：</p>
 * <pre>
 *   本项目（form_layout.layout_type）：0=编辑(默认) 1=显示 3=监控 4=打印
 *   E9   （modehtmllayout.type）    ：0=显示     1=新建 2=编辑 3=监控 4=打印
 * </pre>
 * <p>即：同一个数字 0，在本项目是「编辑」，在 E9 是「显示」，语义相反。</p>
 *
 * <p>处置原则（对齐 S0 字段类型映射的「映射而非改存量」范式）：
 * <b>不修改既有数据与列语义</b>，一切互转走本类；是否做存量迁移由独立迁移脚本决定。
 * 已知差异：本项目暂无独立的「新建(1)」布局，E9 的新建态复用编辑布局（映射时标注 lossy）。</p>
 */
public final class E9LayoutTypeMapper {

	private E9LayoutTypeMapper() {
	}

	// ==================== 本项目自有布局类型 ====================

	public static final int SELF_EDIT = 0;    // 编辑（默认，亦用于新建态）
	public static final int SELF_VIEW = 1;    // 显示
	public static final int SELF_MONITOR = 3; // 监控
	public static final int SELF_PRINT = 4;   // 打印

	// ==================== 泛微 E9 布局类型 ====================

	public static final int E9_VIEW = 0;    // 显示
	public static final int E9_CREATE = 1;  // 新建
	public static final int E9_EDIT = 2;    // 编辑
	public static final int E9_MONITOR = 3; // 监控
	public static final int E9_PRINT = 4;   // 打印

	/**
	 * 自有布局类型 → E9 layouttype。
	 *
	 * @param self 自有 layout_type；为 null 时按默认「编辑」处理
	 */
	public static int toE9(Integer self) {
		int v = self == null ? SELF_EDIT : self;
		switch (v) {
			case SELF_VIEW:
				return E9_VIEW;
			case SELF_MONITOR:
				return E9_MONITOR;
			case SELF_PRINT:
				return E9_PRINT;
			case SELF_EDIT:
			default:
				return E9_EDIT;
		}
	}

	/**
	 * E9 layouttype → 自有布局类型。
	 *
	 * @param e9 E9 layouttype；为 null 时按「显示」处理
	 * @return 自有 layout_type
	 */
	public static int fromE9(Integer e9) {
		int v = e9 == null ? E9_VIEW : e9;
		switch (v) {
			case E9_EDIT:
			case E9_CREATE:
				// 本项目无独立「新建」布局，复用编辑布局
				return SELF_EDIT;
			case E9_MONITOR:
				return SELF_MONITOR;
			case E9_PRINT:
				return SELF_PRINT;
			case E9_VIEW:
			default:
				return SELF_VIEW;
		}
	}

	/**
	 * E9 layouttype → 自有布局类型时是否存在语义降级。
	 *
	 * <p>仅 E9「新建(1)」会降级：本项目复用编辑布局承载新建态。</p>
	 */
	public static boolean isLossyFromE9(Integer e9) {
		return e9 != null && e9 == E9_CREATE;
	}

	/** 自有布局类型中文名（用于日志与前端展示） */
	public static String selfName(Integer self) {
		int v = self == null ? SELF_EDIT : self;
		switch (v) {
			case SELF_VIEW:
				return "显示";
			case SELF_MONITOR:
				return "监控";
			case SELF_PRINT:
				return "打印";
			case SELF_EDIT:
			default:
				return "编辑";
		}
	}

	/** E9 layouttype 中文名 */
	public static String e9Name(Integer e9) {
		int v = e9 == null ? E9_VIEW : e9;
		switch (v) {
			case E9_CREATE:
				return "新建";
			case E9_EDIT:
				return "编辑";
			case E9_MONITOR:
				return "监控";
			case E9_PRINT:
				return "打印";
			case E9_VIEW:
			default:
				return "显示";
		}
	}
}
