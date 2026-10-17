package org.springblade.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

import org.springblade.portal.vo.ThemeVO;

/**
 * 我的门户聚合结果
 * <p>
 * 一次返回「当前用户可见的门户页面 + 全部元素配置（已合并用户个性化）」。
 * </p>
 */
@Data
@Schema(name = "我的门户", description = "当前用户可见的门户页面与元素配置")
public class MyPortalVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "页面ID")
	private Long pageId;

	@Schema(name = "页面编码")
	private String pageCode;

	@Schema(name = "页面名称")
	private String pageName;

	@Schema(name = "主题标识")
	private String theme;

	@Schema(name = "备注")
	private String remark;

	@Schema(name = "生效主题（已解析：用户偏好 > 页面主题 > 租户默认）")
	private ThemeVO resolvedTheme;

	@Schema(name = "元素列表")
	private List<PortalElementVO> elements;

	@Schema(name = "当前用户可见的全部门户页面（供多门户切换；已按授权过滤）")
	private List<PortalPageBrief> visiblePages;

	@Schema(name = "导航菜单源（1系统菜单blade_menu 2门户私有菜单blade_portal_menu；首页据此分流 /menu/system-tree 或 /menu/tree）")
	private Integer menuSource;

	@Schema(name = "是否允许页面维护者在查看页进入编辑布局（配置项 canPageMaintainerSettingInViewpage；按钮权限仍前置校验）")
	private Integer canMaintainInViewpage;
}