package org.springblade.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 特色门户布局授权实体（使用限制：1全员/2部门/3个人，对标 commonshareinfo）
 */
@Data
@TableName("blade_portal_grid_layout_share")
@Schema(name = "特色门户布局授权", description = "门户网格布局使用限制")
public class PortalGridLayoutShare implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("layout_id")
	@Schema(name = "布局ID")
	private Long layoutId;

	@TableField("share_type")
	@Schema(name = "共享范围（1全员 2部门 3个人）")
	private Integer shareType;

	@TableField("share_id")
	@Schema(name = "共享对象ID（部门ID或用户ID；全员为0）")
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
