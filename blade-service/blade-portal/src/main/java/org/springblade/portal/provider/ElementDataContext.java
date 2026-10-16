package org.springblade.portal.provider;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 元素取数上下文
 */
@Data
public class ElementDataContext implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 元素ID（单元素调试时可能为空）
	 */
	private Long elementId;

	/**
	 * 元素类型
	 */
	private String elementType;

	/**
	 * 元素私有配置（已从 config_json 解析；可能为空集合）
	 */
	private Map<String, Object> config = Collections.emptyMap();

	/**
	 * 取数条数上限（来自元素配置 perPage 或请求 limit）
	 */
	private int limit = 5;

	/**
	 * 当前用户ID
	 */
	private Long userId;

	/**
	 * 当前用户部门ID（BladeUser.deptId 为 String 类型）
	 */
	private String deptId;

	/**
	 * 租户ID
	 */
	private String tenantId;

	/**
	 * 数据组织范围：1=按租户(默认) 2=按部门 3=按分部（详见 PortalConstant.ORG_SCOPE_*）
	 */
	private int orgScope = 1;

	/**
	 * 推导出的分部ID（info 用；orgScope=3 时有效）
	 */
	private Long branchId;

	/**
	 * 按部门过滤时的部门ID（orgScope=2 时设置）
	 */
	private Long filterDeptId;

	/**
	 * 按分部过滤时的部门ID集合（orgScope=3 时设置，含分部自身及其下属部门）
	 */
	private List<Long> branchDeptIds;

	/**
	 * 读取配置项（支持 Integer/Long/String 宽松取值）
	 *
	 * @param key          配置键
	 * @param defaultValue 缺省值
	 * @return 配置值
	 */
	public int getIntConfig(String key, int defaultValue) {
		Object value = config == null ? null : config.get(key);
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value instanceof String text && !text.isBlank()) {
			try {
				return Integer.parseInt(text.trim());
			} catch (NumberFormatException ignored) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	/**
	 * 读取字符串配置
	 *
	 * @param key 配置键
	 * @return 配置值（无则返回 null）
	 */
	public String getStringConfig(String key) {
		Object value = config == null ? null : config.get(key);
		return value == null ? null : String.valueOf(value);
	}
}