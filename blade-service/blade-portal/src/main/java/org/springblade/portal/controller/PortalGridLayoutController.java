package org.springblade.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalGridLayoutSaveDTO;
import org.springblade.portal.entity.PortalGridLayout;
import org.springblade.portal.entity.PortalGridLayoutShare;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springblade.portal.service.IPortalGridLayoutService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 特色门户（门户网格布局）控制器（对标泛微 tsportal §16.2）
 * <p>前端标签为「特色门户」，后端管理门户网格布局：按 layout_type 分
 * 登录前门户(login)/登录后门户(theme)，支持启用(layout_use)、默认入口
 * (layout_default_entrance，同类型唯一)、使用限制共享。</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "特色门户", description = "门户网格布局维护（对标泛微 hp_grid_layout）")
public class PortalGridLayoutController {

	private final IPortalGridLayoutService gridLayoutService;
	private final PortalRightUtil portalRightUtil;

	@GetMapping("/grid-layout/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "网格布局列表", description = "layoutType 可空（login/theme）")
	public R<List<PortalGridLayout>> list(
		@Parameter(description = "布局类型") @RequestParam(required = false) String layoutType) {
		requireRight(PortalConstant.PERM_GRID_LAYOUT_MAINT);
		return R.data(gridLayoutService.list(layoutType));
	}

	@PostMapping("/grid-layout/submit")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存网格布局", description = "新增/编辑；同租户 类型+名称 冲突返回 false；defaultEntrance=1 时同类型内唯一")
	public R<Boolean> submit(@RequestBody PortalGridLayoutSaveDTO dto) {
		requireRight(PortalConstant.PERM_GRID_LAYOUT_MAINT);
		return R.status(gridLayoutService.save(dto));
	}

	@PostMapping("/grid-layout/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "删除网格布局", description = "逻辑删除；默认入口不可删；级联清授权")
	public R<Boolean> remove(@Parameter(description = "布局ID") @RequestParam Long id) {
		requireRight(PortalConstant.PERM_GRID_LAYOUT_MAINT);
		return R.status(gridLayoutService.remove(id));
	}

	@PostMapping("/grid-layout/set-use")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "启用/停用网格布局", description = "对标 usePortalGridLayout")
	public R<Boolean> setUse(
		@Parameter(description = "布局ID") @RequestParam Long id,
		@Parameter(description = "true 启用 / false 停用") @RequestParam boolean use) {
		requireRight(PortalConstant.PERM_GRID_LAYOUT_MAINT);
		return R.status(gridLayoutService.setUse(id, use));
	}

	@PostMapping("/grid-layout/set-default-entrance")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "设为默认入口", description = "同类型内唯一；对标 defaultEntrancePortalGridLayout")
	public R<Boolean> setDefaultEntrance(@Parameter(description = "布局ID") @RequestParam Long id) {
		requireRight(PortalConstant.PERM_GRID_LAYOUT_MAINT);
		return R.status(gridLayoutService.setDefaultEntrance(id));
	}

	@GetMapping("/grid-layout/share/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "布局使用限制列表", description = "按布局查共享范围")
	public R<List<PortalGridLayoutShare>> shareList(@Parameter(description = "布局ID") @RequestParam Long id) {
		requireRight(PortalConstant.PERM_GRID_LAYOUT_MAINT);
		return R.data(gridLayoutService.listShare(id));
	}

	@PostMapping("/grid-layout/share/set")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存布局使用限制", description = "差量增删（1全员/2部门/3个人）；传空数组=清空")
	public R<Boolean> shareSet(
		@Parameter(description = "布局ID") @RequestParam Long id,
		@RequestBody(required = false) List<PortalGridLayoutShare> shares) {
		requireRight(PortalConstant.PERM_GRID_LAYOUT_MAINT);
		return R.status(gridLayoutService.saveShare(id, shares));
	}

	private void requireRight(String permCode) {
		if (!portalRightUtil.check(permCode)) {
			throw new PortalAccessDeniedException("无权限执行特色门户维护操作");
		}
	}
}
