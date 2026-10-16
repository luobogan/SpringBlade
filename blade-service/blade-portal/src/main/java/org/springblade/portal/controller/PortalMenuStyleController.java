package org.springblade.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalMenuStyleSaveDTO;
import org.springblade.portal.entity.PortalMenuStyle;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springblade.portal.service.IPortalMenuStyleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单样式库控制器（对标泛微 menustylelib §14.2 E，hpMenuStyle menuh/menuv）
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "菜单样式库", description = "横向/纵向菜单样式维护（对标泛微 hpMenuStyle menuh/menuv）")
public class PortalMenuStyleController {

	private final IPortalMenuStyleService menuStyleService;
	private final PortalRightUtil portalRightUtil;

	@GetMapping("/menu-style/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "菜单样式列表", description = "styleType 可空（menuh/menuv）")
	public R<List<PortalMenuStyle>> list(
		@Parameter(description = "样式类型") @RequestParam(required = false) String styleType) {
		return R.data(menuStyleService.list(styleType));
	}

	@PostMapping("/menu-style/submit")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存菜单样式", description = "新增/编辑；同租户 名称+类型 冲突返回 false")
	public R<Boolean> submit(@RequestBody PortalMenuStyleSaveDTO dto) {
		requireRight(PortalConstant.PERM_STYLE_LIB_MAINT);
		return R.status(menuStyleService.save(dto));
	}

	@PostMapping("/menu-style/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "删除菜单样式", description = "逻辑删除")
	public R<Boolean> remove(@Parameter(description = "样式ID") @RequestParam Long id) {
		requireRight(PortalConstant.PERM_STYLE_LIB_MAINT);
		return R.status(menuStyleService.remove(id));
	}

	private void requireRight(String permCode) {
		if (!portalRightUtil.check(permCode)) {
			throw new PortalAccessDeniedException("无权限执行菜单样式维护操作");
		}
	}
}
