package org.springblade.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalMaterialSaveDTO;
import org.springblade.portal.dto.PortalNewsTemplateSaveDTO;
import org.springblade.portal.entity.PortalMaterial;
import org.springblade.portal.entity.PortalNewsTemplate;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springblade.portal.service.IPortalMaterialService;
import org.springblade.portal.service.IPortalNewsTemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 素材库 + 新闻模板控制器
 * <p>对标泛微 materiallib（§15.4 C2，图片素材库）与 newstemplate（§15.4 C1，新闻模板库）。
 * 素材文件本体走资源服务，本控制器只维护元数据。</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "门户素材库", description = "图片素材 + 新闻模板（对标泛微 materiallib/newstemplate）")
public class PortalMaterialController {

	private final IPortalMaterialService materialService;
	private final IPortalNewsTemplateService newsTemplateService;
	private final PortalRightUtil portalRightUtil;

	/* ==================== 素材库 ==================== */

	@GetMapping("/material/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "素材列表", description = "dirName 可空（按目录过滤）")
	public R<List<PortalMaterial>> materialList(
		@Parameter(description = "目录名") @RequestParam(required = false) String dirName) {
		return R.data(materialService.list(dirName));
	}

	@GetMapping("/material/dirs")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "素材目录", description = "全部目录名（对标 getDirs）")
	public R<List<String>> materialDirs() {
		return R.data(materialService.dirs());
	}

	@PostMapping("/material/submit")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存素材", description = "新增/编辑素材元数据（文件 URL 由前端资源上传后回填）")
	public R<Boolean> materialSubmit(@RequestBody PortalMaterialSaveDTO dto) {
		requireRight(PortalConstant.PERM_MATERIAL_MAINT);
		return R.status(materialService.save(dto));
	}

	@PostMapping("/material/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "删除素材", description = "逻辑删除素材元数据")
	public R<Boolean> materialRemove(@Parameter(description = "素材ID") @RequestParam Long id) {
		requireRight(PortalConstant.PERM_MATERIAL_MAINT);
		return R.status(materialService.remove(id));
	}

	/* ==================== 新闻模板库 ==================== */

	@GetMapping("/news-template/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "新闻模板列表", description = "templateType 可空（inner/outer）")
	public R<List<PortalNewsTemplate>> newsTemplateList(
		@Parameter(description = "模板类型") @RequestParam(required = false) String templateType) {
		return R.data(newsTemplateService.list(templateType));
	}

	@PostMapping("/news-template/submit")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存新闻模板", description = "新增/编辑；is_default=1 时同类型内唯一")
	public R<Boolean> newsTemplateSubmit(@RequestBody PortalNewsTemplateSaveDTO dto) {
		requireRight(PortalConstant.PERM_MATERIAL_MAINT);
		return R.status(newsTemplateService.save(dto));
	}

	@PostMapping("/news-template/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "删除新闻模板", description = "逻辑删除")
	public R<Boolean> newsTemplateRemove(@Parameter(description = "模板ID") @RequestParam Long id) {
		requireRight(PortalConstant.PERM_MATERIAL_MAINT);
		return R.status(newsTemplateService.remove(id));
	}

	@PostMapping("/news-template/set-default")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "设为默认新闻模板", description = "同类型内唯一默认")
	public R<Boolean> newsTemplateSetDefault(@Parameter(description = "模板ID") @RequestParam Long id) {
		requireRight(PortalConstant.PERM_MATERIAL_MAINT);
		return R.status(newsTemplateService.setDefault(id));
	}

	private void requireRight(String permCode) {
		if (!portalRightUtil.check(permCode)) {
			throw new PortalAccessDeniedException("无权限执行素材库维护操作");
		}
	}
}
