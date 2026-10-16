package org.springblade.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalQuickSearchSaveDTO;
import org.springblade.portal.entity.PortalQuickSearch;
import org.springblade.portal.entity.PortalQuickSearchShare;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springblade.portal.service.IPortalQuickSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 快捷搜索管理控制器（对标泛微 quicksearchmanage §14.2 H）
 * <p>总开关在门户应用设置（blade_portal_setting.quick_search_enabled）；本控制器管类型与使用限制。</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "快捷搜索管理", description = "搜索类型维护 + 使用限制（对标泛微 quicksearchmanage）")
public class PortalQuickSearchController {

	private final IPortalQuickSearchService quickSearchService;
	private final PortalRightUtil portalRightUtil;

	@GetMapping("/quick-search/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "搜索类型列表", description = "按排序号返回全部类型")
	public R<List<PortalQuickSearch>> list() {
		return R.data(quickSearchService.list());
	}

	@PostMapping("/quick-search/submit")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存搜索类型", description = "新增/编辑；同租户 module+type 冲突返回 false")
	public R<Boolean> submit(@RequestBody PortalQuickSearchSaveDTO dto) {
		requireRight(PortalConstant.PERM_QUICK_SEARCH_MAINT);
		return R.status(quickSearchService.save(dto));
	}

	@PostMapping("/quick-search/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "删除搜索类型", description = "系统内置不可删；级联清使用限制")
	public R<Boolean> remove(@Parameter(description = "类型ID") @RequestParam Long id) {
		requireRight(PortalConstant.PERM_QUICK_SEARCH_MAINT);
		return R.status(quickSearchService.remove(id));
	}

	@GetMapping("/quick-search/share/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "使用限制列表", description = "按类型查共享范围")
	public R<List<PortalQuickSearchShare>> shareList(@Parameter(description = "类型ID") @RequestParam Long id) {
		requireRight(PortalConstant.PERM_QUICK_SEARCH_MAINT);
		return R.data(quickSearchService.listShare(id));
	}

	@PostMapping("/quick-search/share/set")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存使用限制", description = "差量增删（1全员/2部门/3个人/4角色/5分部）；传空数组=清空")
	public R<Boolean> shareSet(
		@Parameter(description = "类型ID") @RequestParam Long id,
		@RequestBody(required = false) List<PortalQuickSearchShare> shares) {
		requireRight(PortalConstant.PERM_QUICK_SEARCH_MAINT);
		return R.status(quickSearchService.saveShare(id, shares));
	}

	private void requireRight(String permCode) {
		if (!portalRightUtil.check(permCode)) {
			throw new PortalAccessDeniedException("无权限执行快捷搜索维护操作");
		}
	}
}
