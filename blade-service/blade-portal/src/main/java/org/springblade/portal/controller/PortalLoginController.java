package org.springblade.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalLoginTemplateSaveDTO;
import org.springblade.portal.entity.PortalLoginTemplate;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springblade.portal.service.IPortalLoginTemplateService;
import org.springblade.portal.service.IPortalPageService;
import org.springblade.portal.vo.MyPortalVO;
import org.springblade.portal.vo.PortalLoginTemplateVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 登录前门户控制器（对标泛微 loginpage / loginportal §15.2 A1 / §16.1）
 * <p>
 * 管理端：登录页皮肤(模板)CRUD + 设为当前 + 定时切换；
 * 免鉴权端点（/login-template/current、/pre-login/portal）供登录页渲染，
 * 需在网关 blade.secure.skip-url 放行对应路径。
 * </p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "登录前门户", description = "登录页皮肤 + 登录前门户内容")
public class PortalLoginController {

	private final IPortalLoginTemplateService loginTemplateService;
	private final IPortalPageService pageService;
	private final PortalRightUtil portalRightUtil;

	/* ==================== 免鉴权（登录页渲染用） ==================== */

	@GetMapping("/login-template/current")
	@Operation(summary = "当前登录皮肤", description = "登录页渲染用：解析当前生效皮肤（定时优先 > 当前启用 > 兜底）；免鉴权")
	public R<PortalLoginTemplateVO> currentTemplate() {
		return R.data(loginTemplateService.resolveCurrent());
	}

	@GetMapping("/pre-login/portal")
	@Operation(summary = "登录前门户", description = "登录前门户页 + 页面级共享元素（无个性化）；免鉴权")
	public R<MyPortalVO> preLoginPortal(
		@Parameter(description = "页面编码，不传取第一个登录前页") @RequestParam(required = false) String portalCode) {
		return R.data(pageService.getPreLoginPortal(portalCode));
	}

	/* ==================== 管理端 ==================== */

	@GetMapping("/login-template/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "登录皮肤列表", description = "当前租户全部未删除皮肤模板")
	public R<List<PortalLoginTemplate>> list() {
		requireRight(PortalConstant.PERM_LOGIN_TEMPLATE_MAINT);
		return R.data(loginTemplateService.list());
	}

	@PostMapping("/login-template/submit")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存登录皮肤", description = "新增/编辑；templateId 为空视为新增")
	public R<Long> submit(@RequestBody PortalLoginTemplateSaveDTO dto) {
		requireRight(PortalConstant.PERM_LOGIN_TEMPLATE_MAINT);
		return R.data(loginTemplateService.save(dto));
	}

	@PostMapping("/login-template/set-current")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "设为当前登录皮肤", description = "租户内唯一启用")
	public R<Boolean> setCurrent(@Parameter(description = "模板ID") @RequestParam Long templateId) {
		requireRight(PortalConstant.PERM_LOGIN_TEMPLATE_MAINT);
		return R.status(loginTemplateService.setCurrent(templateId));
	}

	@PostMapping("/login-template/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "删除登录皮肤", description = "逻辑删除；当前启用中不可删")
	public R<Boolean> remove(@Parameter(description = "模板ID") @RequestParam Long templateId) {
		requireRight(PortalConstant.PERM_LOGIN_TEMPLATE_MAINT);
		return R.status(loginTemplateService.remove(templateId));
	}

	private void requireRight(String permCode) {
		if (!portalRightUtil.check(permCode)) {
			throw new PortalAccessDeniedException("无权限执行登录前门户维护操作");
		}
	}
}
