package org.springblade.formmode.ecology;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springblade.formmode.entity.FieldDefinition;
import org.springblade.formmode.entity.FieldOption;
import org.springblade.formmode.entity.WorkflowBill;
import org.springblade.formmode.service.IDynamicTableService;
import org.springblade.formmode.service.IFieldDefinitionService;
import org.springblade.formmode.service.IFieldOptionService;
import org.springblade.formmode.service.IWorkflowBillService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 泛微 ecology 表单导入实现
 * <p>读取 ecology 的 workflow_bill / workflow_billfield / workflow_billdetailtable /
 * workflow_selectitem / htmllabelinfo，转换为 blade 的 workflow_bill / workflow_billfield /
 * mode_form_field_option，并复用 DynamicTableService 创建物理表。</p>
 */
@Slf4j
@Service
public class EcologyFormImportServiceImpl implements EcologyFormImportService {

	private static final Set<String> SYSTEM_COLUMNS = new HashSet<>(Arrays.asList(
		"id", "request_id", "modedatacreater", "modedatacreatedate",
		"modedatacreatetime", "modedatamodifier", "modedatamodifydate",
		"modedatamodifytime", "mode_uuid", "main_id", "is_deleted"));

	private final JdbcTemplate ecologyJdbcTemplate;
	private final IWorkflowBillService workflowBillService;
	private final IFieldDefinitionService fieldDefinitionService;
	private final IFieldOptionService fieldOptionService;
	private final IDynamicTableService dynamicTableService;

	public EcologyFormImportServiceImpl(
		@Qualifier("ecologyJdbcTemplate") JdbcTemplate ecologyJdbcTemplate,
		IWorkflowBillService workflowBillService,
		IFieldDefinitionService fieldDefinitionService,
		IFieldOptionService fieldOptionService,
		IDynamicTableService dynamicTableService) {
		this.ecologyJdbcTemplate = ecologyJdbcTemplate;
		this.workflowBillService = workflowBillService;
		this.fieldDefinitionService = fieldDefinitionService;
		this.fieldOptionService = fieldOptionService;
		this.dynamicTableService = dynamicTableService;
	}

	@Override
	public List<Map<String, Object>> listEcologyForms() {
		List<Map<String, Object>> bills = ecologyJdbcTemplate.queryForList(
			"SELECT ID, NAMELABEL, TABLENAME, DETAILTABLENAME, FORMDES, DSPORDER " +
				"FROM workflow_bill WHERE ID < 0 ORDER BY ID");
		List<Map<String, Object>> result = new ArrayList<>();
		for (Map<String, Object> b : bills) {
			Long id = getLong(b, "ID");
			String name = resolveLabel(getLong(b, "NAMELABEL"));
			if (name == null) {
				name = getString(b, "TABLENAME");
			}
			Map<String, Object> m = new HashMap<>();
			m.put("ecologyFormId", id);
			m.put("formName", name);
			m.put("tableName", getString(b, "TABLENAME"));
			m.put("fieldCount", countField(id));
			m.put("detailCount", countDetail(id));
			result.add(m);
		}
		return result;
	}

	private int countField(Long formId) {
		Integer n = ecologyJdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM workflow_billfield WHERE billid = ?", Integer.class, formId);
		return n == null ? 0 : n;
	}

	private int countDetail(Long formId) {
		Integer n = ecologyJdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM workflow_billdetailtable WHERE BILLID = ?", Integer.class, formId);
		return n == null ? 0 : n;
	}

	@Override
	public List<Map<String, Object>> importForms(List<Long> ecologyFormIds) {
		List<Map<String, Object>> results = new ArrayList<>();
		if (ecologyFormIds == null) {
			return results;
		}
		for (Long ecoFormId : ecologyFormIds) {
			Map<String, Object> r = new HashMap<>();
			r.put("ecologyFormId", ecoFormId);
			try {
				importOne(ecoFormId);
				r.put("success", Boolean.TRUE);
				r.put("message", "导入成功");
			} catch (Exception e) {
				log.error("导入 ecology 表单 {} 失败", ecoFormId, e);
				r.put("success", Boolean.FALSE);
				r.put("message", e.getMessage());
			}
			results.add(r);
		}
		return results;
	}

