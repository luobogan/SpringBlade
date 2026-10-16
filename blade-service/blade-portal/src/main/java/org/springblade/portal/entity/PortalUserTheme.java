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
 * 用户主题偏好实体
 * 对应表：blade_portal_user_theme
 * 对标泛微 E-cology：themecenter（主题个性化中心，§13.5）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_user_theme")
@Schema(name = "用户主题偏好", description = "用户级主题覆盖（对标泛微 themecenter）")
public class PortalUserTheme extends Model<PortalUserTheme> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	@Schema(name = "主键ID")
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("user_id")
	@Schema(name = "用户ID")
	private Long userId;

	@TableField("theme_id")
	@Schema(name = "主题ID（blade_portal_theme.id）")
	private Long themeId;

	@TableField("create_time")
	private LocalDateTime createTime;

	@TableField("update_time")
	private LocalDateTime updateTime;
}
