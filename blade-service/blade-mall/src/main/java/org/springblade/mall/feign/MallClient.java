package org.springblade.mall.feign;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.system.user.entity.UserInfo;
import org.springblade.system.user.feign.IUserClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城服务 Feign 实现类
 */
@Hidden
@RestController
@AllArgsConstructor
public class MallClient implements IMallClient {

	private static final String API_PREFIX = "/mall/user";
	private static final String INFO = API_PREFIX + "/info";
	private static final String EXIST = API_PREFIX + "/exist";

	private IUserClient userClient;

	@Override
	@GetMapping(INFO)
	public R<UserInfo> getUserInfo(@RequestParam("userId") Long userId) {
		return userClient.userInfo(userId);
	}

	@Override
	@GetMapping(EXIST)
	public R<Boolean> checkUserExist(@RequestParam("account") String account) {
		R<UserInfo> userInfoResult = userClient.userInfo(null, account, null);
		if (userInfoResult.isSuccess() && userInfoResult.getData() != null && userInfoResult.getData().getUser() != null) {
			return R.data(true);
		}
		return R.data(false);
	}

}