	/**
	 * 导入单个 ecology 表单（独立事务，失败仅回滚本表单）
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	protected void importOne(Long ecoFormId) {
		Map<String, Object> bill = ecologyJdbcTemplate.queryForMap(
			"SELECT ID, NAMELABEL, TABLENAME, DETAILTABLENAME, FORMDES, DSPORDER " +
				"FROM workflow_bill WHERE ID = ?", ecoFormId);

		String formName = resolveLabel(getLong(bill, "NAMELABEL"));
		if (formName == null) {
			formName = getString(bill, "TABLENAME");
		}
		String description = getString(bill, "FORMDES");

		List<Map<String, Object>> details = ecologyJdbcTemplate.queryForList(
			"SELECT TABLENAME, TITLE, ORDERID FROM workflow_billdetailtable " +
				"WHERE BILLID = ? ORDER BY ORDERID", ecoFormId);
		Map<String, Integer> detailIndex = new HashMap<>();
		for (int i = 0; i < details.size(); i++) {
			detailIndex.put(getString(details.get(i), "TABLENAME"), i + 1);
		}

		String tableName = nextTableName();

		// 1) workflow_bill
		WorkflowBill wb = new WorkflowBill();
		wb.setFormName(formName);
		wb.setTableName(tableName);
		wb.setFormType(0);
		wb.setDescription(description);
		wb.setStatus(1);
		wb.setTenantId("000000");
		wb.setIsDeleted(0);
		wb.setDetailTableCount(details.size());
		wb.setCreateUser(1);
		wb.setCreateTime(new Date());
		wb.setUpdateUser(1);
		wb.setUpdateTime(new Date());
		workflowBillService.save(wb);
		Long newBillId = wb.getId();

		// 2) workflow_billfield + mode_form_field_option
		// DEFAULTVALUE 列在不同 ecology 版本间不统一（部分版本 workflow_billfield 无此列），
		// 用 INFORMATION_SCHEMA 探测存在性，缺失时用 NULL 占位，避免 207 列名无效报错。
		Integer dvCount = ecologyJdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
				"WHERE TABLE_NAME = 'workflow_billfield' AND COLUMN_NAME = 'DEFAULTVALUE'", Integer.class);
		String defaultValExpr = (dvCount != null && dvCount > 0)
			? "DEFAULTVALUE"
			: "CAST(NULL AS VARCHAR(MAX)) AS DEFAULTVALUE";
		List<Map<String, Object>> fields = ecologyJdbcTemplate.queryForList(
			"SELECT id, FIELDNAME, FIELDLABEL, FIELDDBTYPE, FIELDHTMLTYPE, TYPE, " +
				"DETAILTABLE, SELECTITEM, TEXTHEIGHT, DSPORDER, " + defaultValExpr + " " +
				"FROM workflow_billfield WHERE billid = ? ORDER BY DETAILTABLE, DSPORDER", ecoFormId);

		for (Map<String, Object> f : fields) {
			String fieldName = getString(f, "FIELDNAME");
			if (fieldName == null || !fieldName.matches("^[A-Za-z0-9_]+$")) {
				log.warn("跳过非法列名: {}", fieldName);
				continue;
			}
			if (SYSTEM_COLUMNS.contains(fieldName.toLowerCase())) {
				log.warn("跳过系统保留列名: {}", fieldName);
				continue;
			}
			String dbTypeRaw = getString(f, "FIELDDBTYPE");
			String dbType = normalizeDbType(dbTypeRaw);
			int fieldLen = parseLen(dbTypeRaw);
			String ecoHtmlType = getString(f, "FIELDHTMLTYPE");
			String ecoType = getString(f, "TYPE");
			int htmlType = mapHtmlType(ecoHtmlType, ecoType);
			int type = mapType(ecoHtmlType, ecoType, dbTypeRaw);
			String label = resolveLabel(getLong(f, "FIELDLABEL"));
			Integer ecoDsp = getInt(f, "DSPORDER");
			int dsp = ecoDsp == null ? 0 : ecoDsp;
			Integer detailIdx = detailIndex.get(getString(f, "DETAILTABLE"));
			int isMain = detailIdx == null ? 1 : 0;

			FieldDefinition fd = new FieldDefinition();
			fd.setBillId(String.valueOf(newBillId));
			fd.setFieldName(fieldName);
			fd.setFieldLabel(label);
			fd.setFieldDbName(fieldName);
			fd.setFieldHtmlType(htmlType);
			fd.setFieldType(String.valueOf(type));
			fd.setFieldDbType(dbType);
			fd.setFieldLen(fieldLen);
			fd.setDecimalDigit(parseDecimal(dbTypeRaw));
			fd.setDefaultValue(getString(f, "DEFAULTVALUE"));
			fd.setDsOrder(dsp);
			fd.setIsNull(0);
			fd.setIsMain(isMain);
			fd.setDetailTable(detailIdx);
			fd.setTextHeight(getInt(f, "TEXTHEIGHT"));
			fd.setIsMand(0);
			fd.setFieldOrder(dsp);
			fd.setIsUsed(1);
			fd.setNeedExcel(0);
			fd.setNeedLog(0);
			fd.setImpCheck(0);
			fd.setStatus(1);
			fd.setIsDeleted(0);
			fd.setTenantId(0L);
			fd.setDescription(label);
			fd.setRemark(label);

			// 下拉/选择/复选：读取泛微选项（workflow_selectitem.FIELDID = 本字段 id）
			List<Map<String, Object>> opts = ecologyJdbcTemplate.queryForList(
				"SELECT SELECTVALUE, SELECTNAME, LISTORDER, ISDEFAULT FROM workflow_selectitem " +
					"WHERE FIELDID = ? AND (CANCEL IS NULL OR CANCEL <> '1') ORDER BY LISTORDER",
				getLong(f, "id"));
			boolean hasOptions = (htmlType == 3 || htmlType == 6 || htmlType == 8) && !opts.isEmpty();
			if (hasOptions) {
				StringBuilder sb = new StringBuilder();
				for (Map<String, Object> o : opts) {
					if (sb.length() > 0) {
						sb.append(",");
					}
					sb.append(String.valueOf(o.get("SELECTVALUE")));
				}
				fd.setSelectItem(sb.toString());
			}

			fieldDefinitionService.save(fd);
			Long newFieldId = fd.getId();

			if (hasOptions) {
				List<FieldOption> fos = new ArrayList<>();
				for (Map<String, Object> o : opts) {
					FieldOption fo = new FieldOption();
					fo.setFieldId(newFieldId);
					fo.setFormId(String.valueOf(newBillId));
					fo.setOptionValue(String.valueOf(o.get("SELECTVALUE")));
					fo.setOptionLabel(getString(o, "SELECTNAME"));
					Integer lo = getInt(o, "LISTORDER");
					fo.setListOrder(lo == null ? 0 : lo);
					String def = getString(o, "ISDEFAULT");
					fo.setIsDefault("y".equalsIgnoreCase(def) ? 1 : 0);
					fo.setIsDeleted(0);
					fo.setStatus(1);
					fos.add(fo);
				}
				fieldOptionService.saveBatch(fos);
			}
		}

		// 3) 物理表：主表 + 明细表 + 字段列（复用 DynamicTableService，与页面设计器一致）
		dynamicTableService.createMainTable(tableName);
		for (int i = 1; i <= details.size(); i++) {
			dynamicTableService.createDetailTable(tableName + "_dt" + i);
		}
		List<FieldDefinition> allFields = fieldDefinitionService.list(
			new QueryWrapper<FieldDefinition>().eq("billid", String.valueOf(newBillId)));
		for (FieldDefinition fd : allFields) {
			String colName = fd.getFieldDbName();
			if (colName == null || colName.isEmpty()) {
				colName = fd.getFieldName();
			}
			if (SYSTEM_COLUMNS.contains(colName.toLowerCase())) {
				continue;
			}
			String targetTable;
			if (fd.getIsMain() != null && fd.getIsMain() == 0) {
				int idx = fd.getDetailTable() == null ? 1 : fd.getDetailTable();
				targetTable = tableName + "_dt" + idx;
			} else {
				targetTable = tableName;
			}
			String sqlType = mapDbTypeToSql(fd.getFieldDbType(), fd.getFieldLen());
			dynamicTableService.addColumnToTable(targetTable, colName, sqlType, null);
		}

		log.info("ecology 表单 {} 导入完成：blade 表单ID={}, 表名={}", ecoFormId, newBillId, tableName);
	}

	/**
	 * 计算下一个主表名 formtable_main_{N}（沿用 blade 约定）
	 */
	private String nextTableName() {
		QueryWrapper<WorkflowBill> queryWrapper = new QueryWrapper<>();
		queryWrapper.likeRight("table_name", "formtable_main_");
		List<WorkflowBill> forms = workflowBillService.list(queryWrapper);
		int maxN = 0;
		for (WorkflowBill form : forms) {
			String t = form.getTableName();
			if (t != null && t.matches("formtable_main_\\d+")) {
				try {
					int n = Integer.parseInt(t.substring("formtable_main_".length()));
					if (n > maxN) {
						maxN = n;
					}
				} catch (NumberFormatException ignore) {
					// ignore
				}
			}
		}
		return "formtable_main_" + (maxN + 1);
	}

