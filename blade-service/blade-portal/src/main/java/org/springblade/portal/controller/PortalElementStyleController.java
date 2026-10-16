package org.springblade.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalElementStyleSaveDTO;
import org.springblade.portal.dto.PortalElementTemplateSaveDTO;
import org.springblade.portal.entity.PortalElementStyle;
import org.springblade.portal.entity.PortalElementTemplate;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springblade.portal.service.IPortalElementStyleService;
import org.springblade.portal.service.IPortalElementTemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 元素样式库 + 元素模板库控制器
 * <p>对标泛微 elementstylelib（§15.3 B3，styleKind=element）、headlinestyle（§15.3 B4，
 * styleKind=headline）与 elementtemplate（§15.3 B2，HPELEMENTTEMPLATE）。</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "元素样式与模板库", description = "元素样式/头条样式/元素模板维护（对标泛微 §15.3 B2/B3/B4）")
public class PortalElementStyleController {

	private final IPortalElementStyleService elementStyleService;
	private final IPortalElementTemplateService elementTemplateService;
	private final PortalRightUtil portalRightUtil;

	/* ==================== 元素样式库 / 头条样式库 ==================== */

	@GetMapping("/element-style/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "元素样式列表", description = "styleKind 可空（element 元素样式 / headline 头条样式）")
	public R<List<PortalElementStyle>> styleList(
		@Parameter(description = "样式分类") @RequestParam(required = false) String styleKind) {
		return R.data(elementStyleService.list(styleKind));
	}

	@PostMapping("/element-style/submit")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存元素样式", description = "新增/编辑；同租户 分类+名称 冲突返回 false")
	public R<Boolean> styleSubmit(@RequestBody PortalElementStyleSaveDTO dto) {
		requireRight(PortalConstant.PERM_STYLE_LIB_MAINT);
		return R.status(elementStyleService.save(dto));
	}

	@PostMapping("/element-style/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "删除元素样式", description = "逻辑删除")
	public R<Boolean> styleRemove(@Parameter(description = "样式ID") @RequestParam Long id) {
		requireRight(PortalConstant.PERM_STYLE_LIB_MAINT);
		return R.status(elementStyleService.remove(id));
	}

	/* ==================== 元素模板库 ==================== */

	@GetMapping("/element-template/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "元素模板列表", description = "elementType 可空")
	public R<List<PortalElementTemplate>> templateList(
		@Parameter(description = "元素类型") @RequestParam(required = false) String elementType) {
		return R.data(elementTemplateService.list(elementType));
	}

	@PostMapping("/element-template/submit")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存元素模板", description = "另存为模板；同租户名称冲突返回 false")
	public R<Boolean> templateSubmit(@RequestBody PortalElementTemplateSaveDTO dto) {
		requireRight(PortalConstant.PERM_STYLE_LIB_MAINT);
		return R.status(elementTemplateService.save(dto));
	}

	@PostMapping("/element-template/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "删除元素模板", description = "逻辑删除")
	public R<Boolean> templateRemove(@Parameter(description = "模板ID") @RequestParam Long id) {
		requireRight(PortalConstant.PERM_STYLE_LIB_MAINT);
		return R.status(elementTemplateService.remove(id));
	}

	@PostMapping("/element-template/instantiate")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "模板实例化到页面", description = "按模板快照在目标页追加元素，返回新元素ID")
	public R<Long> templateInstantiate(
		@Parameter(description = "模板ID") @RequestParam Long templateId,
		@Parameter(description = "目标页面ID") @RequestParam Long pageId,
		@Parameter(description = "行号") @RequestParam(required = false) Integer rowNo,
		@Parameter(description = "栅格宽度") @RequestParam(required = false) Integer colSpan) {
		requireRight(PortalConstant.PERM_STYLE_LIB_MAINT);
		return R.data(elementTemplateService.instantiate(templateId, pageId, rowNo, colSpan));
	}

	private void requireRight(String permCode) {
		if (!portalRightUtil.check(permCode)) {
			throw new PortalAccessDeniedException("无权限执行样式库维护操作");
		}
	}
}
