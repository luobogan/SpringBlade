package org.springblade.portal.security;

/**
 * 门户无权限异常
 * <p>由 {@code PortalExceptionHandler} 统一映射为 HTTP 403 + {@code R.fail}（§13.4 权限验收）。</p>
 */
public class PortalAccessDeniedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PortalAccessDeniedException(String message) {
		super(message);
	}
}
