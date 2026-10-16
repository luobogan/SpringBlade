package org.springblade.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalThemeSaveDTO;
import org.springblade.portal.entity.PortalTheme;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springblade.portal.service.IPortalThemeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 门户主题控制器
 * <p>对标泛微 themestyle（主题库，§15.5 A2）/ themecenter（主题个性化中心，§15.5 配套），
 * 写接口门禁 {@link PortalConstant#PERM_THEME_MAINT}（对标 PortalRight.MAIN_PORTAL 独立权限码）。</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "门户主题", description = "主题库管理 + 用户主题偏好")
public class PortalThemeController {

	private final IPortalThemeService themeService;
	private final PortalRightUtil portalRightUtil;

	private void requireRight(String permCode) {
		if (!portalRightUtil.check(permCode)) {
			throw new PortalAccessDeniedException("无权限执行门户主题操作");
		}
	}

	@GetMapping("/theme/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "主题列表", description = "当前租户全部未删除主题（管理端）")
	public R<List<PortalTheme>> list() {
		requireRight(PortalConstant.PERM_THEME_MAINT);
		return R.data(themeService.list());
	}

	@GetMapping("/theme/options")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "主题下拉选项", description = "查看端：任意登录用户可选主题（不含敏感字段，供首页主题切换）")
	public R<List<PortalTheme>> options() {
		// themeConfig 为前端主题变量（主色/背景/Logo），非敏感信息，故查看端免维护权限
		return R.data(themeService.list());
	}

	@PostMapping("/theme/submit")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存主题", description = "新增/编辑（themeId 为空视为新增）")
	public R<Long> submit(@RequestBody PortalThemeSaveDTO dto) {
		requireRight(PortalConstant.PERM_THEME_MAINT);
		return R.data(themeService.save(dto));
	}

	@PostMapping("/theme/set-default")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "设为默认主题", description = "租户内仅一个默认主题")
	public R<Boolean> setDefault(@RequestParam Long themeId) {
		requireRight(PortalConstant.PERM_THEME_MAINT);
		return R.data(themeService.setDefault(themeId));
	}

	@PostMapping("/theme/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "删除主题", description = "逻辑删除（默认主题不可删）")
	public R<Boolean> remove(@RequestParam Long themeId) {
		requireRight(PortalConstant.PERM_THEME_MAINT);
		return R.data(themeService.remove(themeId));
	}

	@GetMapping("/theme/user")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "我的主题偏好", description = "返回当前用户选用的主题ID（无则返回 null）")
	public R<Long> userTheme() {
		BladeUser user = SecureUtil.getUser();
		return R.data(themeService.getUserThemeId(user == null ? null : user.getUserId()));
	}

	@PostMapping("/theme/user/set")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "设置我的主题", description = "覆盖当前用户的主题偏好")
	public R<Boolean> setUserTheme(
		@Parameter(description = "主题ID") @RequestParam Long themeId) {
		BladeUser user = SecureUtil.getUser();
		return R.data(themeService.setUserTheme(user == null ? null : user.getUserId(), themeId));
	}
}
