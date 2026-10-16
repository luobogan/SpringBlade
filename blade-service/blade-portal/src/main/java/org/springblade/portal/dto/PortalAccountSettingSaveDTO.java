package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 门户个人通用设置保存入参（用户单例 upsert）
 * 对标泛微 accountmore §14.2 I（个人字体/布局/更多菜单显隐）
 */
@Data
@Schema(name = "门户个人通用设置入参", description = "个人字体/布局/更多菜单显隐（对标泛微 accountmore）")
public class PortalAccountSettingSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "个人字体（1大 2中 3默认；NULL=沿用全局）")
	private Integer fontSize;

	@Schema(name = "个人布局类型（1舒适 2标准 3紧凑；NULL=沿用全局）")
	private Integer layoutType;

	@Schema(name = "个人\"更多\"菜单显隐（如 {\"font\":1,\"language\":1}）")
	private Map<String, Object> menuConfig;
}
