package org.springblade.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalMenuSaveDTO;
import org.springblade.portal.entity.PortalMenu;
import org.springblade.portal.entity.PortalMenuShare;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springblade.portal.service.IPortalMenuService;
import org.springblade.portal.vo.PortalMenuNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 门户菜单控制器（对标泛微 portalmenu §14）
 * <p>写接口采用「HAS_AUTH + PortalRightUtil.check(权限码)」双层门禁；
 * 运行时菜单树 /menu/tree 仅需登录，按当前用户过滤可见范围。</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "门户菜单", description = "门户自有导航菜单（顶部/工具栏/快捷入口）管理与运行时")
public class PortalMenuController {

	private final IPortalMenuService menuService;
	private final PortalRightUtil portalRightUtil;

	/* ==================== 运行时（查看端） ==================== */

	@GetMapping("/menu/tree")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "运行时菜单树", description = "当前用户可见的指定类型菜单（已按授权过滤，父子层级）。menuType 支持 front/top/toolbar/quick：front/top 为门户私有导航（需在门户应用设置把导航菜单源切为 2 才会被首页消费），toolbar/quick 为门户私有入口")
	public R<List<PortalMenuNode>> tree(
		@Parameter(description = "菜单类型：front前端/top后端/toolbar工具栏/quick快捷入口") @RequestParam String menuType) {
		return R.data(menuService.menuTree(menuType));
	}

	@GetMapping("/menu/system-tree")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "系统菜单树(前端/后端菜单)", description = "当前用户有权限的系统导航菜单(blade_menu)：前端菜单=左侧整树、后端菜单=顶部；按角色授权过滤，超管见全部（对标泛微 LeftMenu/MainMenu 同一套引擎）")
	public R<List<PortalMenuNode>> systemTree(
		@Parameter(description = "区域：front 前端(左)/top 后端(顶)") @RequestParam(required = false, defaultValue = "front") String region) {
		return R.data(menuService.systemMenuTree(region));
	}

	/* ==================== 管理端 ==================== */

	@GetMapping("/menu/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "菜单列表", description = "管理端：按类型的扁平菜单（含隐藏）")
	public R<List<PortalMenu>> list(
		@Parameter(description = "菜单类型：top/toolbar/quick") @RequestParam(required = false) String menuType) {
		requireRight(PortalConstant.PERM_MENU_MAINT);
		return R.data(menuService.list(menuType));
	}

	@PostMapping("/menu/submit")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存菜单", description = "新增/编辑；menuId 为空视为新增")
	public R<Boolean> submit(@RequestBody PortalMenuSaveDTO dto) {
		requireRight(PortalConstant.PERM_MENU_MAINT);
		return R.status(menuService.save(dto));
	}

	@PostMapping("/menu/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "删除菜单", description = "逻辑删除菜单及其子孙；系统内置不可删")
	public R<Boolean> remove(@Parameter(description = "菜单ID") @RequestParam Long menuId) {
		requireRight(PortalConstant.PERM_MENU_MAINT);
		return R.status(menuService.remove(menuId));
	}

	@GetMapping("/menu/share/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "菜单可见范围", description = "查询菜单授权（1全员/2部门/3个人）")
	public R<List<PortalMenuShare>> shareList(@Parameter(description = "菜单ID") @RequestParam Long menuId) {
		requireRight(PortalConstant.PERM_MENU_MAINT);
		return R.data(menuService.listShare(menuId));
	}

	@PostMapping("/menu/share/set")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存菜单可见范围", description = "差量增删授权；传空数组=清空")
	public R<Boolean> shareSet(
		@Parameter(description = "菜单ID") @RequestParam Long menuId,
		@RequestBody(required = false) List<PortalMenuShare> shares) {
		requireRight(PortalConstant.PERM_MENU_MAINT);
		return R.status(menuService.saveShare(menuId, shares));
	}

	private void requireRight(String permCode) {
		if (!portalRightUtil.check(permCode)) {
			throw new PortalAccessDeniedException("无权限执行门户菜单维护操作");
		}
	}
}
