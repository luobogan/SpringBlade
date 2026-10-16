package org.springblade.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalLayoutInstantiateDTO;
import org.springblade.portal.dto.PortalLayoutTemplateSaveDTO;
import org.springblade.portal.entity.PortalLayout;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springblade.portal.service.IPortalLayoutService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 门户布局模板控制器（对标泛微 portallayout §12.5 P2）
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "门户布局模板", description = "布局模板库：存页面为模板 + 从模板实例化")
public class PortalLayoutController {

	private final IPortalLayoutService layoutService;
	private final PortalRightUtil portalRightUtil;

	@GetMapping("/layout/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "布局模板列表", description = "当前租户全部未删除模板")
	public R<List<PortalLayout>> list() {
		requireRight(PortalConstant.PERM_LAYOUT_MAINT);
		return R.data(layoutService.list());
	}

	@PostMapping("/layout/save-from-page")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "存页面为模板", description = "把某页面元素排列存为可复用模板")
	public R<Long> saveFromPage(@RequestBody PortalLayoutTemplateSaveDTO dto) {
		requireRight(PortalConstant.PERM_LAYOUT_MAINT);
		return R.data(layoutService.saveFromPage(dto));
	}

	@PostMapping("/layout/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "删除布局模板", description = "逻辑删除")
	public R<Boolean> remove(@Parameter(description = "模板ID") @RequestParam Long layoutId) {
		requireRight(PortalConstant.PERM_LAYOUT_MAINT);
		return R.status(layoutService.remove(layoutId));
	}

	@PostMapping("/layout/instantiate")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "从模板创建页面", description = "创建新门户页并复制模板元素，返回新页面ID")
	public R<Long> instantiate(@RequestBody PortalLayoutInstantiateDTO dto) {
		requireRight(PortalConstant.PERM_LAYOUT_MAINT);
		return R.data(layoutService.instantiate(dto));
	}

	private void requireRight(String permCode) {
		if (!portalRightUtil.check(permCode)) {
			throw new PortalAccessDeniedException("无权限执行门户布局模板操作");
		}
	}
}
