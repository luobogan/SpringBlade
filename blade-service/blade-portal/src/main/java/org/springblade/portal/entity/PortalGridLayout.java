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
 * 特色门户-门户网格布局实体（对标泛微 hp_grid_layout §16.2）
 * <p>按 layout_type 分「登录前门户(login) / 登录后门户(theme)」；
 * layout_use=启用、layout_default_entrance=默认入口（同类型内唯一）。
 * 泛微元素级子表 hp_grid_layout_element 本地化合并为 layout_config JSON 列。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_grid_layout")
@Schema(name = "特色门户布局", description = "门户网格布局（对标泛微 hp_grid_layout）")
public class PortalGridLayout extends Model<PortalGridLayout> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("layout_type")
	@Schema(name = "布局类型：login登录前门户/theme登录后门户")
	private String layoutType;

	@TableField("layout_name")
	@Schema(name = "布局名称")
	private String layoutName;

	@TableField("layout_title")
	@Schema(name = "布局标题（展示用）")
	private String layoutTitle;

	@TableField("layout_use")
	@Schema(name = "是否启用（0否 1是）")
	private Integer layoutUse;

	@TableField("layout_default_entrance")
	@Schema(name = "是否默认入口（同类型内唯一）")
	private Integer layoutDefaultEntrance;

	@TableField("layout_config")
	@Schema(name = "网格布局配置JSON（五区元素清单）")
	private String layoutConfig;

	@TableField("layout_background")
	@Schema(name = "背景图/背景色配置")
	private String layoutBackground;

	@TableField("layout_version")
	@Schema(name = "配置版本号")
	private Integer layoutVersion;

	@TableField("remark")
	private String remark;

	@TableField("is_deleted")
	private Integer isDeleted;

	@TableField("status")
	private Integer status;

	@TableField("create_user")
	private Long createUser;

	@TableField("create_dept")
	private Long createDept;

	@TableField("create_time")
	private LocalDateTime createTime;

	@TableField("update_user")
	private Long updateUser;

	@TableField("update_time")
	private LocalDateTime updateTime;
}
