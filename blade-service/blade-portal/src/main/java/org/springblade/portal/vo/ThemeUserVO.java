package org.springblade.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 主题使用人员（反查：某主题被哪些人使用，对标 getAllHrmOfTheme §15.5 A4）
 */
@Data
@Schema(name = "主题使用人员", description = "指定了该主题偏好的用户")
public class ThemeUserVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "用户ID")
	private Long userId;

	@Schema(name = "账号")
	private String account;

	@Schema(name = "姓名")
	private String realName;

	@Schema(name = "部门ID")
	private Long deptId;

	@Schema(name = "偏好设置时间")
	private String updateTime;
}
