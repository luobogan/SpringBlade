package org.springblade.formmode.constant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字段类型元数据（单一来源，对齐前端 src/components/FormMode/fieldTypes.ts）
 *
 * <p>编码体系：1文本字段 2多行文本 3浏览按钮 4选择框 5附件上传 6复选框 7特殊字段 8布局组件。
 * 供 /field-definition/field-types、/browser-types 返回结构化清单，避免前端各页面各解析一套。</p>
 */
public final class FieldTypeMeta {

	private FieldTypeMeta() {
	}

	/** 浏览按钮类型（fieldHtmlType=3 时的 fieldType）-> 中文名，LinkedHashMap 保序即下拉顺序 */
	private static final Map<Integer, String> BROWSER_TYPE_NAMES = new LinkedHashMap<>();

	static {
		BROWSER_TYPE_NAMES.put(1, "人力资源");
		BROWSER_TYPE_NAMES.put(161, "多人力资源");
		BROWSER_TYPE_NAMES.put(162, "应聘人");
		BROWSER_TYPE_NAMES.put(163, "多角色");
		BROWSER_TYPE_NAMES.put(165, "人力资源条件");
		BROWSER_TYPE_NAMES.put(166, "角色人员");
		BROWSER_TYPE_NAMES.put(167, "分权单人力资源");
		BROWSER_TYPE_NAMES.put(168, "分权多人力资源");
		BROWSER_TYPE_NAMES.put(2, "部门");
		BROWSER_TYPE_NAMES.put(17, "多部门");
		BROWSER_TYPE_NAMES.put(18, "分部");
		BROWSER_TYPE_NAMES.put(19, "分权单部门");
		BROWSER_TYPE_NAMES.put(20, "分权多部门");
		BROWSER_TYPE_NAMES.put(21, "分权单分部");
		BROWSER_TYPE_NAMES.put(22, "分权多分部");
		BROWSER_TYPE_NAMES.put(23, "多分部");
		BROWSER_TYPE_NAMES.put(25, "办公地点");
		BROWSER_TYPE_NAMES.put(3, "角色");
		BROWSER_TYPE_NAMES.put(4, "岗位");
		BROWSER_TYPE_NAMES.put(8, "项目");
		BROWSER_TYPE_NAMES.put(16, "相关客户");
		BROWSER_TYPE_NAMES.put(24, "文档");
		BROWSER_TYPE_NAMES.put(26, "多文档");
		BROWSER_TYPE_NAMES.put(30, "流程");
		BROWSER_TYPE_NAMES.put(31, "多流程");
		BROWSER_TYPE_NAMES.put(32, "归档流程");
		BROWSER_TYPE_NAMES.put(57, "附件");
		BROWSER_TYPE_NAMES.put(7, "资产");
		BROWSER_TYPE_NAMES.put(98, "日期");
		BROWSER_TYPE_NAMES.put(99, "时间");
		BROWSER_TYPE_NAMES.put(100, "省份");
		BROWSER_TYPE_NAMES.put(101, "币种");
		BROWSER_TYPE_NAMES.put(102, "城市");
		BROWSER_TYPE_NAMES.put(103, "区县");
		BROWSER_TYPE_NAMES.put(104, "年份");
		BROWSER_TYPE_NAMES.put(105, "语言");
		BROWSER_TYPE_NAMES.put(164, "自定义浏览按钮");
		BROWSER_TYPE_NAMES.put(256, "自定义树形单选");
		BROWSER_TYPE_NAMES.put(257, "自定义树形多选");
		// 公文扩展（与前端 fieldTypes.ts 的 BROWSER_TYPE_LABEL_MAP 保持同一登记表，避免漂移）
		BROWSER_TYPE_NAMES.put(258, "收发文单位");
		BROWSER_TYPE_NAMES.put(259, "多收发文单位");
		BROWSER_TYPE_NAMES.put(260, "秘密等级");
		BROWSER_TYPE_NAMES.put(261, "紧急程度");
		BROWSER_TYPE_NAMES.put(262, "公文种类");
		BROWSER_TYPE_NAMES.put(263, "主题分类");
		BROWSER_TYPE_NAMES.put(264, "发文字号");
	}

	/** 非浏览按钮 htmlType -> (type -> 名称) */
	private static final Map<Integer, Map<Integer, String>> FIELD_TYPE_NAMES = new LinkedHashMap<>();

	static {
		FIELD_TYPE_NAMES.put(1, mapOf(1, "单行文本", 2, "多行文本", 3, "保密字段", 4, "整数", 5, "浮点数", 6, "金额转换", 7, "金额千分位"));
		FIELD_TYPE_NAMES.put(2, mapOf(1, "多行文本"));
		FIELD_TYPE_NAMES.put(4, mapOf(1, "下拉框", 2, "单选框", 3, "复选框"));
		FIELD_TYPE_NAMES.put(5, mapOf(1, "附件上传", 2, "图片上传"));
		FIELD_TYPE_NAMES.put(6, mapOf(1, "复选框"));
		FIELD_TYPE_NAMES.put(7, mapOf(1, "自定义链接", 2, "描述性文字", 3, "日期", 4, "时间"));
		FIELD_TYPE_NAMES.put(8, mapOf(14, "布局组件"));
	}

	private static Map<Integer, String> mapOf(Object... kv) {
		Map<Integer, String> m = new LinkedHashMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			m.put((Integer) kv[i], (String) kv[i + 1]);
		}
		return m;
	}

	/** 根据字段HTML类型与详细类型推导数据库类型（对齐前端 getDbTypeByFieldType） */
	private static String dbType(int htmlType, int type) {
		switch (htmlType) {
			case 1:
				if (type == 2 || type == 3) {
					return "text";
				}
				if (type == 5) {
					return "decimal";
				}
				return "varchar";
			case 2:
				return "text";
			case 3:
			case 4:
			case 5:
				return "varchar";
			case 6:
				return "int";
			case 7:
				if (type == 3) {
					return "date";
				}
				if (type == 4) {
					return "datetime";
				}
				return "varchar";
			default:
				return "varchar";
		}
	}

	/**
	 * 字段类型清单（htmlType + type + name + dbType），htmlType=3 展开全部浏览按钮子类型。
	 */
	public static List<Map<String, Object>> fieldTypes() {
		List<Map<String, Object>> list = new ArrayList<>();
		FIELD_TYPE_NAMES.forEach((htmlType, types) ->
			types.forEach((type, name) -> list.add(item(htmlType, type, name))));
		BROWSER_TYPE_NAMES.forEach((type, name) -> list.add(item(3, type, name)));
		return list;
	}

	/**
	 * 浏览框（浏览按钮）类型清单（type + name）。
	 */
	public static List<Map<String, Object>> browserTypes() {
		List<Map<String, Object>> list = new ArrayList<>();
		BROWSER_TYPE_NAMES.forEach((type, name) -> {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("type", type);
			m.put("name", name);
			list.add(m);
		});
		return list;
	}

	private static Map<String, Object> item(int htmlType, int type, String name) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("htmlType", htmlType);
		m.put("type", type);
		m.put("name", name);
		m.put("dbType", dbType(htmlType, type));
		return m;
	}
}
