package org.springblade.portal.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统导航菜单（只读映射，对标泛微 LeftMenu/MainMenu 同一套引擎）
 * <p>对应表：{@code blade_menu}（由 blade-system 维护）。门户的「前端菜单(左)/后端菜单(顶)」
 * 即对这套系统菜单的门户呈现（§14.1：门户菜单 = 系统 LeftMenu/MainMenu 的可视化维护界面）。
 * blade-portal 仅读取 + 按角色授权过滤，不改变系统菜单主数据的归属。</p>
 */
@Data
@TableName("blade_menu")
@Schema(name = "系统菜单", description = "系统导航菜单（blade_menu，只读）")
public class BladeMenu implements Serializable {

	@TableId(value = "id")
	@Schema(name = "主键ID")
	private Long id;

	@TableField("parent_id")
	@Schema(name = "父菜单ID（0为顶级）")
	private Long parentId;

	@TableField("code")
	@Schema(name = "菜单编码")
	private String code;

	@TableField("name")
	@Schema(name = "菜单名称")
	private String name;

	@TableField("alias")
	@Schema(name = "别名")
	private String alias;

	@TableField("path")
	@Schema(name = "路由地址")
	private String path;

	@TableField("sort")
	@Schema(name = "排序号")
	private Integer sort;

	@TableField("category")
	@Schema(name = "类别（1菜单 2按钮）")
	private Integer category;

	@TableField("is_open")
	@Schema(name = "打开方式")
	private Integer isOpen;

	@TableField("is_component")
	@Schema(name = "是否组件型页面")
	private Integer isComponent;
}
