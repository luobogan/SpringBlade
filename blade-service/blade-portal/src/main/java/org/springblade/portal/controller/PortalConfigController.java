package org.springblade.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.entity.PortalConfig;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springblade.portal.service.IPortalConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 门户配置项控制器（对标泛微 portalconfig §16.6）
 * <p>管理端列出配置项（ccode/vvalue/name/note）并编辑单条取值；
 * 读值接口 /config/value 供运行时消费点查询（如 isOpenMenuCache）。</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "门户配置项", description = "门户级键值配置维护（对标泛微 portalconfig）")
public class PortalConfigController {

	private final IPortalConfigService configService;
	private final PortalRightUtil portalRightUtil;

	@GetMapping("/config/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "配置项列表", description = "当前租户全部未删除配置项")
	public R<List<PortalConfig>> list() {
		requireRight(PortalConstant.PERM_CONFIG_MAINT);
		return R.data(configService.list());
	}

	@PostMapping("/config/set")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "修改配置取值", description = "按编码更新取值；写库后刷新进程内缓存")
	public R<Boolean> set(
		@Parameter(description = "配置项编码") @RequestParam String ccode,
		@Parameter(description = "新取值") @RequestParam String vvalue) {
		requireRight(PortalConstant.PERM_CONFIG_MAINT);
		return R.status(configService.setValue(ccode, vvalue));
	}

	@GetMapping("/config/value")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "读取配置值", description = "运行时消费点查询（无记录返回 null）")
	public R<String> value(@Parameter(description = "配置项编码") @RequestParam String ccode) {
		return R.data(configService.getValue(ccode));
	}

	private void requireRight(String permCode) {
		if (!portalRightUtil.check(permCode)) {
			throw new PortalAccessDeniedException("无权限执行门户配置项维护操作");
		}
	}
}