	// blade fieldHtmlType 枚举（与前端 typings.d.ts FieldHtmlType 对齐）
	private static final int HTML_TEXT = 1;        // 文本字段
	private static final int HTML_BROWSER = 2;     // 浏览按钮
	private static final int HTML_SELECT = 3;      // 选择框
	private static final int HTML_SPECIAL = 5;     // 特殊字段（日期/时间/说明）
	private static final int HTML_CHECKBOX = 6;    // 复选框
	private static final int HTML_DROPDOWN = 8;    // 下拉选择框（新）

	/**
	 * ecology FIELDHTMLTYPE + TYPE → blade fieldHtmlType
	 * <p>ecology FIELDHTMLTYPE：1单行文本 2多行文本 3浏览按钮 4check框 5下拉选择框
	 * 6/9浏览按钮变体 7单选框 8多选框；其中浏览按钮 type=2 在 ecology 为「日期」浏览器，
	 * blade 无浏览器日期，归入 SPECIAL 日期。</p>
	 */
	private int mapHtmlType(String ecoHtmlType, String ecoType) {
		if (ecoHtmlType == null) {
			return HTML_TEXT;
		}
		switch (ecoHtmlType.trim()) {
			case "1":
			case "2":
				return HTML_TEXT;        // 单行/多行文本
			case "3":
			case "6":
			case "9":
				if ("2".equals(ecoType != null ? ecoType.trim() : "")) {
					return HTML_SPECIAL; // 日期浏览器 → 特殊字段-日期
				}
				return HTML_BROWSER;     // 浏览按钮
			case "4":
				return HTML_CHECKBOX;    // check框
			case "5":
				return HTML_DROPDOWN;    // 下拉选择框
			case "7":
			case "8":
				return HTML_SELECT;      // 单选/多选框
			default:
				return HTML_TEXT;
		}
	}

