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
 * 门户菜单实体
 * 对应表：blade_portal_menu
 * 对标泛微 E-cology 门户菜单（portalmenu §14.2）：门户自有导航菜单（顶部/工具栏/快捷入口），
 * 支持父子层级、链接模式（站内路由/外部链接）、可见开关与使用限制共享。
 * <p>与系统导航菜单（blade_menu，由 blade-system 维护）相互独立：本表是「门户」私有的菜单，
 * 不改动系统菜单主数据。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_menu")
@Schema(name = "门户菜单", description = "门户自有导航菜单（对标泛微 portalmenu）")
public class PortalMenu extends Model<PortalMenu> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	@Schema(name = "主键ID")
	private Long id;

	@TableField("tenant_id")
	@Schema(name = "租户ID")
	private String tenantId;

	@TableField("parent_id")
	@Schema(name = "父菜单ID（0为顶级）")
	private Long parentId;

	@TableField("menu_name")
	@Schema(name = "菜单名称")
	private String menuName;

	@TableField("icon")
	@Schema(name = "图标（antd 图标名或图片URL）")
	private String icon;

	@TableField("route_url")
	@Schema(name = "链接地址（站内路由或外部URL）")
	private String routeUrl;

	@TableField("menu_type")
	@Schema(name = "菜单类型：top顶部/toolbar工具栏/quick快捷入口")
	private String menuType;

	@TableField("link_mode")
	@Schema(name = "链接模式：1站内路由 2外部链接")
	private Integer linkMode;

	@TableField("sort_order")
	@Schema(name = "同级排序号（越小越靠前）")
	private Integer sortOrder;

	@TableField("visible")
	@Schema(name = "是否可见（0隐藏 1可见）")
	private Integer visible;

	@TableField("is_system")
	@Schema(name = "是否系统内置（1内置不可删）")
	private Integer isSystem;

	@TableField("config_json")
	@Schema(name = "扩展配置JSON（预留）")
	private String configJson;

	@TableField("is_deleted")
	@Schema(name = "是否删除（0未删除 1已删除）")
	private Integer isDeleted;

	@TableField("status")
	@Schema(name = "状态（1正常 0禁用 -1已删除）")
	private Integer status;

	@TableField("create_user")
	@Schema(name = "创建人")
	private Long createUser;

	@TableField("create_dept")
	@Schema(name = "创建部门")
	private Long createDept;

	@TableField("create_time")
	@Schema(name = "创建时间")
	private LocalDateTime createTime;

	@TableField("update_user")
	@Schema(name = "修改人")
	private Long updateUser;

	@TableField("update_time")
	@Schema(name = "修改时间")
	private LocalDateTime updateTime;
}
