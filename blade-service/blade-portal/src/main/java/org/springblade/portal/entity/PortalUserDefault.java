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
 * 用户个人默认门户实体
 * 对应表：blade_portal_user_default
 * 对标泛微 E-cology：hpcurrentuse（用户上次访问/个人默认首页，§18.2 landing 优先级）
 * <p>landing 优先级：portalCode 精确 &gt; 个人默认 &gt; 机构默认 &gt; 全局默认 &gt; 命中授权页。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_user_default")
@Schema(name = "用户个人默认门户", description = "用户级默认首页（对标泛微 hpcurrentuse）")
public class PortalUserDefault extends Model<PortalUserDefault> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	@Schema(name = "主键ID")
	private Long id;

	@TableField("tenant_id")
	@Schema(name = "租户ID")
	private String tenantId;

	@TableField("user_id")
	@Schema(name = "用户ID")
	private Long userId;

	@TableField("page_id")
	@Schema(name = "门户页面ID（blade_portal_page.id）")
	private Long pageId;

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
