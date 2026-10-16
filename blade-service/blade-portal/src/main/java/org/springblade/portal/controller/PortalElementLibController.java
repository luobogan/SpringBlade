package org.springblade.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalElementLibInstantiateDTO;
import org.springblade.portal.dto.PortalElementLibSaveDTO;
import org.springblade.portal.entity.PortalElementLib;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springblade.portal.service.IPortalElementLibService;
import org.springblade.portal.vo.PortalPageBrief;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 门户独立元素库控制器（对标 hpBaseElement，§12.5 P3 / §15.3 B1）
 * <p>写接口门禁 {@link PortalConstant#PERM_ELEMENT_LIB_MAINT}（元素库独立权限码，对标 §17.2）。</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "门户元素库", description = "独立元素库：复用 / 引用追踪 / 封存")
public class PortalElementLibController {

	private final IPortalElementLibService elementLibService;
	private final PortalRightUtil portalRightUtil;

	@GetMapping("/element-lib/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "元素库列表", description = "当前租户全部未删除库元素（含封存）")
	public R<List<PortalElementLib>> list() {
		requireRight(PortalConstant.PERM_ELEMENT_LIB_MAINT);
		return R.data(elementLibService.list());
	}

	@PostMapping("/element-lib/submit")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存库元素", description = "新增/编辑；libId 为空视为新增")
	public R<Long> submit(@RequestBody PortalElementLibSaveDTO dto) {
		requireRight(PortalConstant.PERM_ELEMENT_LIB_MAINT);
		return R.data(elementLibService.save(dto));
	}

	@PostMapping("/element-lib/archive")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "封存/解封库元素", description = "封存后不可再被新引用，既有引用继续有效")
	public R<Boolean> archive(
		@Parameter(description = "库元素ID") @RequestParam Long libId,
		@Parameter(description = "true 封存 / false 解封") @RequestParam boolean archived) {
		requireRight(PortalConstant.PERM_ELEMENT_LIB_MAINT);
		return R.status(elementLibService.setArchived(libId, archived));
	}

	@PostMapping("/element-lib/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "删除库元素", description = "被页面引用时禁止删除")
	public R<Boolean> remove(@Parameter(description = "库元素ID") @RequestParam Long libId) {
		requireRight(PortalConstant.PERM_ELEMENT_LIB_MAINT);
		return R.status(elementLibService.remove(libId));
	}

	@GetMapping("/element-lib/references")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "库元素被引用页面", description = "引用该库元素的页面列表")
	public R<List<PortalPageBrief>> references(@Parameter(description = "库元素ID") @RequestParam Long libId) {
		requireRight(PortalConstant.PERM_ELEMENT_LIB_MAINT);
		return R.data(elementLibService.references(libId));
	}

	@PostMapping("/element-lib/instantiate")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "实例化到页面", description = "把库元素引用为某页面的页面元素，返回新元素ID")
	public R<Long> instantiate(@RequestBody PortalElementLibInstantiateDTO dto) {
		requireRight(PortalConstant.PERM_ELEMENT_LIB_MAINT);
		return R.data(elementLibService.instantiate(dto));
	}

	private void requireRight(String permCode) {
		if (!portalRightUtil.check(permCode)) {
			throw new PortalAccessDeniedException("无权限执行门户元素库操作");
		}
	}
}
