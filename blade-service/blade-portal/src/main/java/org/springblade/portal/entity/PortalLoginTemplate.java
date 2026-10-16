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
 * 登录前门户皮肤/模板实体
 * 对应表：blade_portal_login_template
 * 对标泛微 E-cology：SystemLoginTemplate（登录模板/皮肤，§16.1）+
 *                      portal_mloginpage 的结构化 config 分区（§20.4）
 * <p>登录页（未登录）按此配置渲染背景/Logo/登录框/二维码/版权；
 * 支持"当前启用(is_current)"与"定时切换(open_timer+timer_start/timer_end，拉模式)"。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_login_template")
@Schema(name = "登录前门户皮肤", description = "登录页皮肤/模板（对标泛微 SystemLoginTemplate）")
public class PortalLoginTemplate extends Model<PortalLoginTemplate> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	@Schema(name = "主键ID")
	private Long id;

	@TableField("tenant_id")
	@Schema(name = "租户ID")
	private String tenantId;

	@TableField("template_name")
	@Schema(name = "模板名称")
	private String templateName;

	@TableField("template_type")
	@Schema(name = "模板类型标识（如 default）")
	private String templateType;

	@TableField("background_config")
	@Schema(name = "背景配置JSON：{bgColor,bgImage}")
	private String backgroundConfig;

	@TableField("logo_config")
	@Schema(name = "Logo配置JSON：{logoUrl,title}")
	private String logoConfig;

	@TableField("form_config")
	@Schema(name = "登录框配置JSON")
	private String formConfig;

	@TableField("qrcode_config")
	@Schema(name = "二维码配置JSON")
	private String qrcodeConfig;

	@TableField("copyright_config")
	@Schema(name = "版权配置JSON")
	private String copyrightConfig;

	@TableField("is_current")
	@Schema(name = "是否当前启用（0否 1是，租户内唯一）")
	private Integer isCurrent;

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
