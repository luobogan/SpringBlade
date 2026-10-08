package org.springblade.system.feign;

import org.springblade.core.tool.api.R;
import org.springframework.stereotype.Component;

/**
 * 参数公开 Feign失败配置
 *
 * <p>返回 null（调用方据此走 fail-closed 逻辑）。</p>
 *
 * @author Blade
 */
@Component
public class IParamClientFallback implements IParamClient {

	@Override
	public R<String> publicValue(String paramKey, String tenantId) {
		return null;
	}

}
