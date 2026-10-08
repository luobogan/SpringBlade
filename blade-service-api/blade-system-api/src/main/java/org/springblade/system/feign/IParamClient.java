package org.springblade.system.feign;

import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.tool.api.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 参数公开 Feign接口类
 *
 * <p>映射 blade-system 的 ParamPublicController#publicValue（/param/public-value，无需登录），
 * 供 blade-auth 等服务经 Nacos 服务发现读取系统参数（如登录验证码开关 captcha_mode），
 * 替代 RestTemplate 写死 host:port 的自调用方式。</p>
 *
 * @author Blade
 */
@FeignClient(
	value = AppConstant.APPLICATION_SYSTEM_NAME,
	fallback = IParamClientFallback.class
)
public interface IParamClient {

	String API_PREFIX = "/param";

	/**
	 * 公开获取参数值（按 参数键:租户 组合键查询）
	 *
	 * @param paramKey 参数键（如 captcha_mode）
	 * @param tenantId 租户id
	 * @return 参数值；参数不存在时 data 为 null
	 */
	@GetMapping(API_PREFIX + "/public-value")
	R<String> publicValue(@RequestParam("paramKey") String paramKey, @RequestParam("tenantId") String tenantId);

}