	/**
	 * ecology FIELDHTMLTYPE + TYPE → blade fieldType
	 * <p>blade fieldType 按 fieldHtmlType 分组（见前端 typings.d.ts FieldType）。</p>
	 */
	private int mapType(String ecoHtmlType, String ecoType, String dbTypeRaw) {
		String ht = ecoHtmlType == null ? "" : ecoHtmlType.trim();
		String t = ecoType == null ? "" : ecoType.trim();
		switch (ht) {
			case "1":
				return 1; // 文本-单行文本
			case "2":
				return 2; // 文本-多行文本
			case "3":
			case "6":
			case "9":
				if ("2".equals(t)) {
					return 1; // 特殊字段-日期
				}
				return mapBrowserType(t);
			case "4":
				return 1; // 复选框
			case "5":
				return 1; // 下拉选择框
			case "7":
				return 1; // 选择框-单选框
			case "8":
				return 2; // 选择框-多选框
			default:
				return 1;
		}
	}

	/**
	 * ecology 浏览按钮 TYPE → blade 浏览按钮 fieldType。
	 * 参考示例（已用 ecology2020_demo 报销单核实）：
	 * 1人力资源 / 2日期(走 SPECIAL) / 4部门 / 16流程 / 161/256自定义浏览框(自定义树形)；
	 * 其余未知类型兜底为人力资源。
	 */
	private int mapBrowserType(String ecoType) {
		switch (ecoType == null ? "" : ecoType.trim()) {
			case "1":
				return 1;  // 人力资源
			case "4":
				return 2;  // 部门
			case "22":
				return 3;  // 角色
			case "24":
				return 4;  // 资产
			case "7":
			case "8":
				return 7;  // 文档
			case "16":
				return 8;  // 流程
			case "161":
			case "256":
				return 9;  // 自定义浏览框（自定义树形单选）
			default:
				return 1;  // 兜底：人力资源
		}
	}

