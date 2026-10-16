package org.springblade.portal.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.entity.PortalLog;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 门户审计控制器（管理端查询）
 * <p>
 * 前端经网关以 {@code /api/blade-portal/**} 访问。
 * 门禁口径与其它管理端接口统一：HAS_AUTH + PortalRightUtil.check(PERM_LOG_VIEW)
 * （原 HAS_ADMIN 角色硬编码口径废弃，对标 PortalRight 权限码体系）。
 * </p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "门户审计", description = "门户写操作审计查询")
public class PortalLogController {

	private final IPortalLogService logService;
	private final PortalRightUtil portalRightUtil;

	@GetMapping("/log/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "门户审计分页列表", description = "按操作时间倒序；支持按 logType/bizKey 过滤")
	public R<IPage<PortalLog>> list(
		@Parameter(description = "页码") @RequestParam(defaultValue = "1") long current,
		@Parameter(description = "每页条数") @RequestParam(defaultValue = "15") long size,
		@Parameter(description = "操作类型") @RequestParam(required = false) String logType,
		@Parameter(description = "业务标识") @RequestParam(required = false) String bizKey) {
		if (!portalRightUtil.check(PortalConstant.PERM_LOG_VIEW)) {
			throw new PortalAccessDeniedException("无权限查看门户审计日志");
		}
		return R.data(logService.page(current, size, logType, bizKey));
	}
}
