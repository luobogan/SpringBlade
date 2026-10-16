package org.springblade.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 个人通用设置生效值 VO（个人值优先，NULL 回退全局默认）
 * 对标泛微 accountmore §14.2 I + GetMyThemeCmd 的「个人 > 组织 > 全局」优先级
 */
@Data
@Schema(name = "个人通用设置生效值", description = "字体/布局/更多菜单的最终生效值（个人优先，回退全局）")
public class AccountMoreVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "生效字体（1大 2中 3默认）")
	private Integer fontSize;

	@Schema(name = "生效布局类型（1舒适 2标准 3紧凑）")
	private Integer layoutType;

	@Schema(name = "个人\"更多\"菜单显隐JSON")
	private Map<String, Object> menuConfig;

	@Schema(name = "个人原始字体值（NULL=未设置，沿用全局；供设置表单回显）")
	private Integer personalFontSize;

	@Schema(name = "个人原始布局值（NULL=未设置，沿用全局；供设置表单回显）")
	private Integer personalLayoutType;

	@Schema(name = "全局默认字体（个人未设时的回退值）")
	private Integer globalFontSize;

	@Schema(name = "全局默认布局（个人未设时的回退值）")
	private Integer globalLayoutType;

	@Schema(name = "全局：快捷搜索总开关（0关 1开）")
	private Integer quickSearchEnabled;

	@Schema(name = "全局：是否隐藏\"我的门户\"入口")
	private Integer hiddenMyPortal;

	@Schema(name = "全局：是否显示常用菜单")
	private Integer showFreqMenu;

	@Schema(name = "全局：默认展开左侧菜单")
	private Integer defaultExpandLeft;
}
