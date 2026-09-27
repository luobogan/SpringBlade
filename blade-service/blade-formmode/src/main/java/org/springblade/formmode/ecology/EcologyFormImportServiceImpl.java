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
			boolean hasOptions = (htmlType == 3 || htmlType == 4 || htmlType == 6) && !opts.isEmpty();
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

	// blade fieldHtmlType 枚举（必须与前端 TableDesign.tsx 实际编码一致，而非 typings.d.ts）
	private static final int HTML_TEXT = 1;        // 文本字段
	private static final int HTML_MULTILINE = 2;   // 多行文本
	private static final int HTML_BROWSER = 3;     // 浏览按钮
	private static final int HTML_SELECT = 4;      // 选择框（下拉/单选/多选）
	private static final int HTML_CHECKBOX = 6;    // 复选框
	private static final int HTML_SPECIAL = 7;     // 特殊字段（描述性文字/自定义链接/日期/时间）

	/**
	 * ecology FIELDHTMLTYPE → 前端 fieldHtmlType（对齐 TableDesign.tsx 下拉项与 getFieldTypeLabel）
	 * ecology（本部署实测，非标准）：1单行文本 2多行文本 3浏览按钮 4复选框(check框) 5选择框(下拉/单选/复选由TYPE区分) 7特殊字段(描述文本) 8多选框(本部署未见) 6/9浏览按钮变体
	 */
	private int mapHtmlType(String ecoHtmlType, String ecoType) {
		if (ecoHtmlType == null) {
			return HTML_TEXT;
		}
		switch (ecoHtmlType.trim()) {
			case "1":
				return HTML_TEXT;        // 单行文本
			case "2":
				return HTML_MULTILINE;   // 多行文本
			case "3":
			case "6":
			case "9":
				return HTML_BROWSER;     // 浏览按钮（日期/时间也以浏览器类型 98/99 表达）
			case "4":
				return HTML_CHECKBOX;    // check框
			case "5":
			case "8":
				return HTML_SELECT;      // 选择框（下拉/单选/多选，由 type 区分；8=多选框，本部署未见）
			case "7":
				return HTML_SPECIAL;     // 特殊字段（本部署实测为流程说明/请假说明等描述性文字）
			default:
				return HTML_TEXT;
		}
	}

	/**
	 * ecology FIELDHTMLTYPE + TYPE → 前端 fieldType（对齐 TableDesign.tsx 各 htmlType 分支的选项）
	 */
	private int mapType(String ecoHtmlType, String ecoType, String dbTypeRaw) {
		String ht = ecoHtmlType == null ? "" : ecoHtmlType.trim();
		String t = ecoType == null ? "" : ecoType.trim();
		switch (ht) {
			case "1":
				return mapTextType(dbTypeRaw); // 文本：按数据库类型细分 单行/整数/浮点
			case "2":
				return 1; // 多行文本
			case "3":
			case "6":
			case "9":
				return mapBrowserType(t, dbTypeRaw); // 浏览按钮（按 dbType 区分日期/部门）
			case "4":
				return 1; // 复选框
			case "5":
				if ("2".equals(t)) return 2;   // 单选框
				if ("3".equals(t)) return 3;   // 复选框（多选）
				return 1;                       // 下拉框（TYPE=1 或未识别）
			case "7":
				if ("1".equals(t)) return 1;   // 自定义链接
				if ("3".equals(t)) return 3;   // 日期
				if ("4".equals(t)) return 4;   // 时间
				return 2;                       // 描述性文字（流程说明/请假说明等，默认）
			case "8":
				return 3; // 选择框-复选框
			default:
				return 1;
		}
	}

	/**
	 * 单行文本按数据库类型细分：整数(4)/浮点数(5)；其余单行文本(1)。
	 */
	private int mapTextType(String dbTypeRaw) {
		String r = dbTypeRaw == null ? "" : dbTypeRaw.toLowerCase();
		if (r.contains("int")) {
			return 4; // 整数
		}
		if (r.contains("decimal") || r.contains("float") || r.contains("numeric")) {
			return 5; // 浮点数
		}
		return 1; // 单行文本
	}

	/**
	 * 判断 ecology 浏览器字段的数据库类型是否像「日期/时间」。
	 * 本库日期浏览器用 FIELDHTMLTYPE=3 + TYPE=2/3 + FIELDDBTYPE=char(10)/char(8) 表达，
	 * 而「组织/部门」同样可能用 TYPE=2 —— 必须靠 dbType 区分，否则会把部门误判成日期。
	 */
	private boolean isDateLike(String dbTypeRaw) {
		if (dbTypeRaw == null) {
			return false;
		}
		String r = dbTypeRaw.toLowerCase().replaceAll("\\s+", "");
		if (r.contains("date") || r.contains("datetime") || r.contains("smalldatetime")) {
			return true;
		}
		// ecology 日期/时间存为定长字符：char(8)~char(11)
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("char\\((\\d+)\\)").matcher(r);
		if (m.find()) {
			int len = Integer.parseInt(m.group(1));
			return len >= 8 && len <= 11;
		}
		return false;
	}

	/**
	 * ecology 浏览按钮 TYPE → 前端浏览按钮 fieldType（对齐 TableDesign.tsx 浏览器类型选项）。
	 * 依据（已用 ecology2020_demo 实际数据核对，非标准 weaver 部署，字典标签不可信）：
	 *  - workflow_browsertype 字典(种子)：1人员 2组织 4文档 7项目 8资产 9人事 …（本部署实际语义与字典不符，以实测为准）
	 *  - 字段实测语义：-9 表单实测：4=部门(经办部门) 24=岗位(经办岗位)
	 *                 全局标签实测：9=文档(相关文档/正文/合同文件) 37=文档(自定义：合同文档/PDF正文)
	 *                 2/19 多为日期/时间（按 dbType 判定）；组织类：2=部门 17=多人力资源(本部署) 18=分部 19=分权单部门 20=分权多部门 21=分权单分部 22=分权多分部 23=多分部；164=分部(本部署，非自定义浏览按钮)
	 *  - 前端文档选项 fieldType=24（browserTypeMap[24]='文档'），故 9、37 均映射到 24。
	 * 日期/时间优先按 dbType 判定（TYPE=2/3 + char(10) 为日期，避免与部门冲突）。
	 * 其余未知类型兜底为人力资源。
	 * 注意：字典写 4=文档，但本部署 4 实际是部门，故 4 仍映射部门，文档走 9/37。
	 */
	private int mapBrowserType(String ecoType, String dbTypeRaw) {
		String t = ecoType == null ? "" : ecoType.trim();
		// 1) 日期/时间：dbType 像日期时按 TYPE 区分（与「部门」用同一 TYPE=2 的关键区分）
		//    TYPE=2 → 日期；TYPE=3/19 → 时间（本库 开始时间/结束时间 用 19）
		if (isDateLike(dbTypeRaw)) {
			if ("2".equals(t)) {
				return 98; // 日期
			}
			if ("3".equals(t) || "19".equals(t)) {
				return 99; // 时间
			}
		}
		// 2) 常规浏览器类型映射（ecology 编码 → 前端编码）
		switch (t) {
			case "1":
				return 1;    // 人员 → 人力资源
			case "2":
				return 2;    // 组织 → 部门（非日期场景）
			case "3":
				return 3;    // 角色
			case "4":
				return 2;    // 部门（本库 经办部门/报销部门 均用 4）
			case "7":
				return 8;    // 项目 → 项目（前端 8）
			case "8":
				return 7;    // 资产 → 资产（前端新增选项 7）
			case "9":
				return 24;   // 文档（本库 相关文档/正文/合同文件 均用 9）
			case "12":
				return 1;    // 集成 → 人力资源（无对应项，兜底）
			case "16":
				return 30;   // 相关客户/流程 → 流程（本库 借款流程/相关请求 用 16）
			case "17":
				return 161;  // 多人力资源（本部署 同行人 等用 TYPE=17 表示多人力资源，非多部门）
			case "18":
				return 18;   // 分部（独立类型，不再并入部门）
			case "19":
				return 19;   // 分权单部门（日期型 19 已在上方映射为时间）
			case "20":
				return 20;   // 分权多部门
			case "21":
				return 21;   // 分权单分部
			case "22":
				return 22;   // 分权多分部
			case "23":
				return 23;   // 多分部
			case "24":
				return 4;    // 岗位（本库 经办岗位 用 24）
			case "25":
				return 1;    // 多人员 → 人力资源
			case "30":
				return 30;   // 流程
			case "37":
				return 24;   // 文档（自定义：合同文档/PDF正文/岗位说明文档 等）
			case "31":
				return 30;   // 多流程 → 流程
			case "57":
				return 57;   // 附件
			case "98":
				return 98;   // 日期
			case "99":
				return 99;   // 时间
			case "161":
			case "256":
				return 256;  // 自定义树形单选
			case "162":
			case "257":
				return 257;  // 自定义树形多选
			case "164":
				return 18;   // 分部（本部署 经办分部 等用 TYPE=164 表示分部，非自定义浏览按钮）
			default:
				return 1;    // 兜底：人力资源
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
