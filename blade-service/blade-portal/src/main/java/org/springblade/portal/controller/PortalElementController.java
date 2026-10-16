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
import org.springblade.portal.dto.PortalElementSaveDTO;
import org.springblade.portal.dto.PortalElementSettingSaveDTO;
import org.springblade.portal.entity.PortalElement;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springblade.portal.service.IPortalElementService;
import org.springblade.portal.vo.PortalElementVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 门户元素控制器（管理端）
 * <p>写接口采用「HAS_AUTH + PortalRightUtil.check(权限码)」双层门禁；个性化 setting 保持用户级 HAS_AUTH。</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "门户元素", description = "门户元素配置维护")
public class PortalElementController {

	private final IPortalElementService elementService;
	private final PortalRightUtil portalRightUtil;

	@GetMapping("/element/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "页面元素列表", description = "按页面查询元素，按行号与排序号升序；config/style 已解析为对象")
	public R<List<PortalElementVO>> list(
		@Parameter(description = "页面ID") @RequestParam Long pageId) {
		requireRight(PortalConstant.PERM_ELEMENT_MAINT);
		return R.data(PortalElementVO.fromList(elementService.listByPage(pageId)));
	}

	@PostMapping("/element/submit")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存单个元素", description = "新增/编辑；elementId 为空视为新增")
	public R<Boolean> submit(@RequestBody PortalElementSaveDTO dto) {
		requireRight(PortalConstant.PERM_ELEMENT_MAINT);
		return R.status(elementService.save(dto));
	}

	@PostMapping("/element/save-batch")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "整页保存元素", description = "设计器保存：差量更新该页元素")
	public R<Boolean> saveBatch(
		@Parameter(description = "页面ID") @RequestParam Long pageId,
		@RequestBody List<PortalElementSaveDTO> elements) {
		requireRight(PortalConstant.PERM_ELEMENT_MAINT);
		return R.status(elementService.saveBatch(pageId, elements));
	}

	@PostMapping("/element/setting")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存元素个性化设置", description = "当前用户对某个元素的显示条数/标题显隐；userId 取登录人，不可越权")
	public R<Boolean> saveSetting(@RequestBody PortalElementSettingSaveDTO dto) {
		return R.status(elementService.saveSetting(dto));
	}

	/* ==================== 终端用户「我的门户」私有元素（对标 UserAddElementInfo） ==================== */

	@PostMapping("/my-portal/element/add")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "添加元素到我的门户", description = "终端用户新增私有元素（仅本人可见）；userId 取登录人")
	public R<Long> addMyElement(@RequestBody PortalElementSaveDTO dto) {
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		return R.data(elementService.addUserElement(userId, dto));
	}

	@PostMapping("/my-portal/element/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "移除我的门户元素", description = "终端用户删除自己添加的私有元素（仅限本人私有）")
	public R<Boolean> removeMyElement(@Parameter(description = "元素ID") @RequestParam Long elementId) {
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		return R.status(elementService.removeUserElement(userId, elementId));
	}

	@PostMapping("/my-portal/element/update")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "编辑我的门户元素", description = "终端用户编辑自己添加的私有元素（标题/宽度/配置；仅限本人私有）")
	public R<Boolean> updateMyElement(@RequestBody PortalElementSaveDTO dto) {
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		return R.status(elementService.updateUserElement(userId, dto));
	}

	@PostMapping("/element/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "删除元素", description = "逻辑删除元素并清理其个性化设置")
	public R<Boolean> remove(@RequestParam Long elementId) {
		requireRight(PortalConstant.PERM_ELEMENT_MAINT);
		return R.status(elementService.remove(elementId));
	}

	private void requireRight(String permCode) {
		if (!portalRightUtil.check(permCode)) {
			throw new PortalAccessDeniedException("无权限执行门户维护操作");
		}
	}
}
