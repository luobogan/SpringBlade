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
 * 门户主题实体
 * 对应表：blade_portal_theme
 * 对标泛微 E-cology：themestyle（主题库，§12.5 P2 / §13.7）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_theme")
@Schema(name = "门户主题", description = "门户主题库（配色/Logo/背景，对标泛微 themestyle）")
public class PortalTheme extends Model<PortalTheme> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	@Schema(name = "主键ID")
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("theme_name")
	@Schema(name = "主题名称")
	private String themeName;

	@TableField("theme_key")
	@Schema(name = "主题标识（租户内唯一）")
	private String themeKey;

	@TableField("theme_config")
	@Schema(name = "主题配置JSON（{primaryColor,bgColor,logoUrl,...}）")
	private String themeConfig;

	@TableField("is_default")
	@Schema(name = "是否租户默认主题（0否 1是）")
	private Integer isDefault;

	@TableField("open_timer")
	@Schema(name = "是否开启定时切换（0否 1是）")
	private Integer openTimer;

	@TableField("timer_start")
	@Schema(name = "定时开始（yyyy-MM-dd HH:mm:ss）")
	private String timerStart;

	@TableField("timer_end")
	@Schema(name = "定时结束（yyyy-MM-dd HH:mm:ss）")
	private String timerEnd;

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
