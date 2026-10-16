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
 * 快捷搜索使用限制实体
 * 对应表：blade_portal_quick_search_share
 * 对标泛微 E-cology：commonshareinfo（module='qs'，§14.2 H/§14.3）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_quick_search_share")
@Schema(name = "快捷搜索使用限制", description = "按全员/部门/个人/角色/分部限制快捷搜索可见（对标 commonshareinfo）")
public class PortalQuickSearchShare extends Model<PortalQuickSearchShare> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("search_id")
	@Schema(name = "快捷搜索类型ID")
	private Long searchId;

	@TableField("share_type")
	@Schema(name = "共享范围（1全员 2部门 3个人 4角色 5分部）")
	private Integer shareType;

	@TableField("share_id")
	@Schema(name = "共享对象ID（全员为0）")
	private Long shareId;

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
