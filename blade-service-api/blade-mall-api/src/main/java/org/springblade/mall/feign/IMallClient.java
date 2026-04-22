package org.springblade.mall.feign;

import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.tool.api.R;
import org.springblade.system.user.entity.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 商城服务 Feign 接口
 */
@FeignClient(
	value = AppConstant.APPLICATION_MALL_NAME,
	fallback = IMallClientFallback.class
)
public interface IMallClient {

	String API_PREFIX = "/mall/user";
	String INFO = API_PREFIX + "/info";
	String EXIST = API_PREFIX + "/exist";

	/**
	 * 获取用户信息
	 *
	 * @param userId 用户ID
	 * @return 用户信息
	 */
	@GetMapping(INFO)
	R<UserInfo> getUserInfo(@RequestParam("userId") Long userId);

}
