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
 * 快捷搜索类型实体
 * 对应表：blade_portal_quick_search
 * 对标泛微 E-cology：quick_search_types（§14.2 H，quicksearchmanage 快捷搜索管理）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_quick_search")
@Schema(name = "快捷搜索类型", description = "门户快捷搜索类型（对标泛微 quick_search_types）")
public class PortalQuickSearch extends Model<PortalQuickSearch> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("module")
	@Schema(name = "所属模块（如 workflow/doc/notice）")
	private String module;

	@TableField("type")
	@Schema(name = "搜索类型标识（模块内唯一）")
	private String type;

	@TableField("custom_name")
	@Schema(name = "搜索类型显示名")
	private String customName;

	@TableField("icon")
	@Schema(name = "图标（antd 图标名或图片URL）")
	private String icon;

	@TableField("url")
	@Schema(name = "搜索跳转地址")
	private String url;

	@TableField("link_mode")
	@Schema(name = "链接模式（1站内路由 2外部链接）")
	private Integer linkMode;

	@TableField("is_use")
	@Schema(name = "是否启用（0停用 1启用）")
	private Integer isUse;

	@TableField("is_sys")
	@Schema(name = "是否系统内置（1内置不可删）")
	private Integer isSys;

	@TableField("order_num")
	@Schema(name = "排序号（越小越靠前）")
	private Integer orderNum;

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
