package org.springblade.mall.feign;

import org.springblade.core.tool.api.R;
import org.springblade.system.user.entity.UserInfo;
import org.springframework.stereotype.Component;

/**
 * 商城服务 Feign 降级实现
 */
@Component
public class IMallClientFallback implements IMallClient {

	@Override
	public R<UserInfo> getUserInfo(Long userId) {
		return R.fail("获取用户信息失败");
	}

}
