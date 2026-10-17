package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 门户应用设置保存入参（租户单例 upsert）
 * 对标泛微 portalsetting §16.3
 */
@Data
@Schema(name = "门户应用设置入参", description = "门户全局应用设置（对标泛微 portalsetting）")
public class PortalSettingSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "首页自动刷新开关（0关 1开）")
	private Integer needRefresh;

	@Schema(name = "刷新范围（1整页 2指定元素）")
	private Integer refreshScope;

	@Schema(name = "自动刷新间隔（分钟）")
	private Integer refreshMins;

	@Schema(name = "RSS读取类型（1客户端 2服务器）")
	private Integer rsstype;

	@Schema(name = "记住上次访问首页（0否 1是）")
	private Integer showLastHp;

	@Schema(name = "显示无查看权限元素（0否 1是）")
	private Integer showNoRightElement;

	@Schema(name = "隐藏\"我的门户\"入口（0否 1是）")
	private Integer hiddenMyPortal;

	@Schema(name = "菜单按使用频率自动排序（0否 1是）")
	private Integer autoSortTopMenu;

	@Schema(name = "默认展开左侧菜单（0否 1是）")
	private Integer defaultExpandLeft;

	@Schema(name = "顶部菜单显示全称（0否 1是）")
	private Integer showTopMenuFullName;

	@Schema(name = "显示常用菜单（0否 1是）")
	private Integer showFreqMenu;

	@Schema(name = "快捷搜索总开关（0关 1开）")
	private Integer quickSearchEnabled;

	@Schema(name = "门户布局类型（1舒适 2标准 3紧凑）")
	private Integer layoutType;

	@Schema(name = "全局默认字体（1大 2中 3默认）")
	private Integer fontSize;

	@Schema(name = "导航菜单源（1系统菜单blade_menu 2门户私有菜单blade_portal_menu front/top）")
	private Integer menuSource;
}
