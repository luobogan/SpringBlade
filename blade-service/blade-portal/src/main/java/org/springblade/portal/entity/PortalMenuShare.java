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
 * 门户菜单授权实体
 * 对应表：blade_portal_menu_share
 * 对标泛微 commonshareinfo / elementshareinfo（§14.3）：菜单使用限制（1全员/2部门/3个人）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_menu_share")
@Schema(name = "门户菜单授权", description = "门户菜单可见范围（全员/部门/个人）")
public class PortalMenuShare extends Model<PortalMenuShare> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	@Schema(name = "主键ID")
	private Long id;

	@TableField("tenant_id")
	@Schema(name = "租户ID")
	private String tenantId;

	@TableField("menu_id")
	@Schema(name = "菜单ID（blade_portal_menu.id）")
	private Long menuId;

	@TableField("share_type")
	@Schema(name = "共享范围（1全员 2部门 3个人）")
	private Integer shareType;

	@TableField("share_id")
	@Schema(name = "共享对象ID（部门ID或用户ID；全员为0）")
	private Long shareId;

	@TableField("is_deleted")
	@Schema(name = "是否删除（0未删除 1已删除）")
	private Integer isDeleted;

	@TableField("status")
	@Schema(name = "状态（1正常 0禁用 -1已删除）")
	private Integer status;

	@TableField("create_user")
	@Schema(name = "创建人")
	private Long createUser;

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
