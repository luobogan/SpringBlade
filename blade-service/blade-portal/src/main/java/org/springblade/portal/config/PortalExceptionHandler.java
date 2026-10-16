package org.springblade.portal.config;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 门户全局异常处理
 * <p>无权限访问门户维护接口 → HTTP 403 + {@code R.fail}（满足 Phase 1 验收：无权限返回 403 而非 401/500）。
 * 其余异常不在此兜底，交由既有默认行为处理，避免影响其它接口。</p>
 */
@Slf4j
@RestControllerAdvice
public class PortalExceptionHandler {

	@ExceptionHandler(PortalAccessDeniedException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public R<Void> handleAccessDenied(PortalAccessDeniedException e) {
		log.warn("[portal] 无权限访问门户维护接口：{}", e.getMessage());
		return R.fail(e.getMessage());
	}
}
