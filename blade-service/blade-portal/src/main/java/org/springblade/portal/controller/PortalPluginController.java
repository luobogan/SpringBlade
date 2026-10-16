package org.springblade.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalPluginSaveDTO;
import org.springblade.portal.entity.PortalPlugin;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springblade.portal.service.IPortalPluginService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 插件管理控制器（对标泛微 pluginmanage §14.2 G，hppluginsetting）
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "插件管理", description = "门户插件登记（pid/文件路径/启用/排序/类型，对标泛微 hppluginsetting）")
public class PortalPluginController {

	private final IPortalPluginService pluginService;
	private final PortalRightUtil portalRightUtil;

	@GetMapping("/plugin/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "插件列表", description = "按排序号返回全部登记插件")
	public R<List<PortalPlugin>> list() {
		return R.data(pluginService.list());
	}

	@PostMapping("/plugin/submit")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存插件", description = "新增/编辑；同租户 pid 冲突返回 false")
	public R<Boolean> submit(@RequestBody PortalPluginSaveDTO dto) {
		requireRight(PortalConstant.PERM_PLUGIN_MAINT);
		return R.status(pluginService.save(dto));
	}

	@PostMapping("/plugin/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "删除插件", description = "逻辑删除插件登记")
	public R<Boolean> remove(@Parameter(description = "插件ID") @RequestParam Long id) {
		requireRight(PortalConstant.PERM_PLUGIN_MAINT);
		return R.status(pluginService.remove(id));
	}

	private void requireRight(String permCode) {
		if (!portalRightUtil.check(permCode)) {
			throw new PortalAccessDeniedException("无权限执行插件维护操作");
		}
	}
}
