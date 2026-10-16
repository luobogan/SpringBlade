package org.springblade.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalAccountSettingSaveDTO;
import org.springblade.portal.dto.PortalSettingSaveDTO;
import org.springblade.portal.entity.PortalSetting;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springblade.portal.service.IPortalAccountSettingService;
import org.springblade.portal.service.IPortalSettingService;
import org.springblade.portal.vo.AccountMoreVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 门户应用设置 + 个人通用设置控制器
 * <p>对标泛微 portalsetting（§16.3，全局单例 + 同步到所有人）与 accountmore（§14.2 I，
 * 个人字体/布局/更多菜单显隐）。</p>
 * <p>写接口双层门禁：HAS_AUTH + PortalRightUtil.check(PERM_SETTING_MAINT)；
 * 个人设置接口仅需登录（userId 取登录人，防越权）。</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "门户应用设置", description = "全局应用设置 + 个人通用设置（对标泛微 portalsetting/accountmore）")
public class PortalSettingController {

	private final IPortalSettingService settingService;
	private final IPortalAccountSettingService accountSettingService;
	private final PortalRightUtil portalRightUtil;

	@GetMapping("/setting/get")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "读取门户应用设置", description = "租户全局设置（无记录时返回默认值兜底实例）")
	public R<PortalSetting> get() {
		return R.data(settingService.get());
	}

	@PostMapping("/setting/save")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存门户应用设置", description = "upsert 租户全局设置")
	public R<Boolean> save(@RequestBody PortalSettingSaveDTO dto) {
		requireRight(PortalConstant.PERM_SETTING_MAINT);
		return R.status(settingService.save(dto));
	}

	@PostMapping("/setting/sync")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "同步到所有人", description = "把全局字体/布局同步到所有已个性化用户（对标 synchronize）")
	public R<Integer> sync() {
		requireRight(PortalConstant.PERM_SETTING_MAINT);
		return R.data(settingService.syncToAll());
	}

	@GetMapping("/account-more/get")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "个人通用设置生效值", description = "个人值优先、NULL 回退全局（对标 accountmore）")
	public R<AccountMoreVO> accountMore() {
		return R.data(accountSettingService.getEffective(SecureUtil.getUser() == null ? null : SecureUtil.getUser().getUserId()));
	}

	@PostMapping("/account-more/save")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存个人通用设置", description = "userId 取当前登录人；字段传 null=回退全局")
	public R<Boolean> saveAccountMore(@RequestBody PortalAccountSettingSaveDTO dto) {
		Long userId = (SecureUtil.getUser() == null) ? null : SecureUtil.getUser().getUserId();
		return R.status(accountSettingService.save(userId, dto));
	}

	private void requireRight(String permCode) {
		if (!portalRightUtil.check(permCode)) {
			throw new PortalAccessDeniedException("无权限执行门户设置维护操作");
		}
	}
}
