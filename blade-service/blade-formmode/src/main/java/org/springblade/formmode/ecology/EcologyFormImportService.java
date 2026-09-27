package org.springblade.formmode.ecology;

import java.util.List;
import java.util.Map;

/**
 * 泛微 ecology 表单导入服务
 * <p>仅导入结构：表单定义 + 字段 + 下拉选项 + 明细表 + 物理表；不迁移业务数据。</p>
 */
public interface EcologyFormImportService {

	/**
	 * 列出 ecology 中可导入的自定义表单（ID &lt; 0）
	 *
	 * @return 每个元素含 ecologyFormId / formName / tableName / fieldCount / detailCount
	 */
	List<Map<String, Object>> listEcologyForms();

	/**
	 * 按 ecology 表单ID导入为 blade 表单（每个表单独立事务，互不影响）
	 *
	 * @return 每个元素含 ecologyFormId / success / message
	 */
	List<Map<String, Object>> importForms(List<Long> ecologyFormIds);
}