	/**
	 * 将 ecology FIELDDBTYPE 规整为 blade fieldDbType 名称（varchar/int/decimal/date/datetime/text）
	 */
	private String normalizeDbType(String raw) {
		if (raw == null) {
			return "varchar";
		}
		String t = raw.toLowerCase();
		if (t.contains("int") || t.contains("bigint")) {
			return "int";
		}
		if (t.contains("decimal") || t.contains("float") || t.contains("double") || t.contains("numeric")) {
			return "decimal";
		}
		if (t.contains("date")) {
			return "date";
		}
		if (t.contains("time")) {
			return "datetime";
		}
		if (t.contains("text") || t.contains("blob")) {
			return "text";
		}
		return "varchar";
	}

	private int parseLen(String raw) {
		if (raw == null) {
			return 255;
		}
		Matcher m = Pattern.compile("varchar\\((\\d+)\\)", Pattern.CASE_INSENSITIVE).matcher(raw);
		if (m.find()) {
			return Integer.parseInt(m.group(1));
		}
		return 255;
	}

	private int parseDecimal(String raw) {
		if (raw == null) {
			return 0;
		}
		Matcher m = Pattern.compile("decimal\\(\\d+,\\s*(\\d+)\\)", Pattern.CASE_INSENSITIVE).matcher(raw);
		if (m.find()) {
			return Integer.parseInt(m.group(1));
		}
		return 0;
	}

	/**
	 * 解析 ecology 标签（优先 htmllabelinfo LANGUAGEID=7，回退 htmllabelindex）
	 */
	private String resolveLabel(Long indexId) {
		if (indexId == null) {
			return null;
		}
		try {
			List<Map<String, Object>> rows = ecologyJdbcTemplate.queryForList(
				"SELECT TOP 1 LABELNAME FROM htmllabelinfo WHERE INDEXID = ? " +
					"ORDER BY (CASE WHEN LANGUAGEID = 7 THEN 0 ELSE 1 END)", indexId);
			if (!rows.isEmpty()) {
				Object v = rows.get(0).get("LABELNAME");
				if (v != null) {
					return v.toString();
				}
			}
			List<Map<String, Object>> r2 = ecologyJdbcTemplate.queryForList(
				"SELECT indexdesc FROM htmllabelindex WHERE id = ?", indexId);
			if (!r2.isEmpty()) {
				Object v = r2.get(0).get("indexdesc");
				if (v != null) {
					return v.toString();
				}
			}
		} catch (Exception e) {
			log.warn("解析标签失败 indexId={}: {}", indexId, e.getMessage());
		}
		return null;
	}

	/**
	 * 字段数据库类型 → SQL 类型（与 WorkflowBillController.mapDbTypeToSql 保持一致）
	 */
	private String mapDbTypeToSql(String dbType, Integer length) {
		if (dbType == null) {
			return "VARCHAR(255)";
		}
		String type = dbType.trim().toLowerCase();
		Map<String, String> typeMap = new HashMap<>();
		typeMap.put("varchar", "VARCHAR");
		typeMap.put("int", "INT");
		typeMap.put("decimal", "DECIMAL");
		typeMap.put("date", "DATE");
		typeMap.put("datetime", "DATETIME");
		typeMap.put("text", "TEXT");
		typeMap.put("longtext", "LONGTEXT");

		String sqlType = typeMap.getOrDefault(type, "VARCHAR");
		if (sqlType.equals("VARCHAR")) {
			if (length != null && length > 0) {
				return sqlType + "(" + Math.min(length, 2000) + ")";
			}
			return sqlType + "(255)";
		}
		if (sqlType.equals("DECIMAL")) {
			return "DECIMAL(18,2)";
		}
		return sqlType;
	}

	private String getString(Map<String, Object> m, String key) {
		Object v = m.get(key);
		if (v == null) {
			v = m.get(key.toLowerCase());
		}
		return v == null ? null : v.toString();
	}

	private Long getLong(Map<String, Object> m, String key) {
		Object v = m.get(key);
		if (v == null) {
			v = m.get(key.toLowerCase());
		}
		if (v == null) {
			return null;
		}
		if (v instanceof Long) {
			return (Long) v;
		}
		if (v instanceof Number) {
			return ((Number) v).longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Integer getInt(Map<String, Object> m, String key) {
		Long l = getLong(m, key);
		return l == null ? null : l.intValue();
	}
}
