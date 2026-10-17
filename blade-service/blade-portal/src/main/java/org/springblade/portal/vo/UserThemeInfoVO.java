package org.springblade.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 人员当前主题详情（查人员当前主题，对标 getAllThemesOfUserId §15.5 A4）
 */
@Data
@Schema(name = "人员主题详情", description = "某用户当前生效/显式偏好主题")
public class UserThemeInfoVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "用户ID")
	private Long userId;

	@Schema(name = "账号")
	private String account;

	@Schema(name = "姓名")
	private String realName;

	@Schema(name = "部门ID")
	private Long deptId;

	@Schema(name = "是否有显式主题偏好（blade_portal_user_theme 有记录）")
	private Boolean hasExplicit;

	@Schema(name = "显式偏好主题ID（无显式偏好时为空）")
	private Long themeId;

	@Schema(name = "显式偏好主题名称")
	private String themeName;

	@Schema(name = "生效主题名称（按 resolveTheme 优先级解析：偏好>页面>定时>默认；仅供展示）")
	private String effectiveThemeName;
}
