package org.springblade.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.spring.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门户应用设置实体（租户单例）
 * 对应表：blade_portal_setting
 * 对标泛微 E-cology：portalsetting（§16.3，SystemSet + PortalThemeConfig + PortalPageConfig 合并）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_setting")
@Schema(name = "门户应用设置", description = "门户全局应用设置（租户单例，对标泛微 portalsetting）")
public class PortalSetting extends Model<PortalSetting> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("need_refresh")
	@Schema(name = "首页自动刷新开关（0关 1开）")
	private Integer needRefresh;

	@TableField("refresh_scope")
	@Schema(name = "刷新范围（1整页 2指定元素）")
	private Integer refreshScope;

	@TableField("refresh_mins")
	@Schema(name = "自动刷新间隔（分钟）")
	private Integer refreshMins;

	@TableField("rsstype")
	@Schema(name = "RSS读取类型（1客户端 2服务器）")
	private Integer rsstype;

	@TableField("show_last_hp")
	@Schema(name = "记住上次访问首页（0否 1是）")
	private Integer showLastHp;

	@TableField("show_no_right_element")
	@Schema(name = "显示无查看权限元素（0否 1是）")
	private Integer showNoRightElement;

	@TableField("hidden_my_portal")
	@Schema(name = "隐藏\"我的门户\"入口（0否 1是）")
	private Integer hiddenMyPortal;

	@TableField("auto_sort_top_menu")
	@Schema(name = "菜单按使用频率自动排序（0否 1是）")
	private Integer autoSortTopMenu;

	@TableField("default_expand_left")
	@Schema(name = "默认展开左侧菜单（0否 1是）")
	private Integer defaultExpandLeft;

	@TableField("show_top_menu_full_name")
	@Schema(name = "顶部菜单显示全称（0否 1是）")
	private Integer showTopMenuFullName;

	@TableField("show_freq_menu")
	@Schema(name = "显示常用菜单（0否 1是）")
	private Integer showFreqMenu;

	@TableField("quick_search_enabled")
	@Schema(name = "快捷搜索总开关（0关 1开，对标 PortalThemeConfig.showquicksearch）")
	private Integer quickSearchEnabled;

	@TableField("layout_type")
	@Schema(name = "门户布局类型（1舒适 2标准 3紧凑）")
	private Integer layoutType;

	@TableField("font_size")
	@Schema(name = "全局默认字体（1大 2中 3默认）")
	private Integer fontSize;

	@TableField("remark")
	private String remark;

	@TableField("is_deleted")
	private Integer isDeleted;

	@TableField("status")
	private Integer status;

	@TableField("create_user")
	private Long createUser;

	@TableField("create_time")
	private LocalDateTime createTime;

	@TableField("update_user")
	private Long updateUser;

	@TableField("update_time")
	private LocalDateTime updateTime;
}
